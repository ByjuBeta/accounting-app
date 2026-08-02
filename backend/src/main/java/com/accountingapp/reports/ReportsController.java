package com.accountingapp.reports;

import com.accountingapp.reports.dto.BalanceSheet;
import com.accountingapp.reports.dto.CashFlowMethod;
import com.accountingapp.reports.dto.CashFlowStatement;
import com.accountingapp.reports.dto.GeneralLedgerDetail;
import com.accountingapp.reports.dto.IncomeStatement;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/balance-sheet")
    public BalanceSheet balanceSheet(@RequestParam(required = false) LocalDate asOfDate) {
        return reportsService.getBalanceSheet(asOfDate != null ? asOfDate : LocalDate.now());
    }

    @GetMapping("/income-statement")
    public IncomeStatement incomeStatement(
            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return reportsService.getIncomeStatement(fromDate, toDate);
    }

    @GetMapping("/cash-flow")
    public CashFlowStatement cashFlow(
            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate,
            @RequestParam(defaultValue = "DIRECT") CashFlowMethod method) {
        return reportsService.getCashFlowStatement(fromDate, toDate, method);
    }

    @GetMapping("/general-ledger/{accountId}")
    public GeneralLedgerDetail generalLedger(
            @PathVariable UUID accountId,
            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return reportsService.getGeneralLedgerDetail(accountId, fromDate, toDate);
    }
}
