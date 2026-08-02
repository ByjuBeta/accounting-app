package com.accountingapp.ar;

import com.accountingapp.ar.dto.ArAgingReport;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts-receivable")
@RequiredArgsConstructor
public class ArAgingController {

    private final ArAgingService arAgingService;

    @GetMapping("/aging")
    public ArAgingReport getAging(@RequestParam(required = false) LocalDate asOfDate) {
        return arAgingService.getAgingReport(asOfDate != null ? asOfDate : LocalDate.now());
    }
}
