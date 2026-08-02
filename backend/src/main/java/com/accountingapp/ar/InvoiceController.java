package com.accountingapp.ar;

import com.accountingapp.ar.dto.CreateInvoiceRequest;
import com.accountingapp.ar.dto.InvoiceDto;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    @PostMapping
    public ResponseEntity<InvoiceDto> create(@Valid @RequestBody CreateInvoiceRequest request) {
        Invoice invoice = invoiceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceMapper.toDto(invoice));
    }

    @GetMapping("/{id}")
    public InvoiceDto get(@PathVariable UUID id) {
        return invoiceMapper.toDto(invoiceService.getOrThrow(id));
    }

    @GetMapping
    public Page<InvoiceDto> list(Pageable pageable) {
        return invoiceService.list(pageable).map(invoiceMapper::toDto);
    }

    @GetMapping("/open")
    public List<InvoiceDto> listOpen() {
        return invoiceService.listOpen().stream().map(invoiceMapper::toDto).toList();
    }

    @PostMapping("/{id}/send")
    public InvoiceDto send(@PathVariable UUID id) {
        return invoiceMapper.toDto(invoiceService.send(id));
    }

    @PostMapping("/{id}/cancel")
    public InvoiceDto cancel(@PathVariable UUID id) {
        return invoiceMapper.toDto(invoiceService.cancel(id));
    }
}
