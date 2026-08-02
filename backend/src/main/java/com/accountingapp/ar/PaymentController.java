package com.accountingapp.ar;

import com.accountingapp.ar.dto.CreatePaymentRequest;
import com.accountingapp.ar.dto.PaymentDto;
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
 * (applications, and the customer's name) that are only safe to read while
 * the Hibernate session from the service call is still open.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    @Transactional
    public ResponseEntity<PaymentDto> create(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMapper.toDto(payment));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PaymentDto get(@PathVariable UUID id) {
        return paymentMapper.toDto(paymentService.getOrThrow(id));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<PaymentDto> list(Pageable pageable) {
        return paymentService.list(pageable).map(paymentMapper::toDto);
    }
}
