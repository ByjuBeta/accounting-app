package com.accountingapp.ar;

import com.accountingapp.audit.AuditLogService;
import com.accountingapp.common.context.OrganizationContext;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.ar.dto.CreateCustomerRequest;
import com.accountingapp.ar.dto.UpdateCustomerRequest;
import com.accountingapp.organization.Organization;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    @Transactional
    public Customer create(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.name())
                .companyName(request.companyName())
                .email(request.email())
                .phone(request.phone())
                .billingAddress(request.billingAddress() != null ? customerMapper.toEntity(request.billingAddress()) : null)
                .paymentTermsDays(request.paymentTermsDays() != null ? request.paymentTermsDays() : 30)
                .taxExempt(request.taxExempt())
                .notes(request.notes())
                .status(CustomerStatus.ACTIVE)
                .build();
        customer.setOrganization(orgReference());

        Customer saved = customerRepository.save(customer);
        auditLogService.record("CREATE", "Customer", saved.getId(), "Created customer " + saved.getName(), null);
        return saved;
    }

    @Transactional
    public Customer update(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = getOrThrow(customerId);
        customer.setName(request.name());
        customer.setCompanyName(request.companyName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setBillingAddress(request.billingAddress() != null ? customerMapper.toEntity(request.billingAddress()) : null);
        if (request.paymentTermsDays() != null) {
            customer.setPaymentTermsDays(request.paymentTermsDays());
        }
        customer.setTaxExempt(request.taxExempt());
        customer.setNotes(request.notes());
        if (request.status() != null) {
            customer.setStatus(request.status());
        }

        Customer saved = customerRepository.save(customer);
        auditLogService.record("UPDATE", "Customer", saved.getId(), "Updated customer " + saved.getName(), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public Customer getOrThrow(UUID customerId) {
        return customerRepository.findByIdAndOrganizationId(customerId, OrganizationContext.getRequired())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    @Transactional(readOnly = true)
    public List<Customer> list() {
        return customerRepository.findByOrganizationIdOrderByNameAsc(OrganizationContext.getRequired());
    }

    private Organization orgReference() {
        return entityManager.getReference(Organization.class, OrganizationContext.getRequired());
    }
}
