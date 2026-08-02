package com.accountingapp.ap.dto;

import com.accountingapp.common.dto.AgingBucket;
import java.time.LocalDate;
import java.util.List;

public record ApAgingReport(LocalDate asOfDate, List<VendorAging> rows, AgingBucket totals) {
}
