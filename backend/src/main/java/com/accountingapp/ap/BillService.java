package com.accountingapp.ap;

import com.accountingapp.account.Account;
import com.accountingapp.account.AccountRepository;
import com.accountingapp.account.AccountService;
import com.accountingapp.account.AccountType;
import com.accountingapp.ap.dto.BillLineRequest;
import com.accountingapp.ap.dto.CreateBillRequest;
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
public class BillService {

    static final String ACCOUNTS_PAYABLE_CODE = "AP";
    private static final String SOURCE_TYPE = "BILL";

    private final BillRepository billRepository;
    private final VendorRepository vendorRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final OrganizationRepository organizationRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public Bill create(CreateBillRequest request) {
        UUID organizationId = OrganizationContext.getRequired();
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(request.vendorId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", request.vendorId()));
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

        Bill bill = Bill.builder()
                .billNumber(nextBillNumber())
                .vendorReferenceNumber(request.vendorReferenceNumber())
                .vendor(vendor)
                .billDate(request.billDate())
                .dueDate(request.dueDate() != null
                        ? request.dueDate() : request.billDate().plusDays(vendor.getPaymentTermsDays()))
                .status(BillStatus.DRAFT)
                .memo(request.memo())
                .currencyCode(request.currencyCode() != null ? request.currencyCode() : organization.getBaseCurrencyCode())
                .build();
        bill.setOrganization(organization);

        applyLines(bill, request.lines(), organizationId);
        bill.recalculateTotals();

        Bill saved = billRepository.save(bill);
        auditLogService.record("CREATE", "Bill", saved.getId(), "Created draft bill " + saved.getBillNumber(), null);
        return saved;
    }

    @Transactional
    public Bill receive(UUID billId) {
        Bill bill = getOrThrow(billId);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Only draft bills can be received (current status: " + bill.getStatus() + ")");
        }

        Account accountsPayable = accountService.getOrCreateSystemAccount(
                ACCOUNTS_PAYABLE_CODE, "Accounts Payable", AccountType.ACCOUNTS_PAYABLE);

        List<JournalEntryLineRequest> lines = new ArrayList<>();
        for (BillLine line : bill.getLines()) {
            lines.add(new JournalEntryLineRequest(line.getExpenseAccount().getId(), line.lineTotal(), BigDecimal.ZERO,
                    line.getDescription(), Set.of()));
        }
        lines.add(new JournalEntryLineRequest(accountsPayable.getId(), BigDecimal.ZERO, bill.getTotal(),
                "Bill " + bill.getBillNumber() + " — " + bill.getVendor().getName(), Set.of()));

        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                bill.getBillDate(), TransactionType.BILL,
                "Bill " + bill.getBillNumber() + " — " + bill.getVendor().getName(),
                bill.getVendorReferenceNumber(), bill.getCurrencyCode(), lines);
        JournalEntry entry = journalEntryService.create(request);
        entry.setSourceType(SOURCE_TYPE);
        entry.setSourceId(bill.getId());
        journalEntryService.post(entry.getId());

        bill.setStatus(BillStatus.RECEIVED);
        Bill saved = billRepository.save(bill);
        auditLogService.record("RECEIVE", "Bill", saved.getId(), "Received bill " + saved.getBillNumber(), null);
        return saved;
    }

    @Transactional
    public Bill cancel(UUID billId) {
        Bill bill = getOrThrow(billId);
        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Bill is already cancelled");
        }
        if (bill.getAmountPaid().signum() > 0) {
            throw new BusinessRuleException("HAS_PAYMENTS_APPLIED",
                    "Cannot cancel a bill with payments applied — unapply payments first");
        }

        if (bill.getStatus() == BillStatus.RECEIVED) {
            journalEntryRepository.findBySourceTypeAndSourceIdAndOrganizationId(
                            SOURCE_TYPE, bill.getId(), OrganizationContext.getRequired())
                    .ifPresent(entry -> journalEntryService.reverse(entry.getId(), LocalDate.now()));
        }

        bill.setStatus(BillStatus.CANCELLED);
        Bill saved = billRepository.save(bill);
        auditLogService.record("CANCEL", "Bill", saved.getId(), "Cancelled bill " + saved.getBillNumber(), null);
        return saved;
    }

    /** Records that {@code amount} (cash + any discount) was just paid off this bill's balance; called by {@code BillPaymentService}. */
    @Transactional
    public void applyPayment(UUID billId, BigDecimal amount) {
        Bill bill = getOrThrow(billId);
        if (!bill.isOpen()) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Cannot apply a payment to a bill with status " + bill.getStatus());
        }
        if (amount.compareTo(bill.balanceDue()) > 0) {
            throw new BusinessRuleException("OVERPAYMENT_ON_BILL",
                    "Payment amount (%s) exceeds bill balance due (%s)".formatted(amount, bill.balanceDue()));
        }
        bill.setAmountPaid(bill.getAmountPaid().add(amount));
        bill.setStatus(bill.balanceDue().signum() == 0 ? BillStatus.PAID : BillStatus.PARTIALLY_PAID);
        billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public Bill getOrThrow(UUID billId) {
        return billRepository.findByIdAndOrganizationId(billId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Bill", billId));
    }

    @Transactional(readOnly = true)
    public Page<Bill> list(Pageable pageable) {
        return billRepository.findByOrganizationIdOrderByDueDateAscBillNumberAsc(OrganizationContext.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Bill> listOpen() {
        return billRepository.findByOrganizationIdAndStatusIn(
                OrganizationContext.getRequired(), List.of(BillStatus.RECEIVED, BillStatus.PARTIALLY_PAID));
    }

    private void applyLines(Bill bill, List<BillLineRequest> lineRequests, UUID organizationId) {
        for (BillLineRequest lineRequest : lineRequests) {
            Account expenseAccount = accountRepository.findByIdAndOrganizationId(lineRequest.expenseAccountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", lineRequest.expenseAccountId()));
            if (expenseAccount.getAccountType() != AccountType.EXPENSE
                    && expenseAccount.getAccountType() != AccountType.COST_OF_GOODS_SOLD) {
                throw new BusinessRuleException("INVALID_EXPENSE_ACCOUNT",
                        "Account " + expenseAccount.getCode() + " is not an expense or COGS account");
            }
            BillLine line = BillLine.builder()
                    .description(lineRequest.description())
                    .quantity(lineRequest.quantity())
                    .unitPrice(lineRequest.unitPrice())
                    .taxRate(lineRequest.taxRate() != null ? lineRequest.taxRate() : BigDecimal.ZERO)
                    .expenseAccount(expenseAccount)
                    .build();
            bill.addLine(line);
        }
    }

    private String nextBillNumber() {
        Number nextValue = (Number) entityManager
                .createNativeQuery("select nextval('bill_number_seq')")
                .getSingleResult();
        return "BILL-%06d".formatted(nextValue.longValue());
    }
}
