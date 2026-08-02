package com.accountingapp.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.AccountType;
import com.accountingapp.account.dto.AccountDto;
import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.journal.dto.CreateJournalEntryRequest;
import com.accountingapp.journal.dto.JournalEntryDto;
import com.accountingapp.journal.dto.JournalEntryLineRequest;
import com.accountingapp.organization.dto.CreateOrganizationRequest;
import com.accountingapp.organization.dto.OrganizationDto;
import com.accountingapp.support.AbstractHttpIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Regression test for a real bug: fetching a journal entry through the
 * actual HTTP endpoint threw LazyInitializationException on the `lines`
 * collection (and each line's account name/code), because the DTO mapping
 * happened in the controller, outside the service's transaction.
 */
class JournalEntryHttpIntegrationTest extends AbstractHttpIntegrationTest {

    @Test
    void creatingAndPostingAnEntryThroughHttpDoesNotThrowLazyInitializationException() {
        String organizationId = restTemplate.postForEntity(
                        baseUrl() + "/organizations",
                        new CreateOrganizationRequest("Test Co", "Test Co LLC", "USD", "America/New_York"),
                        OrganizationDto.class)
                .getBody().id().toString();

        AccountDto bank = restTemplate.exchange(
                        baseUrl() + "/chart-of-accounts", HttpMethod.POST,
                        withOrg(organizationId, new CreateAccountRequest(
                                "1000", "Checking", null, AccountType.BANK, null, null, Set.of(), null, null)),
                        AccountDto.class)
                .getBody();
        AccountDto revenue = restTemplate.exchange(
                        baseUrl() + "/chart-of-accounts", HttpMethod.POST,
                        withOrg(organizationId, new CreateAccountRequest(
                                "4000", "Sales Revenue", null, AccountType.INCOME, null, null, Set.of(), null, null)),
                        AccountDto.class)
                .getBody();

        CreateJournalEntryRequest createRequest = new CreateJournalEntryRequest(
                LocalDate.now(), TransactionType.DEPOSIT, "Test", null, "USD",
                List.of(
                        new JournalEntryLineRequest(bank.id(), new BigDecimal("100.00"), BigDecimal.ZERO, null, Set.of()),
                        new JournalEntryLineRequest(revenue.id(), BigDecimal.ZERO, new BigDecimal("100.00"), null, Set.of())));

        ResponseEntity<JournalEntryDto> createResponse = restTemplate.exchange(
                baseUrl() + "/journal-entries", HttpMethod.POST,
                withOrg(organizationId, createRequest), JournalEntryDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JournalEntryDto created = createResponse.getBody();
        assertThat(created.lines()).hasSize(2);
        assertThat(created.lines().get(0).accountCode()).isNotBlank();

        ResponseEntity<JournalEntryDto> postResponse = restTemplate.exchange(
                baseUrl() + "/journal-entries/" + created.id() + "/post", HttpMethod.POST,
                withOrg(organizationId), JournalEntryDto.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postResponse.getBody().status()).isEqualTo(TransactionStatus.POSTED);

        ResponseEntity<JournalEntryDto> getResponse = restTemplate.exchange(
                baseUrl() + "/journal-entries/" + created.id(), HttpMethod.GET,
                withOrg(organizationId), JournalEntryDto.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().lines()).extracting("accountName").containsExactly("Checking", "Sales Revenue");
    }
}
