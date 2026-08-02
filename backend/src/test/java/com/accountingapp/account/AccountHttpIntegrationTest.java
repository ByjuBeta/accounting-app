package com.accountingapp.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.dto.AccountDto;
import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.organization.dto.CreateOrganizationRequest;
import com.accountingapp.organization.dto.OrganizationDto;
import com.accountingapp.support.AbstractHttpIntegrationTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Regression test for a real bug: fetching accounts through the actual HTTP
 * endpoint threw LazyInitializationException on the `tags` collection once
 * any account existed, because the DTO mapping happened in the controller,
 * outside the service's transaction. Service-layer tests never caught this
 * since they call the service directly rather than going through HTTP.
 */
class AccountHttpIntegrationTest extends AbstractHttpIntegrationTest {

    @Test
    void listingAccountsWithTagsThroughHttpDoesNotThrowLazyInitializationException() {
        ResponseEntity<OrganizationDto> orgResponse = restTemplate.postForEntity(
                baseUrl() + "/organizations",
                new CreateOrganizationRequest("Test Co", "Test Co LLC", "USD", "America/New_York"),
                OrganizationDto.class);
        assertThat(orgResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String organizationId = orgResponse.getBody().id().toString();

        ResponseEntity<AccountDto> createResponse = restTemplate.exchange(
                baseUrl() + "/chart-of-accounts",
                org.springframework.http.HttpMethod.POST,
                withOrg(organizationId, new CreateAccountRequest(
                        "1000", "Checking", null, AccountType.BANK, null, null, Set.of("operating"), null, null)),
                AccountDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<AccountDto[]> listResponse = restTemplate.exchange(
                baseUrl() + "/chart-of-accounts", org.springframework.http.HttpMethod.GET,
                withOrg(organizationId), AccountDto[].class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<AccountDto> accounts = List.of(listResponse.getBody());
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).tags()).containsExactly("operating");
    }
}
