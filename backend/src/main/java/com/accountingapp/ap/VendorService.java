package com.accountingapp.ap;

import com.accountingapp.ap.dto.CreateVendorRequest;
import com.accountingapp.ap.dto.UpdateVendorRequest;
import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.organization.Organization;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public Vendor create(CreateVendorRequest request) {
        Vendor vendor = Vendor.builder()
                .name(request.name())
                .companyName(request.companyName())
                .email(request.email())
                .phone(request.phone())
                .address(request.address() != null ? vendorMapper.toEntity(request.address()) : null)
                .paymentTermsDays(request.paymentTermsDays() != null ? request.paymentTermsDays() : 30)
                .earlyPaymentDiscountPercent(
                        request.earlyPaymentDiscountPercent() != null ? request.earlyPaymentDiscountPercent() : BigDecimal.ZERO)
                .earlyPaymentDiscountDays(request.earlyPaymentDiscountDays() != null ? request.earlyPaymentDiscountDays() : 0)
                .taxId(request.taxId())
                .notes(request.notes())
                .status(VendorStatus.ACTIVE)
                .build();
        vendor.setOrganization(entityManager.getReference(Organization.class, OrganizationContext.getRequired()));

        Vendor saved = vendorRepository.save(vendor);
        auditLogService.record("CREATE", "Vendor", saved.getId(), "Created vendor " + saved.getName(), null);
        return saved;
    }

    @Transactional
    public Vendor update(UUID vendorId, UpdateVendorRequest request) {
        Vendor vendor = getOrThrow(vendorId);
        vendor.setName(request.name());
        vendor.setCompanyName(request.companyName());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setAddress(request.address() != null ? vendorMapper.toEntity(request.address()) : null);
        if (request.paymentTermsDays() != null) {
            vendor.setPaymentTermsDays(request.paymentTermsDays());
        }
        if (request.earlyPaymentDiscountPercent() != null) {
            vendor.setEarlyPaymentDiscountPercent(request.earlyPaymentDiscountPercent());
        }
        if (request.earlyPaymentDiscountDays() != null) {
            vendor.setEarlyPaymentDiscountDays(request.earlyPaymentDiscountDays());
        }
        vendor.setTaxId(request.taxId());
        vendor.setNotes(request.notes());
        if (request.status() != null) {
            vendor.setStatus(request.status());
        }

        Vendor saved = vendorRepository.save(vendor);
        auditLogService.record("UPDATE", "Vendor", saved.getId(), "Updated vendor " + saved.getName(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public Vendor getOrThrow(UUID vendorId) {
        return vendorRepository.findByIdAndOrganizationId(vendorId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
    }

    @Transactional(readOnly = true)
    public List<Vendor> list() {
        return vendorRepository.findByOrganizationIdOrderByNameAsc(OrganizationContext.getRequired());
    }
}
