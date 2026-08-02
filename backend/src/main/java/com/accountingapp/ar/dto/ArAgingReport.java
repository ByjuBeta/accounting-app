package com.accountingapp.ar.dto;

import com.accountingapp.common.dto.AgingBucket;
import java.time.LocalDate;
import java.util.List;

public record ArAgingReport(LocalDate asOfDate, List<CustomerAging> rows, AgingBucket totals) {
}
