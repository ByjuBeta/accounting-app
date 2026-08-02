package com.accountingapp.ap;

import com.accountingapp.ap.dto.ApAgingReport;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts-payable")
@RequiredArgsConstructor
public class ApAgingController {

    private final ApAgingService apAgingService;

    @GetMapping("/aging")
    public ApAgingReport getAging(@RequestParam(required = false) LocalDate asOfDate) {
        return apAgingService.getAgingReport(asOfDate != null ? asOfDate : LocalDate.now());
    }
}
