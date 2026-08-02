package com.accountingapp.organization;

import com.accountingapp.organization.dto.CreateOrganizationRequest;
import com.accountingapp.organization.dto.OrganizationDto;
import com.accountingapp.organization.dto.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationMapper organizationMapper;

    @PostMapping
    public ResponseEntity<OrganizationDto> create(@Valid @RequestBody CreateOrganizationRequest request) {
        Organization organization = organizationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationMapper.toDto(organization));
    }

    @GetMapping("/{id}")
    public OrganizationDto get(@PathVariable UUID id) {
        return organizationMapper.toDto(organizationService.getOrThrow(id));
    }

    @GetMapping
    public List<OrganizationDto> list() {
        return organizationService.listForCurrentUser().stream().map(organizationMapper::toDto).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.isOrgAdmin()")
    public OrganizationDto update(@PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationMapper.toDto(organizationService.update(id, request));
    }
}
