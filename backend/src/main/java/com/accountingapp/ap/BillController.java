package com.accountingapp.ap;

import com.accountingapp.ap.dto.BillDto;
import com.accountingapp.ap.dto.CreateBillRequest;
import jakarta.validation.Valid;
import java.util.List;
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
 * (lines, and the vendor's name) that are only safe to read while the
 * Hibernate session from the service call is still open.
 */
@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final BillMapper billMapper;

    @PostMapping
    @Transactional
    public ResponseEntity<BillDto> create(@Valid @RequestBody CreateBillRequest request) {
        Bill bill = billService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(billMapper.toDto(bill));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public BillDto get(@PathVariable UUID id) {
        return billMapper.toDto(billService.getOrThrow(id));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<BillDto> list(Pageable pageable) {
        return billService.list(pageable).map(billMapper::toDto);
    }

    @GetMapping("/open")
    @Transactional(readOnly = true)
    public List<BillDto> listOpen() {
        return billService.listOpen().stream().map(billMapper::toDto).toList();
    }

    @PostMapping("/{id}/receive")
    @Transactional
    public BillDto receive(@PathVariable UUID id) {
        return billMapper.toDto(billService.receive(id));
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public BillDto cancel(@PathVariable UUID id) {
        return billMapper.toDto(billService.cancel(id));
    }
}
