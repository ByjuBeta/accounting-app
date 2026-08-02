package com.accountingapp.ledger;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/general-ledger")
@RequiredArgsConstructor
public class GeneralLedgerController {

    private final GeneralLedgerService generalLedgerService;

    @GetMapping("/accounts/{accountId}/balance")
    public AccountBalance getAccountBalance(
            @PathVariable UUID accountId,
            @RequestParam(required = false) LocalDate asOfDate) {
        return generalLedgerService.getAccountBalance(accountId, asOfDate != null ? asOfDate : LocalDate.now());
    }

    @GetMapping("/trial-balance")
    public TrialBalance getTrialBalance(@RequestParam(required = false) LocalDate asOfDate) {
        return generalLedgerService.getTrialBalance(asOfDate != null ? asOfDate : LocalDate.now());
    }
}
