package com.accountingapp.journal;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountStatus;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.journal.dto.UpdateJournalEntryRequest;
import com.accountingapp.organization.Organization;
import com.accountingapp.organization.OrganizationRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JournalEntryService {

    private static final Set<TransactionStatus> BALANCE_AFFECTING_STATUSES =
            EnumSet.of(TransactionStatus.POSTED, TransactionStatus.RECONCILED, TransactionStatus.LOCKED);

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    public static Set<TransactionStatus> balanceAffectingStatuses() {
        return BALANCE_AFFECTING_STATUSES;
    }

    @Transactional
    public JournalEntry create(CreateJournalEntryRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        JournalEntry entry = JournalEntry.builder()
                .entryNumber(nextEntryNumber())
                .entryDate(request.entryDate())
                .transactionType(request.transactionType())
                .status(TransactionStatus.DRAFT)
                .memo(request.memo())
                .referenceNumber(request.referenceNumber())
                .currencyCode(request.currencyCode() != null ? request.currencyCode() : organization.getBaseCurrencyCode())
                .build();
        entry.setOrganization(organization);

        applyLines(entry, request.lines(), organizationId);
        validateBalanced(entry);

        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.record("CREATE", "JournalEntry", saved.getId(),
                "Created draft journal entry " + saved.getEntryNumber(), null);
        return saved;
    }

    @Transactional
    public JournalEntry update(UUID entryId, UpdateJournalEntryRequest request) {
        JournalEntry entry = getEditableOrThrow(entryId);

        entry.setEntryDate(request.entryDate());
        entry.setMemo(request.memo());
        entry.setReferenceNumber(request.referenceNumber());
        entry.getLines().clear();
        applyLines(entry, request.lines(), OrganizationContext.getRequired());
        validateBalanced(entry);

        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.record("UPDATE", "JournalEntry", saved.getId(),
                "Updated draft journal entry " + saved.getEntryNumber(), null);
        return saved;
    }

    @Transactional
    public JournalEntry post(UUID entryId) {
        JournalEntry entry = getOrThrow(entryId);
        if (entry.getStatus() != TransactionStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Only draft entries can be posted (current status: " + entry.getStatus() + ")");
        }
        validateBalanced(entry);
        validateAccountsActive(entry);

        entry.setStatus(TransactionStatus.POSTED);
        entry.setPostedAt(Instant.now());
        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.record("POST", "JournalEntry", saved.getId(),
                "Posted journal entry " + saved.getEntryNumber(), null);
        return saved;
    }

    @Transactional
    public JournalEntry voidEntry(UUID entryId, String reason) {
        JournalEntry entry = getOrThrow(entryId);
        if (entry.getStatus() == TransactionStatus.VOID) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Entry is already void");
        }
        if (entry.getStatus() == TransactionStatus.RECONCILED || entry.getStatus() == TransactionStatus.LOCKED) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Reconciled or locked entries cannot be voided directly — reverse them instead");
        }
        entry.setStatus(TransactionStatus.VOID);
        entry.setVoidedAt(Instant.now());
        entry.setVoidReason(reason);
        JournalEntry saved = journalEntryRepository.save(entry);
        auditLogService.record("VOID", "JournalEntry", saved.getId(),
                "Voided journal entry " + saved.getEntryNumber() + ": " + reason, null);
        return saved;
    }

    @Transactional
    public void deleteDraft(UUID entryId) {
        JournalEntry entry = getEditableOrThrow(entryId);
        journalEntryRepository.delete(entry);
        auditLogService.record("DELETE", "JournalEntry", entryId,
                "Deleted draft journal entry " + entry.getEntryNumber(), null);
    }

    @Transactional
    public JournalEntry reverse(UUID entryId, LocalDate reversalDate) {
        JournalEntry original = getOrThrow(entryId);
        if (original.getStatus() != TransactionStatus.POSTED && original.getStatus() != TransactionStatus.RECONCILED) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Only posted or reconciled entries can be reversed (current status: " + original.getStatus() + ")");
        }

        JournalEntry reversal = JournalEntry.builder()
                .entryNumber(nextEntryNumber())
                .entryDate(reversalDate != null ? reversalDate : LocalDate.now())
                .transactionType(original.getTransactionType())
                .status(TransactionStatus.POSTED)
                .memo("Reversal of " + original.getEntryNumber())
                .referenceNumber(original.getReferenceNumber())
                .currencyCode(original.getCurrencyCode())
                .reversalOf(original)
                .postedAt(Instant.now())
                .build();
        reversal.setOrganization(original.getOrganization());

        for (JournalEntryLine line : original.getLines()) {
            JournalEntryLine swapped = JournalEntryLine.builder()
                    .account(line.getAccount())
                    .debitAmount(line.getCreditAmount())
                    .creditAmount(line.getDebitAmount())
                    .memo(line.getMemo())
                    .tags(new java.util.LinkedHashSet<>(line.getTags()))
                    .build();
            reversal.addLine(swapped);
        }
        validateBalanced(reversal);

        JournalEntry saved = journalEntryRepository.save(reversal);
        auditLogService.record("REVERSE", "JournalEntry", saved.getId(),
                "Reversed journal entry " + original.getEntryNumber() + " as " + saved.getEntryNumber(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public JournalEntry getOrThrow(UUID entryId) {
        return journalEntryRepository.findByIdAndOrganizationId(entryId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry", entryId));
    }

    @Transactional(readOnly = true)
    public Page<JournalEntry> list(Pageable pageable) {
        return journalEntryRepository.findByOrganizationIdOrderByEntryDateDescEntryNumberDesc(
                OrganizationContext.getRequired(), pageable);
    }

    private JournalEntry getEditableOrThrow(UUID entryId) {
        JournalEntry entry = getOrThrow(entryId);
        if (!entry.isEditable()) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Only draft entries can be edited or deleted (current status: " + entry.getStatus() + ")");
        }
        return entry;
    }

    private void applyLines(JournalEntry entry, List<JournalEntryLineRequest> lineRequests, UUID organizationId) {
        for (JournalEntryLineRequest lineRequest : lineRequests) {
            Account account = accountRepository.findByIdAndOrganizationId(lineRequest.accountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", lineRequest.accountId()));

            boolean hasDebit = lineRequest.debitAmount() != null && lineRequest.debitAmount().signum() > 0;
            boolean hasCredit = lineRequest.creditAmount() != null && lineRequest.creditAmount().signum() > 0;
            if (hasDebit == hasCredit) {
                throw new BusinessRuleException("INVALID_LINE_AMOUNT",
                        "Each line must have exactly one of debit or credit amount greater than zero");
            }

            JournalEntryLine line = JournalEntryLine.builder()
                    .account(account)
                    .debitAmount(lineRequest.debitAmount() != null ? lineRequest.debitAmount() : BigDecimal.ZERO)
                    .creditAmount(lineRequest.creditAmount() != null ? lineRequest.creditAmount() : BigDecimal.ZERO)
                    .memo(lineRequest.memo())
                    .tags(lineRequest.tags() != null ? new java.util.LinkedHashSet<>(lineRequest.tags()) : new java.util.LinkedHashSet<>())
                    .build();
            entry.addLine(line);
        }
    }

    private void validateBalanced(JournalEntry entry) {
        if (entry.getLines().size() < 2) {
            throw new BusinessRuleException("UNBALANCED_ENTRY", "A journal entry needs at least two lines");
        }
        BigDecimal totalDebit = entry.getLines().stream()
                .map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream()
                .map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessRuleException("UNBALANCED_ENTRY",
                    "Debits (%s) must equal credits (%s)".formatted(totalDebit, totalCredit));
        }
    }

    private void validateAccountsActive(JournalEntry entry) {
        for (JournalEntryLine line : entry.getLines()) {
            if (line.getAccount().getStatus() != AccountStatus.ACTIVE) {
                throw new BusinessRuleException("INACTIVE_ACCOUNT",
                        "Cannot post to inactive account " + line.getAccount().getCode());
            }
        }
    }

    private String nextEntryNumber() {
        Number nextValue = (Number) entityManager
                .createNativeQuery("select nextval('journal_entry_number_seq')")
                .getSingleResult();
        return "JE-%06d".formatted(nextValue.longValue());
    }
}
