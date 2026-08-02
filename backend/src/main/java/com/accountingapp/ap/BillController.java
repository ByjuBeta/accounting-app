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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final BillMapper billMapper;

    @PostMapping
    public ResponseEntity<BillDto> create(@Valid @RequestBody CreateBillRequest request) {
        Bill bill = billService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(billMapper.toDto(bill));
    }

    @GetMapping("/{id}")
    public BillDto get(@PathVariable UUID id) {
        return billMapper.toDto(billService.getOrThrow(id));
    }

    @GetMapping
    public Page<BillDto> list(Pageable pageable) {
        return billService.list(pageable).map(billMapper::toDto);
    }

    @GetMapping("/open")
    public List<BillDto> listOpen() {
        return billService.listOpen().stream().map(billMapper::toDto).toList();
    }

    @PostMapping("/{id}/receive")
    public BillDto receive(@PathVariable UUID id) {
        return billMapper.toDto(billService.receive(id));
    }

    @PostMapping("/{id}/cancel")
    public BillDto cancel(@PathVariable UUID id) {
        return billMapper.toDto(billService.cancel(id));
    }
}
