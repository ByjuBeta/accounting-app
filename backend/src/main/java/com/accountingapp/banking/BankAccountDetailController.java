package com.accountingapp.banking;

import com.accountingapp.banking.dto.BankAccountDetailDto;
import com.accountingapp.banking.dto.UpsertBankAccountDetailRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bank-accounts/{accountId}/details")
@RequiredArgsConstructor
public class BankAccountDetailController {

    private final BankAccountDetailService bankAccountDetailService;
    private final BankAccountDetailMapper bankAccountDetailMapper;

    @GetMapping
    public BankAccountDetailDto get(@PathVariable UUID accountId) {
        return bankAccountDetailMapper.toDto(bankAccountDetailService.getOrThrow(accountId));
    }

    @PutMapping
    public BankAccountDetailDto upsert(@PathVariable UUID accountId, @Valid @RequestBody UpsertBankAccountDetailRequest request) {
        return bankAccountDetailMapper.toDto(bankAccountDetailService.upsert(accountId, request));
    }
}
