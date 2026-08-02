package com.accountingapp.support;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountStatus;
import com.accountingapp.account.AccountType;
import com.accountingapp.ar.Customer;
import com.accountingapp.ar.CustomerRepository;
import com.accountingapp.ar.CustomerStatus;
import com.accountingapp.organization.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArTestFixture {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public Account createBankAccount(Organization organization) {
        Account account = Account.builder()
                .code("1000").name("Checking").accountType(AccountType.BANK)
                .currencyCode("USD").status(AccountStatus.ACTIVE).build();
        account.setOrganization(organization);
        return accountRepository.save(account);
    }

    public Account createIncomeAccount(Organization organization) {
        Account account = Account.builder()
                .code("4000").name("Sales Revenue").accountType(AccountType.INCOME)
                .currencyCode("USD").status(AccountStatus.ACTIVE).build();
        account.setOrganization(organization);
        return accountRepository.save(account);
    }

    public Customer createCustomer(Organization organization) {
        Customer customer = Customer.builder()
                .name("Acme Co").email("ap@acme.test").paymentTermsDays(30)
                .status(CustomerStatus.ACTIVE).build();
        customer.setOrganization(organization);
        return customerRepository.save(customer);
    }
}
