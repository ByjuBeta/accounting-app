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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller methods that map an entity to its DTO are wrapped in
 * {@code @Transactional} here — the mapping touches lazy associations
 * (applications, and the vendor's name) that are only safe to read while
 * the Hibernate session from the service call is still open.
 */
@RestController
@RequestMapping("/api/v1/bill-payments")
@RequiredArgsConstructor
public class BillPaymentController {

    private final BillPaymentService billPaymentService;
    private final BillPaymentMapper billPaymentMapper;

    @PostMapping
    @Transactional
    public ResponseEntity<BillPaymentDto> create(@Valid @RequestBody CreateBillPaymentRequest request) {
        BillPayment payment = billPaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(billPaymentMapper.toDto(payment));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public BillPaymentDto get(@PathVariable UUID id) {
        return billPaymentMapper.toDto(billPaymentService.getOrThrow(id));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<BillPaymentDto> list(Pageable pageable) {
        return billPaymentService.list(pageable).map(billPaymentMapper::toDto);
    }
}
