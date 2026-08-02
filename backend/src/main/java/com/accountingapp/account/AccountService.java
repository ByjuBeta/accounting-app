package com.accountingapp.account;

import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.account.dto.UpdateAccountRequest;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ConflictException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntryLineRepository;
import com.accountingapp.journal.JournalEntryService;
import com.accountingapp.journal.TransactionType;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.Organization;
import com.accountingapp.organization.OrganizationRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String OPENING_BALANCE_EQUITY_CODE = "OBE";

    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public Account create(CreateAccountRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        if (accountRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, request.code())) {
            throw new ConflictException("DUPLICATE_ACCOUNT_CODE",
                    "An account with code '%s' already exists".formatted(request.code()));
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        Account account = Account.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .accountType(request.accountType())
                .currencyCode(request.currencyCode() != null ? request.currencyCode() : organization.getBaseCurrencyCode())
                .status(AccountStatus.ACTIVE)
                .tags(request.tags() != null ? new LinkedHashSet<>(request.tags()) : new LinkedHashSet<>())
                .build();
        account.setOrganization(organization);
        account.setParent(resolveParent(request.parentId(), organizationId));

        Account saved = accountRepository.save(account);
        auditLogService.record("CREATE", "Account", saved.getId(), "Created account " + saved.getCode(), null);

        if (request.openingBalance() != null && request.openingBalance().signum() > 0) {
            postOpeningBalance(saved, request.openingBalance(),
                    request.openingBalanceDate() != null ? request.openingBalanceDate() : LocalDate.now());
        }
        return saved;
    }

    @Transactional
    public Account update(UUID accountId, UpdateAccountRequest request) {
        Account account = getOrThrow(accountId);
        UUID organizationId = OrganizationContext.getRequired();

        if (!account.getCode().equalsIgnoreCase(request.code())
                && accountRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, request.code())) {
            throw new ConflictException("DUPLICATE_ACCOUNT_CODE",
                    "An account with code '%s' already exists".formatted(request.code()));
        }

        Account parent = resolveParent(request.parentId(), organizationId);
        if (parent != null) {
            validateNotOwnDescendant(account, parent);
        }

        if (request.status() == AccountStatus.ARCHIVED && account.getStatus() != AccountStatus.ARCHIVED
                && accountRepository.existsByParentId(account.getId())) {
            throw new BusinessRuleException("HAS_ACTIVE_CHILDREN",
                    "Cannot archive an account that still has sub-accounts");
        }

        account.setCode(request.code());
        account.setName(request.name());
        account.setDescription(request.description());
        account.setParent(parent);
        if (request.status() != null) {
            account.setStatus(request.status());
        }
        account.setTags(request.tags() != null ? new LinkedHashSet<>(request.tags()) : new LinkedHashSet<>());

        Account saved = accountRepository.save(account);
        auditLogService.record("UPDATE", "Account", saved.getId(), "Updated account " + saved.getCode(), null);
        return saved;
    }

    @Transactional
    public void delete(UUID accountId) {
        Account account = getOrThrow(accountId);
        if (accountRepository.existsByParentId(accountId)) {
            throw new ConflictException("HAS_CHILDREN", "Cannot delete an account with sub-accounts");
        }
        if (journalEntryLineRepository.existsByAccountId(accountId)) {
            throw new ConflictException("HAS_TRANSACTIONS",
                    "Cannot delete an account with posted transactions — archive it instead");
        }
        accountRepository.delete(account);
        auditLogService.record("DELETE", "Account", accountId, "Deleted account " + account.getCode(), null);
    }

    @Transactional(readOnly = true)
    public Account getOrThrow(UUID accountId) {
        return accountRepository.findByIdAndOrganizationId(accountId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    @Transactional(readOnly = true)
    public List<Account> list(AccountStatus statusFilter) {
        UUID organizationId = OrganizationContext.getRequired();
        return statusFilter != null
                ? accountRepository.findByOrganizationIdAndStatusOrderByCodeAsc(organizationId, statusFilter)
                : accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId);
    }

    private Account resolveParent(UUID parentId, UUID organizationId) {
        if (parentId == null) {
            return null;
        }
        return accountRepository.findByIdAndOrganizationId(parentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", parentId));
    }

    private void validateNotOwnDescendant(Account account, Account proposedParent) {
        Account cursor = proposedParent;
        while (cursor != null) {
            if (cursor.getId().equals(account.getId())) {
                throw new BusinessRuleException("CIRCULAR_HIERARCHY",
                        "An account cannot be moved under itself or one of its own sub-accounts");
            }
            cursor = cursor.getParent();
        }
    }

    private void postOpeningBalance(Account account, BigDecimal amount, LocalDate asOfDate) {
        Account openingBalanceEquity = getOrCreateOpeningBalanceEquityAccount();
        boolean accountIsDebitNormal = account.getNormalBalance() == NormalBalance.DEBIT;

        JournalEntryLineRequest accountLine = new JournalEntryLineRequest(
                account.getId(),
                accountIsDebitNormal ? amount : BigDecimal.ZERO,
                accountIsDebitNormal ? BigDecimal.ZERO : amount,
                "Opening balance", Set.of());
        JournalEntryLineRequest offsetLine = new JournalEntryLineRequest(
                openingBalanceEquity.getId(),
                accountIsDebitNormal ? BigDecimal.ZERO : amount,
                accountIsDebitNormal ? amount : BigDecimal.ZERO,
                "Opening balance offset", Set.of());

        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                asOfDate, TransactionType.JOURNAL_ENTRY,
                "Opening balance for " + account.getCode() + " " + account.getName(),
                null, account.getCurrencyCode(), List.of(accountLine, offsetLine));

        var entry = journalEntryService.create(request);
        journalEntryService.post(entry.getId());
    }

    private Account getOrCreateOpeningBalanceEquityAccount() {
        return getOrCreateSystemAccount(OPENING_BALANCE_EQUITY_CODE, "Opening Balance Equity", AccountType.EQUITY);
    }

    /**
     * Finds or lazily creates a well-known account (e.g. Accounts Receivable,
     * Sales Tax Payable) that other modules post to automatically. Shared
     * across A/R, A/P, and banking so each doesn't reimplement the same
     * find-or-create dance.
     */
    @Transactional
    public Account getOrCreateSystemAccount(String code, String name, AccountType accountType) {
        UUID organizationId = OrganizationContext.getRequired();
        return accountRepository.findByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                .orElseGet(() -> {
                    Organization organization = entityManager.getReference(Organization.class, organizationId);
                    Account account = Account.builder()
                            .code(code)
                            .name(name)
                            .accountType(accountType)
                            .currencyCode(organizationRepository.findById(organizationId)
                                    .map(Organization::getBaseCurrencyCode).orElse("USD"))
                            .status(AccountStatus.ACTIVE)
                            .build();
                    account.setOrganization(organization);
                    return accountRepository.save(account);
                });
    }
}
