package com.accountingapp.ar;

import com.accountingapp.ar.dto.CreateCustomerRequest;
import com.accountingapp.ar.dto.CustomerDto;
import com.accountingapp.ar.dto.UpdateCustomerRequest;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @PostMapping
    public ResponseEntity<CustomerDto> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerMapper.toDto(customer));
    }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable UUID id) {
        return customerMapper.toDto(customerService.getOrThrow(id));
    }

    @GetMapping
    public List<CustomerDto> list() {
        return customerService.list().stream().map(customerMapper::toDto).toList();
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        return customerMapper.toDto(customerService.update(id, request));
    }
}
