package com.accountingapp.ar;

import com.accountingapp.ar.dto.ArAgingReport;
import com.accountingapp.ar.dto.CustomerAging;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.dto.AgingBucket;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArAgingService {

    private final InvoiceRepository invoiceRepository;

    public ArAgingReport getAgingReport(LocalDate asOfDate) {
        UUID organizationId = OrganizationContext.getRequired();
        var openInvoices = invoiceRepository.findByOrganizationIdAndStatusIn(
                organizationId, java.util.List.of(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID));

        Map<UUID, String> customerNames = new LinkedHashMap<>();
        Map<UUID, AgingBucket> byCustomer = new LinkedHashMap<>();

        for (Invoice invoice : openInvoices) {
            UUID customerId = invoice.getCustomer().getId();
            customerNames.putIfAbsent(customerId, invoice.getCustomer().getName());
            long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate);
            AgingBucket bucket = AgingBucket.forDaysOverdue(daysOverdue, invoice.balanceDue());
            byCustomer.merge(customerId, bucket, AgingBucket::plus);
        }

        var rows = byCustomer.entrySet().stream()
                .map(e -> new CustomerAging(e.getKey(), customerNames.get(e.getKey()), e.getValue()))
                .sorted((a, b) -> a.customerName().compareToIgnoreCase(b.customerName()))
                .toList();

        AgingBucket totals = rows.stream().map(CustomerAging::bucket).reduce(AgingBucket.zero(), AgingBucket::plus);
        return new ArAgingReport(asOfDate, rows, totals);
    }
}
