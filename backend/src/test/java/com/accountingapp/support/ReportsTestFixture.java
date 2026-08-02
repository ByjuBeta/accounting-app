package com.accountingapp.support;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountStatus;
import com.accountingapp.account.AccountType;
import com.accountingapp.organization.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportsTestFixture {

    @Autowired
    private AccountRepository accountRepository;

    public Account create(Organization organization, String code, String name, AccountType type) {
        Account account = Account.builder()
                .code(code).name(name).accountType(type)
                .currencyCode("USD").status(AccountStatus.ACTIVE).build();
        account.setOrganization(organization);
        return accountRepository.save(account);
    }
}
