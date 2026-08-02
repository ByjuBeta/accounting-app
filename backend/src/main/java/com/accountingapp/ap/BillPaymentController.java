package com.accountingapp.ap;

import com.accountingapp.ap.dto.BillPaymentDto;
import com.accountingapp.ap.dto.CreateBillPaymentRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bill-payments")
@RequiredArgsConstructor
public class BillPaymentController {

    private final BillPaymentService billPaymentService;
    private final BillPaymentMapper billPaymentMapper;

    @PostMapping
    public ResponseEntity<BillPaymentDto> create(@Valid @RequestBody CreateBillPaymentRequest request) {
        BillPayment payment = billPaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(billPaymentMapper.toDto(payment));
    }

    @GetMapping("/{id}")
    public BillPaymentDto get(@PathVariable UUID id) {
        return billPaymentMapper.toDto(billPaymentService.getOrThrow(id));
    }

    @GetMapping
    public Page<BillPaymentDto> list(Pageable pageable) {
        return billPaymentService.list(pageable).map(billPaymentMapper::toDto);
    }
}
