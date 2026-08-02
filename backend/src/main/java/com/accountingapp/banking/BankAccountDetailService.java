package com.accountingapp.banking;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.banking.dto.UpsertBankAccountDetailRequest;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankAccountDetailService {

    private final BankAccountDetailRepository bankAccountDetailRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public BankAccountDetail upsert(UUID accountId, UpsertBankAccountDetailRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));

        BankAccountDetail detail = bankAccountDetailRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    BankAccountDetail created = BankAccountDetail.builder().account(account).build();
                    return created;
                });
        detail.setBankName(request.bankName());
        detail.setRoutingNumberLast4(request.routingNumberLast4());
        detail.setAccountNumberLast4(request.accountNumberLast4());
        detail.setNotes(request.notes());

        BankAccountDetail saved = bankAccountDetailRepository.save(detail);
        auditLogService.record("UPSERT", "BankAccountDetail", saved.getId(),
                "Updated bank details for " + account.getCode(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public BankAccountDetail getOrThrow(UUID accountId) {
        return bankAccountDetailRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("BankAccountDetail for account", accountId));
    }
}
