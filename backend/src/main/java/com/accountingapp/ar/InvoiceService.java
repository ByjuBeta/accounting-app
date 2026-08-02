package com.accountingapp.ar;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountService;
import com.accountingapp.account.AccountType;
import com.accountingapp.ar.dto.CreateInvoiceRequest;
import com.accountingapp.ar.dto.InvoiceLineRequest;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.journal.JournalEntry;
import com.accountingapp.journal.JournalEntryRepository;
import com.accountingapp.journal.JournalEntryService;
import com.accountingapp.journal.TransactionType;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.Organization;
import com.accountingapp.organization.OrganizationRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class InvoiceService {

    static final String ACCOUNTS_RECEIVABLE_CODE = "AR";
    static final String SALES_TAX_PAYABLE_CODE = "TAX-PAY";
    private static final String SOURCE_TYPE = "INVOICE";

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final OrganizationRepository organizationRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public Invoice create(CreateInvoiceRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        Customer customer = customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        Invoice invoice = Invoice.builder()
                .invoiceNumber(nextInvoiceNumber())
                .customer(customer)
                .invoiceDate(request.invoiceDate())
                .dueDate(request.dueDate() != null
                        ? request.dueDate() : request.invoiceDate().plusDays(customer.getPaymentTermsDays()))
                .status(InvoiceStatus.DRAFT)
                .memo(request.memo())
                .currencyCode(request.currencyCode() != null ? request.currencyCode() : organization.getBaseCurrencyCode())
                .build();
        invoice.setOrganization(organization);

        applyLines(invoice, request.lines(), organizationId, customer.isTaxExempt());
        invoice.recalculateTotals();

        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record("CREATE", "Invoice", saved.getId(), "Created draft invoice " + saved.getInvoiceNumber(), null);
        return saved;
    }

    @Transactional
    public Invoice send(UUID invoiceId) {
        Invoice invoice = getOrThrow(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Only draft invoices can be sent (current status: " + invoice.getStatus() + ")");
        }

        Account accountsReceivable = accountService.getOrCreateSystemAccount(
                ACCOUNTS_RECEIVABLE_CODE, "Accounts Receivable", AccountType.ACCOUNTS_RECEIVABLE);

        List<JournalEntryLineRequest> lines = new ArrayList<>();
        lines.add(new JournalEntryLineRequest(accountsReceivable.getId(), invoice.getTotal(), BigDecimal.ZERO,
                "Invoice " + invoice.getInvoiceNumber(), Set.of()));
        for (InvoiceLine line : invoice.getLines()) {
            lines.add(new JournalEntryLineRequest(line.getIncomeAccount().getId(), BigDecimal.ZERO, line.lineSubtotal(),
                    line.getDescription(), Set.of()));
        }
        if (invoice.getTaxTotal().signum() > 0) {
            Account taxPayable = accountService.getOrCreateSystemAccount(
                    SALES_TAX_PAYABLE_CODE, "Sales Tax Payable", AccountType.OTHER_CURRENT_LIABILITY);
            lines.add(new JournalEntryLineRequest(taxPayable.getId(), BigDecimal.ZERO, invoice.getTaxTotal(),
                    "Sales tax", Set.of()));
        }

        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                invoice.getInvoiceDate(), TransactionType.INVOICE,
                "Invoice " + invoice.getInvoiceNumber() + " — " + invoice.getCustomer().getName(),
                invoice.getInvoiceNumber(), invoice.getCurrencyCode(), lines);
        JournalEntry entry = journalEntryService.create(request);
        entry.setSourceType(SOURCE_TYPE);
        entry.setSourceId(invoice.getId());
        journalEntryService.post(entry.getId());

        invoice.setStatus(InvoiceStatus.SENT);
        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record("SEND", "Invoice", saved.getId(), "Sent invoice " + saved.getInvoiceNumber(), null);
        return saved;
    }

    @Transactional
    public Invoice cancel(UUID invoiceId) {
        Invoice invoice = getOrThrow(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Invoice is already cancelled");
        }
        if (invoice.getAmountPaid().signum() > 0) {
            throw new BusinessRuleException("HAS_PAYMENTS_APPLIED",
                    "Cannot cancel an invoice with payments applied — refund or unapply payments first");
        }

        if (invoice.getStatus() == InvoiceStatus.SENT) {
            journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                            SOURCE_TYPE, invoice.getId(), OrganizationContext.getRequired())
                    .ifPresent(entry -> journalEntryService.reverse(entry.getId(), LocalDate.now()));
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record("CANCEL", "Invoice", saved.getId(), "Cancelled invoice " + saved.getInvoiceNumber(), null);
        return saved;
    }

    /** Records that {@code amount} of this invoice's balance was just paid off; called by {@code PaymentService}. */
    @Transactional
    public void applyPayment(UUID invoiceId, BigDecimal amount) {
        Invoice invoice = getOrThrow(invoiceId);
        if (!invoice.isOpen()) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Cannot apply a payment to an invoice with status " + invoice.getStatus());
        }
        if (amount.compareTo(invoice.balanceDue()) > 0) {
            throw new BusinessRuleException("OVERPAYMENT_ON_INVOICE",
                    "Payment amount (%s) exceeds invoice balance due (%s)".formatted(amount, invoice.balanceDue()));
        }
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        invoice.setStatus(invoice.balanceDue().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getOrThrow(UUID invoiceId) {
        return invoiceRepository.findByIdAndOrganizationId(invoiceId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    }

    @Transactional(readOnly = true)
    public Page<Invoice> list(Pageable pageable) {
        return invoiceRepository.findByOrganizationIdOrderByInvoiceDateDescInvoiceNumberDesc(
                OrganizationContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listOpen() {
        return invoiceRepository.findByOrganizationIdAndStatusIn(
                OrganizationContext.getRequired(), List.of(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID));
    }

    @Transactional(readOnly = true)
    public List<Invoice> listOpenForCustomer(UUID customerId) {
        return invoiceRepository.findByCustomerIdAndStatusIn(
                customerId, List.of(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID));
    }

    private void applyLines(Invoice invoice, List<InvoiceLineRequest> lineRequests, UUID organizationId, boolean taxExempt) {
        for (InvoiceLineRequest lineRequest : lineRequests) {
            Account incomeAccount = accountRepository.findByIdAndOrganizationId(lineRequest.incomeAccountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", lineRequest.incomeAccountId()));
            if (incomeAccount.getAccountType() != AccountType.INCOME) {
                throw new BusinessRuleException("INVALID_INCOME_ACCOUNT",
                        "Account " + incomeAccount.getCode() + " is not an income account");
            }
            InvoiceLine line = InvoiceLine.builder()
                    .description(lineRequest.description())
                    .quantity(lineRequest.quantity())
                    .unitPrice(lineRequest.unitPrice())
                    .taxRate(taxExempt || lineRequest.taxRate() == null ? BigDecimal.ZERO : lineRequest.taxRate())
                    .incomeAccount(incomeAccount)
                    .build();
            invoice.addLine(line);
        }
    }

    private String nextInvoiceNumber() {
        Number nextValue = (Number) entityManager
                .createNativeQuery("select nextval('invoice_number_seq')")
                .getSingleResult();
        return "INV-%06d".formatted(nextValue.longValue());
    }
}
