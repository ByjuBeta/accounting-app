package com.accountingapp.ap;

import com.accountingapp.ap.dto.CreateVendorRequest;
import com.accountingapp.ap.dto.UpdateVendorRequest;
import com.accountingapp.ap.dto.VendorDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;
    private final VendorMapper vendorMapper;

    @PostMapping
    public ResponseEntity<VendorDto> create(@Valid @RequestBody CreateVendorRequest request) {
        Vendor vendor = vendorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorMapper.toDto(vendor));
    }

    @GetMapping("/{id}")
    public VendorDto get(@PathVariable UUID id) {
        return vendorMapper.toDto(vendorService.getOrThrow(id));
    }

    @GetMapping
    public List<VendorDto> list() {
        return vendorService.list().stream().map(vendorMapper::toDto).toList();
    }

    @PutMapping("/{id}")
    public VendorDto update(@PathVariable UUID id, @Valid @RequestBody UpdateVendorRequest request) {
        return vendorMapper.toDto(vendorService.update(id, request));
    }
}
