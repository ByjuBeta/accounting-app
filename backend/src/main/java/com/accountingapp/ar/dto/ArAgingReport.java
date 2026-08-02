package com.accountingapp.ar.dto;

import java.time.LocalDate;
import java.util.List;

public record ArAgingReport(LocalDate asOfDate, List<CustomerAging> rows, AgingBucket totals) {
}
