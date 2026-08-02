package com.accountingapp.account;

import com.accountingapp.account.dto.AccountDto;
import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.account.dto.UpdateAccountRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chart-of-accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @PostMapping
    public ResponseEntity<AccountDto> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.toDto(account));
    }

    @GetMapping("/{id}")
    public AccountDto get(@PathVariable UUID id) {
        return accountMapper.toDto(accountService.getOrThrow(id));
    }

    @GetMapping
    public List<AccountDto> list(@RequestParam(required = false) AccountStatus status) {
        return accountService.list(status).stream().map(accountMapper::toDto).toList();
    }

    @PutMapping("/{id}")
    public AccountDto update(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        return accountMapper.toDto(accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
