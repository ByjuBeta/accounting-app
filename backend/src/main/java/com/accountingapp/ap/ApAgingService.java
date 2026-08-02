package com.accountingapp.ap;

import com.accountingapp.ap.dto.ApAgingReport;
import com.accountingapp.ap.dto.VendorAging;
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
public class ApAgingService {

    private final BillRepository billRepository;

    public ApAgingReport getAgingReport(LocalDate asOfDate) {
        UUID organizationId = OrganizationContext.getRequired();
        var openBills = billRepository.findByOrganizationIdAndStatusIn(
                organizationId, java.util.List.of(BillStatus.RECEIVED, BillStatus.PARTIALLY_PAID));

        Map<UUID, String> vendorNames = new LinkedHashMap<>();
        Map<UUID, AgingBucket> byVendor = new LinkedHashMap<>();

        for (Bill bill : openBills) {
            UUID vendorId = bill.getVendor().getId();
            vendorNames.putIfAbsent(vendorId, bill.getVendor().getName());
            long daysOverdue = ChronoUnit.DAYS.between(bill.getDueDate(), asOfDate);
            AgingBucket bucket = AgingBucket.forDaysOverdue(daysOverdue, bill.balanceDue());
            byVendor.merge(vendorId, bucket, AgingBucket::plus);
        }

        var rows = byVendor.entrySet().stream()
                .map(e -> new VendorAging(e.getKey(), vendorNames.get(e.getKey()), e.getValue()))
                .sorted((a, b) -> a.vendorName().compareToIgnoreCase(b.vendorName()))
                .toList();

        AgingBucket totals = rows.stream().map(VendorAging::bucket).reduce(AgingBucket.zero(), AgingBucket::plus);
        return new ApAgingReport(asOfDate, rows, totals);
    }
}
