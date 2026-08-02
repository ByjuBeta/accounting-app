package com.accountingapp.banking;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.banking.dto.OutstandingLineDto;
import com.accountingapp.banking.dto.ReconciliationWorksheet;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ConflictException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntryLine;
import com.accountingapp.journal.JournalEntryLineRepository;
import com.accountingapp.journal.JournalEntryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final OutstandingLineRepository outstandingLineRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Reconciliation start(UUID accountId, LocalDate statementDate, BigDecimal statementEndingBalance) {
        UUID organizationId = OrganizationContext.getRequired();
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        if (reconciliationRepository.existsByAccountIdAndStatus(accountId, ReconciliationStatus.IN_PROGRESS)) {
            throw new ConflictException("RECONCILIATION_IN_PROGRESS",
                    "There's already an in-progress reconciliation for " + account.getCode());
        }

        BigDecimal beginningBalance = reconciliationRepository
                .findFirstByAccountIdAndStatusOrderByStatementDateDesc(accountId, ReconciliationStatus.COMPLETED)
                .map(Reconciliation::getStatementEndingBalance)
                .orElse(BigDecimal.ZERO);

        Reconciliation reconciliation = Reconciliation.builder()
                .account(account)
                .statementDate(statementDate)
                .beginningBalance(beginningBalance)
                .statementEndingBalance(statementEndingBalance)
                .status(ReconciliationStatus.IN_PROGRESS)
                .build();
        reconciliation.setOrganization(account.getOrganization());

        Reconciliation saved = reconciliationRepository.save(reconciliation);
        auditLogService.record("START", "Reconciliation", saved.getId(),
                "Started reconciliation for " + account.getCode() + " as of " + statementDate, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public ReconciliationWorksheet getWorksheet(UUID reconciliationId) {
        Reconciliation reconciliation = getOrThrow(reconciliationId);
        return buildWorksheet(reconciliation);
    }

    @Transactional
    public Reconciliation complete(UUID reconciliationId) {
        Reconciliation reconciliation = getOrThrow(reconciliationId);
        if (reconciliation.getStatus() != ReconciliationStatus.IN_PROGRESS) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "This reconciliation is already completed");
        }

        ReconciliationWorksheet worksheet = buildWorksheet(reconciliation);
        if (!worksheet.isBalanced()) {
            throw new BusinessRuleException("RECONCILIATION_NOT_BALANCED",
                    "Statement balance and cleared transactions differ by " + worksheet.difference()
                            + " — match or unmatch transactions until the difference is zero");
        }

        List<BankTransaction> cleared = bankTransactionRepository.findClearedUnreconciled(
                reconciliation.getAccount().getId(), reconciliation.getStatementDate());
        Instant now = Instant.now();
        for (BankTransaction transaction : cleared) {
            JournalEntryLine line = transaction.getMatchedJournalEntryLine();
            line.setReconciled(true);
            line.setReconciledAt(now);
            journalEntryLineRepository.save(line);
        }

        reconciliation.setClearedBalance(worksheet.clearedTotal());
        reconciliation.setStatus(ReconciliationStatus.COMPLETED);
        reconciliation.setCompletedAt(now);
        Reconciliation saved = reconciliationRepository.save(reconciliation);
        auditLogService.record("COMPLETE", "Reconciliation", saved.getId(),
                "Completed reconciliation for " + reconciliation.getAccount().getCode()
                        + " — " + cleared.size() + " transactions cleared", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public Reconciliation getOrThrow(UUID reconciliationId) {
        return reconciliationRepository.findByIdAndOrganizationId(reconciliationId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation", reconciliationId));
    }

    @Transactional(readOnly = true)
    public List<Reconciliation> history(UUID accountId) {
        return reconciliationRepository.findByAccountIdOrderByStatementDateDesc(accountId);
    }

    private ReconciliationWorksheet buildWorksheet(Reconciliation reconciliation) {
        UUID accountId = reconciliation.getAccount().getId();
        List<BankTransaction> cleared =
                bankTransactionRepository.findClearedUnreconciled(accountId, reconciliation.getStatementDate());
        BigDecimal clearedTotal = cleared.stream().map(BankTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal projectedEndingBalance = reconciliation.getBeginningBalance().add(clearedTotal);
        BigDecimal difference = reconciliation.getStatementEndingBalance().subtract(projectedEndingBalance);

        List<JournalEntryLine> outstanding = outstandingLineRepository.findOutstandingLines(
                accountId, reconciliation.getStatementDate(), JournalEntryService.balanceAffectingStatuses());
        List<OutstandingLineDto> outstandingDtos = outstanding.stream()
                .map(line -> new OutstandingLineDto(
                        line.getId(), line.getJournalEntry().getId(), line.getJournalEntry().getEntryNumber(),
                        line.getJournalEntry().getEntryDate(), line.getMemo(), line.signedAmount()))
                .toList();

        return new ReconciliationWorksheet(
                reconciliation.getId(), accountId, reconciliation.getStatementDate(), reconciliation.getStatus(),
                reconciliation.getBeginningBalance(), reconciliation.getStatementEndingBalance(),
                clearedTotal, projectedEndingBalance, difference, cleared.size(), outstandingDtos);
    }
}
