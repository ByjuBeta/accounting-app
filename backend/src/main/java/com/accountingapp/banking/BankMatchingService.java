package com.accountingapp.banking;

import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntryLine;
import com.accountingapp.journal.JournalEntryLineRepository;
import com.accountingapp.journal.JournalEntryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Links imported {@link BankTransaction}s to existing {@link JournalEntryLine}s.
 * Matching (this class) is separate from reconciling (see
 * {@link ReconciliationService}): a match just says "this bank feed row and
 * this GL line are the same event" — reconciliation is the formal,
 * statement-by-statement sign-off that locks matched lines in.
 */
@Service
@RequiredArgsConstructor
public class BankMatchingService {

    private static final int MATCH_WINDOW_DAYS = 5;

    private final BankTransactionRepository bankTransactionRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<JournalEntryLine> suggestMatches(UUID bankTransactionId) {
        BankTransaction transaction = getOrThrow(bankTransactionId);
        return findCandidates(transaction);
    }

    /** Auto-matches every unmatched transaction on the account that has exactly one unambiguous candidate. */
    @Transactional
    public int autoMatch(UUID accountId) {
        List<BankTransaction> unmatched = bankTransactionRepository
                .findByAccountIdAndStatusOrderByTransactionDateAsc(accountId, BankTransactionStatus.UNMATCHED);
        int matched = 0;
        for (BankTransaction transaction : unmatched) {
            List<JournalEntryLine> candidates = findCandidates(transaction);
            if (candidates.size() == 1) {
                link(transaction, candidates.get(0));
                matched++;
            }
        }
        return matched;
    }

    @Transactional
    public BankTransaction confirmMatch(UUID bankTransactionId, UUID journalEntryLineId) {
        BankTransaction transaction = getOrThrow(bankTransactionId);
        JournalEntryLine line = journalEntryLineRepository.findById(journalEntryLineId)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntryLine", journalEntryLineId));
        if (line.isReconciled()) {
            throw new BusinessRuleException("LINE_ALREADY_RECONCILED", "That ledger line is already reconciled");
        }
        link(transaction, line);
        return transaction;
    }

    @Transactional
    public BankTransaction unmatch(UUID bankTransactionId) {
        BankTransaction transaction = getOrThrow(bankTransactionId);
        transaction.setStatus(BankTransactionStatus.UNMATCHED);
        transaction.setMatchedJournalEntryLine(null);
        return bankTransactionRepository.save(transaction);
    }

    @Transactional
    public BankTransaction ignore(UUID bankTransactionId) {
        BankTransaction transaction = getOrThrow(bankTransactionId);
        transaction.setStatus(BankTransactionStatus.IGNORED);
        transaction.setMatchedJournalEntryLine(null);
        return bankTransactionRepository.save(transaction);
    }

    private List<JournalEntryLine> findCandidates(BankTransaction transaction) {
        return journalEntryLineRepository.findUnreconciledCandidates(
                transaction.getAccount().getId(),
                transaction.getAmount(),
                transaction.getTransactionDate().minusDays(MATCH_WINDOW_DAYS),
                transaction.getTransactionDate().plusDays(MATCH_WINDOW_DAYS),
                JournalEntryService.balanceAffectingStatuses());
    }

    private void link(BankTransaction transaction, JournalEntryLine line) {
        transaction.setStatus(BankTransactionStatus.MATCHED);
        transaction.setMatchedJournalEntryLine(line);
        bankTransactionRepository.save(transaction);
        auditLogService.record("MATCH", "BankTransaction", transaction.getId(),
                "Matched bank transaction dated " + transaction.getTransactionDate() + " to ledger line", null);
    }

    private BankTransaction getOrThrow(UUID bankTransactionId) {
        return bankTransactionRepository.findByIdAndOrganizationId(bankTransactionId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("BankTransaction", bankTransactionId));
    }
}
