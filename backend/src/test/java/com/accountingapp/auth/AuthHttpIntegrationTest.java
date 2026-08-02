package com.accountingapp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.accountingapp.account.AccountType;
import com.accountingapp.account.dto.AccountDto;
import com.accountingapp.account.dto.CreateAccountRequest;
import com.accountingapp.auth.dto.AuthResponse;
import com.accountingapp.auth.dto.LoginRequest;
import com.accountingapp.auth.dto.MeResponse;
import com.accountingapp.auth.dto.RefreshRequest;
import com.accountingapp.auth.dto.RegisterRequest;
import com.accountingapp.common.exception.ErrorResponse;
import com.accountingapp.organization.dto.CreateOrganizationRequest;
import com.accountingapp.organization.dto.OrganizationDto;
import com.accountingapp.support.AbstractHttpIntegrationTest;
import com.accountingapp.user.Role;
import com.accountingapp.user.dto.AddMemberRequest;
import com.accountingapp.user.dto.MemberDto;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AuthHttpIntegrationTest extends AbstractHttpIntegrationTest {

    @Test
    void registerLoginRefreshAndLogoutRoundTrip() {
        String email = "roundtrip-" + UUID.randomUUID() + "@example.com";

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequest(email, "Correct-Horse-9", "Ada", "Lovelace"),
                AuthResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody().accessToken()).isNotBlank();
        assertThat(registerResponse.getBody().memberships()).isEmpty();

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login", new LoginRequest(email, "Correct-Horse-9"), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String refreshToken = loginResponse.getBody().refreshToken();

        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.setBearerAuth(loginResponse.getBody().accessToken());
        ResponseEntity<MeResponse> meResponse = restTemplate.exchange(
                baseUrl() + "/auth/me", HttpMethod.GET, new HttpEntity<>(meHeaders), MeResponse.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().user().email()).isEqualTo(email.toLowerCase());

        ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/refresh", new RefreshRequest(refreshToken), AuthResponse.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(refreshToken);

        // the rotated-out refresh token must no longer work
        ResponseEntity<ErrorResponse> reuseResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/refresh", new RefreshRequest(refreshToken), ErrorResponse.class);
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/logout", new RefreshRequest(refreshResponse.getBody().refreshToken()), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ErrorResponse> afterLogoutResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/refresh",
                new RefreshRequest(refreshResponse.getBody().refreshToken()),
                ErrorResponse.class);
        assertThat(afterLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        String email = "badpw-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequest(email, "Correct-Horse-9", "Grace", "Hopper"),
                AuthResponse.class);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                baseUrl() + "/auth/login", new LoginRequest(email, "wrong-password"), ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void protectedEndpointRejectsRequestsWithNoToken() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                baseUrl() + "/organizations", ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void organizationCreatorBecomesAdminAndCanManageMembers() {
        registerAndLogin();
        OrganizationDto org = postAuthed(
                        "/organizations",
                        new CreateOrganizationRequest("Member Test Co", null, "USD", "America/New_York"),
                        OrganizationDto.class)
                .getBody();

        // register a second user to add as a VIEWER
        String viewerEmail = "viewer-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequest(viewerEmail, "Correct-Horse-9", "View", "Only"),
                AuthResponse.class);

        ResponseEntity<MemberDto> addResponse = restTemplate.exchange(
                baseUrl() + "/members", HttpMethod.POST,
                withOrg(org.id().toString(), new AddMemberRequest(viewerEmail, Role.VIEWER)), MemberDto.class);
        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(addResponse.getBody().role()).isEqualTo(Role.VIEWER);

        // log in as the viewer and confirm they can read but not write
        ResponseEntity<AuthResponse> viewerLogin = restTemplate.postForEntity(
                baseUrl() + "/auth/login", new LoginRequest(viewerEmail, "Correct-Horse-9"), AuthResponse.class);
        HttpHeaders viewerHeaders = new HttpHeaders();
        viewerHeaders.setBearerAuth(viewerLogin.getBody().accessToken());
        viewerHeaders.set("X-Organization-Id", org.id().toString());

        ResponseEntity<AccountDto[]> viewerRead = restTemplate.exchange(
                baseUrl() + "/chart-of-accounts", HttpMethod.GET, new HttpEntity<>(viewerHeaders), AccountDto[].class);
        assertThat(viewerRead.getStatusCode()).isEqualTo(HttpStatus.OK);

        viewerHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<ErrorResponse> viewerWrite = restTemplate.exchange(
                baseUrl() + "/chart-of-accounts", HttpMethod.POST,
                new HttpEntity<>(
                        new CreateAccountRequest("9999", "Nope", null, AccountType.BANK, null, null, Set.of(), null, null),
                        viewerHeaders),
                ErrorResponse.class);
        assertThat(viewerWrite.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(viewerWrite.getBody().errorCode()).isEqualTo("READ_ONLY_ROLE");

        // the viewer (non-admin) cannot add other members
        ResponseEntity<ErrorResponse> viewerAddMember = restTemplate.exchange(
                baseUrl() + "/members", HttpMethod.POST,
                new HttpEntity<>(new AddMemberRequest(viewerEmail, Role.ADMIN), viewerHeaders),
                ErrorResponse.class);
        assertThat(viewerAddMember.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void lastAdminCannotBeRemoved() {
        registerAndLogin();
        OrganizationDto org = postAuthed(
                        "/organizations",
                        new CreateOrganizationRequest("Solo Admin Co", null, "USD", "America/New_York"),
                        OrganizationDto.class)
                .getBody();

        ResponseEntity<MemberDto[]> members = restTemplate.exchange(
                baseUrl() + "/members", HttpMethod.GET, withOrg(org.id().toString()), MemberDto[].class);
        assertThat(members.getBody()).hasSize(1);
        UUID onlyMemberId = members.getBody()[0].id();

        ResponseEntity<ErrorResponse> removeResponse = restTemplate.exchange(
                baseUrl() + "/members/" + onlyMemberId, HttpMethod.DELETE,
                withOrg(org.id().toString()), ErrorResponse.class);
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(removeResponse.getBody().errorCode()).isIn("CANNOT_REMOVE_SELF", "LAST_ADMIN");
    }
}
