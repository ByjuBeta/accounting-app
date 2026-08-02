package com.accountingapp.ar;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountService;
import com.accountingapp.account.AccountType;
import com.accountingapp.ar.dto.CreatePaymentRequest;
import com.accountingapp.ar.dto.PaymentApplicationRequest;
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
public class PaymentService {

    static final String CUSTOMER_CREDITS_CODE = "CUST-CREDITS";
    private static final String SOURCE_TYPE = "PAYMENT";

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final InvoiceService invoiceService;
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public Payment create(CreatePaymentRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        Account depositToAccount = accountRepository.findByIdAndOrganizationId(request.depositToAccountId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", request.depositToAccountId()));

        List<PaymentApplicationRequest> applicationRequests =
                request.applications() != null ? request.applications() : List.of();
        BigDecimal totalApplied = applicationRequests.stream()
                .map(PaymentApplicationRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalApplied.compareTo(request.amount()) > 0) {
            throw new BusinessRuleException("APPLIED_EXCEEDS_PAYMENT",
                    "Applied amount (%s) exceeds payment amount (%s)".formatted(totalApplied, request.amount()));
        }

        Payment payment = Payment.builder()
                .paymentNumber(nextPaymentNumber())
                .customer(customer)
                .paymentDate(request.paymentDate())
                .amount(request.amount())
                .depositToAccount(depositToAccount)
                .memo(request.memo())
                .referenceNumber(request.referenceNumber())
                .build();
        payment.setOrganization(entityManager.getReference(Organization.class, organizationId));

        for (PaymentApplicationRequest applicationRequest : applicationRequests) {
            Invoice invoice = invoiceRepository.findByIdAndOrganizationId(applicationRequest.invoiceId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice", applicationRequest.invoiceId()));
            if (!invoice.getCustomer().getId().equals(customer.getId())) {
                throw new BusinessRuleException("INVOICE_CUSTOMER_MISMATCH",
                        "Invoice " + invoice.getInvoiceNumber() + " does not belong to this customer");
            }
            PaymentApplication application = PaymentApplication.builder()
                    .payment(payment)
                    .invoice(invoice)
                    .amountApplied(applicationRequest.amount())
                    .build();
            payment.getApplications().add(application);
        }

        Payment saved = paymentRepository.save(payment);

        for (PaymentApplication application : saved.getApplications()) {
            invoiceService.applyPayment(application.getInvoice().getId(), application.getAmountApplied());
        }

        postJournalEntry(saved, totalApplied);

        auditLogService.record("CREATE", "Payment", saved.getId(),
                "Recorded payment " + saved.getPaymentNumber() + " from " + customer.getName(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public Payment getOrThrow(UUID paymentId) {
        return paymentRepository.findByIdAndOrganizationId(paymentId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    }

    @Transactional(readOnly = true)
    public Page<Payment> list(Pageable pageable) {
        return paymentRepository.findByOrganizationIdOrderByPaymentDateDesc(OrganizationContext.getRequired(), pageable);
    }

    private void postJournalEntry(Payment payment, BigDecimal totalApplied) {
        Account accountsReceivable = accountService.getOrCreateSystemAccount(
                InvoiceService.ACCOUNTS_RECEIVABLE_CODE, "Accounts Receivable", AccountType.ACCOUNTS_RECEIVABLE);

        List<JournalEntryLineRequest> lines = new ArrayList<>();
        lines.add(new JournalEntryLineRequest(payment.getDepositToAccount().getId(), payment.getAmount(), BigDecimal.ZERO,
                "Payment " + payment.getPaymentNumber(), Set.of()));
        if (totalApplied.signum() > 0) {
            lines.add(new JournalEntryLineRequest(accountsReceivable.getId(), BigDecimal.ZERO, totalApplied,
                    "Applied to invoices", Set.of()));
        }
        BigDecimal unapplied = payment.getAmount().subtract(totalApplied);
        if (unapplied.signum() > 0) {
            Account customerCredits = accountService.getOrCreateSystemAccount(
                    CUSTOMER_CREDITS_CODE, "Customer Credits", AccountType.OTHER_CURRENT_LIABILITY);
            lines.add(new JournalEntryLineRequest(customerCredits.getId(), BigDecimal.ZERO, unapplied,
                    "Unapplied / overpayment", Set.of()));
        }

        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                payment.getPaymentDate(), TransactionType.DEPOSIT,
                "Payment " + payment.getPaymentNumber() + " from " + payment.getCustomer().getName(),
                payment.getReferenceNumber(), payment.getDepositToAccount().getCurrencyCode(), lines);
        JournalEntry entry = journalEntryService.create(request);
        entry.setSourceType(SOURCE_TYPE);
        entry.setSourceId(payment.getId());
        journalEntryService.post(entry.getId());
    }

    private String nextPaymentNumber() {
        Number nextValue = (Number) entityManager
                .createNativeQuery("select nextval('payment_number_seq')")
                .getSingleResult();
        return "PMT-%06d".formatted(nextValue.longValue());
    }
}
