package com.accountingapp.ap;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountService;
import com.accountingapp.account.AccountType;
import com.accountingapp.ap.dto.BillPaymentApplicationRequest;
import com.accountingapp.ap.dto.CreateBillPaymentRequest;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryService;
import com.accountingapp.journal.TransactionType;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.Organization;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
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
public class BillPaymentService {

    static final String VENDOR_CREDITS_CODE = "VENDOR-CREDITS";
    static final String PURCHASE_DISCOUNTS_CODE = "PURCH-DISC";
    private static final String SOURCE_TYPE = "BILL_PAYMENT";

    private final BillPaymentRepository billPaymentRepository;
    private final VendorRepository vendorRepository;
    private final BillRepository billRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final BillService billService;
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public BillPayment create(CreateBillPaymentRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(request.vendorId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", request.vendorId()));
        Account paidFromAccount = accountRepository.findByIdAndOrganizationId(request.paidFromAccountId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.paidFromAccountId()));

        List<BillPaymentApplicationRequest> applicationRequests =
                request.applications() != null ? request.applications() : List.of();
        BigDecimal totalApplied = applicationRequests.stream()
                .map(BillPaymentApplicationRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalApplied.compareTo(request.amount()) > 0) {
            throw new BusinessRuleException("APPLIED_EXCEEDS_PAYMENT",
                    "Applied amount (%s) exceeds payment amount (%s)".formatted(totalApplied, request.amount()));
        }

        BillPayment payment = BillPayment.builder()
                .paymentNumber(nextPaymentNumber())
                .vendor(vendor)
                .paymentDate(request.paymentDate())
                .amount(request.amount())
                .paidFromAccount(paidFromAccount)
                .memo(request.memo())
                .referenceNumber(request.referenceNumber())
                .build();
        payment.setOrganization(entityManager.getReference(Organization.class, organizationId));

        for (BillPaymentApplicationRequest applicationRequest : applicationRequests) {
            Bill bill = billRepository.findByIdAndOrganizationId(applicationRequest.billId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bill", applicationRequest.billId()));
            if (!bill.getVendor().getId().equals(vendor.getId())) {
                throw new BusinessRuleException("BILL_VENDOR_MISMATCH",
                        "Bill " + bill.getBillNumber() + " does not belong to this vendor");
            }
            BigDecimal discountTaken = applicationRequest.discountTaken() != null
                    ? applicationRequest.discountTaken() : BigDecimal.ZERO;
            BillPaymentApplication application = BillPaymentApplication.builder()
                    .billPayment(payment)
                    .bill(bill)
                    .amountApplied(applicationRequest.amount())
                    .discountTaken(discountTaken)
                    .build();
            payment.getApplications().add(application);
        }

        BillPayment saved = billPaymentRepository.save(payment);

        for (BillPaymentApplication application : saved.getApplications()) {
            billService.applyPayment(application.getBill().getId(), application.totalAppliedToBalance());
        }

        postJournalEntry(saved, totalApplied);

        auditLogService.record("CREATE", "BillPayment", saved.getId(),
                "Recorded payment " + saved.getPaymentNumber() + " to " + vendor.getName(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public BillPayment getOrThrow(UUID paymentId) {
        return billPaymentRepository.findByIdAndOrganizationId(paymentId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("BillPayment", paymentId));
    }

    @Transactional(readOnly = true)
    public Page<BillPayment> list(Pageable pageable) {
        return billPaymentRepository.findByOrganizationIdOrderByPaymentDateDesc(OrganizationContext.getRequired(), pageable);
    }

    private void postJournalEntry(BillPayment payment, BigDecimal totalApplied) {
        Account accountsPayable = accountService.getOrCreateSystemAccount(
                BillService.ACCOUNTS_PAYABLE_CODE, "Accounts Payable", AccountType.ACCOUNTS_PAYABLE);
        BigDecimal totalDiscount = payment.totalDiscountTaken();
        BigDecimal totalAppliedToBalance = totalApplied.add(totalDiscount);

        List<JournalEntryLineRequest> lines = new ArrayList<>();
        if (totalAppliedToBalance.signum() > 0) {
            lines.add(new JournalEntryLineRequest(accountsPayable.getId(), totalAppliedToBalance, BigDecimal.ZERO,
                    "Applied to bills", Set.of()));
        }
        lines.add(new JournalEntryLineRequest(payment.getPaidFromAccount().getId(), BigDecimal.ZERO, payment.getAmount(),
                "Payment " + payment.getPaymentNumber(), Set.of()));
        if (totalDiscount.signum() > 0) {
            Account purchaseDiscounts = accountService.getOrCreateSystemAccount(
                    PURCHASE_DISCOUNTS_CODE, "Purchase Discounts Taken", AccountType.INCOME);
            lines.add(new JournalEntryLineRequest(purchaseDiscounts.getId(), BigDecimal.ZERO, totalDiscount,
                    "Early payment discount", Set.of()));
        }
        BigDecimal unapplied = payment.unappliedAmount();
        if (unapplied.signum() > 0) {
            Account vendorCredits = accountService.getOrCreateSystemAccount(
                    VENDOR_CREDITS_CODE, "Vendor Credits", AccountType.OTHER_CURRENT_ASSET);
            lines.add(new JournalEntryLineRequest(vendorCredits.getId(), unapplied, BigDecimal.ZERO,
                    "Unapplied / prepayment", Set.of()));
        }

        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                payment.getPaymentDate(), TransactionType.CHECK,
                "Payment " + payment.getPaymentNumber() + " to " + payment.getVendor().getName(),
                payment.getReferenceNumber(), payment.getPaidFromAccount().getCurrencyCode(), lines);
        JournalEntry entry = journalEntryService.create(request);
        entry.setSourceType(SOURCE_TYPE);
        entry.setSourceId(payment.getId());
        journalEntryService.post(entry.getId());
    }

    private String nextPaymentNumber() {
        Number nextValue = (Number) entityManager
                .createNativeQuery("select nextval('bill_payment_number_seq')")
                .getSingleResult();
        return "BPMT-%06d".formatted(nextValue.longValue());
    }
}
