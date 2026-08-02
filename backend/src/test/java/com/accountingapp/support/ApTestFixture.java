package com.accountingapp.support;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountStatus;
import com.accountingapp.account.AccountType;
import com.accountingapp.ap.Vendor;
import com.accountingapp.ap.VendorRepository;
import com.accountingapp.ap.VendorStatus;
import com.accountingapp.organization.Organization;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApTestFixture {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VendorRepository vendorRepository;

    public Account createBankAccount(Organization organization) {
        Account account = Account.builder()
                .code("1000").name("Checking").accountType(AccountType.BANK)
                .currencyCode("USD").status(AccountStatus.ACTIVE).build();
        account.setOrganization(organization);
        return accountRepository.save(account);
    }

    public Account createExpenseAccount(Organization organization) {
        Account account = Account.builder()
                .code("6000").name("Office Supplies").accountType(AccountType.EXPENSE)
                .currencyCode("USD").status(AccountStatus.ACTIVE).build();
        account.setOrganization(organization);
        return accountRepository.save(account);
    }

    public Vendor createVendor(Organization organization) {
        return createVendor(organization, BigDecimal.ZERO, 0);
    }

    public Vendor createVendor(Organization organization, BigDecimal discountPercent, int discountDays) {
        Vendor vendor = Vendor.builder()
                .name("Acme Supplies").email("ap@acmesupplies.test").paymentTermsDays(30)
                .earlyPaymentDiscountPercent(discountPercent).earlyPaymentDiscountDays(discountDays)
                .status(VendorStatus.ACTIVE).build();
        vendor.setOrganization(organization);
        return vendorRepository.save(vendor);
    }
}
