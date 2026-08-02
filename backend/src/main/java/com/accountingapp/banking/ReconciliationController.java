package com.accountingapp.banking;

import com.accountingapp.banking.dto.ReconciliationDto;
import com.accountingapp.banking.dto.ReconciliationWorksheet;
import com.accountingapp.banking.dto.StartReconciliationRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final ReconciliationMapper reconciliationMapper;

    @PostMapping
    public ResponseEntity<ReconciliationDto> start(
            @RequestParam UUID accountId, @Valid @RequestBody StartReconciliationRequest request) {
        Reconciliation reconciliation = reconciliationService.start(accountId, request.statementDate(), request.statementEndingBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliationMapper.toDto(reconciliation));
    }

    @GetMapping("/{id}")
    public ReconciliationDto get(@PathVariable UUID id) {
        return reconciliationMapper.toDto(reconciliationService.getOrThrow(id));
    }

    @GetMapping("/{id}/worksheet")
    public ReconciliationWorksheet worksheet(@PathVariable UUID id) {
        return reconciliationService.getWorksheet(id);
    }

    @PostMapping("/{id}/complete")
    public ReconciliationDto complete(@PathVariable UUID id) {
        return reconciliationMapper.toDto(reconciliationService.complete(id));
    }

    @GetMapping
    public List<ReconciliationDto> history(@RequestParam UUID accountId) {
        return reconciliationService.history(accountId).stream().map(reconciliationMapper::toDto).toList();
    }
}
