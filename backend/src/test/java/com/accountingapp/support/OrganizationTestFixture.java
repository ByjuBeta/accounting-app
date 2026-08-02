package com.accountingapp.support;

import com.accountingapp.organization.Organization;
import com.accountingapp.organization.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrganizationTestFixture {

    @Autowired
    private OrganizationRepository organizationRepository;

    public Organization createOrganization() {
        Organization organization = Organization.builder()
                .name("Test Co")
                .legalName("Test Co LLC")
                .baseCurrencyCode("USD")
                .timeZone("UTC")
                .active(true)
                .build();
        return organizationRepository.save(organization);
    }
}
