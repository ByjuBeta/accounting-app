package com.accountingapp.support;

import com.accountingapp.auth.dto.AuthResponse;
import com.accountingapp.auth.dto.RegisterRequest;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the full servlet container so tests exercise real HTTP requests
 * through the controller layer — unlike {@link AbstractIntegrationTest},
 * which calls services directly and can't catch bugs that only surface
 * when DTO mapping happens outside a service's transaction (e.g. lazy
 * association access in the controller).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractHttpIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    protected int port;

    @org.springframework.beans.factory.annotation.Autowired
    protected TestRestTemplate restTemplate;

    protected String accessToken;

    protected String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    /** Registers a fresh user and stashes their access token for subsequent {@code withOrg}/{@code authed} calls. */
    protected void registerAndLogin() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        AuthResponse response = restTemplate.postForEntity(
                        baseUrl() + "/auth/register",
                        new RegisterRequest(email, "Test-Password-123", "Test", "User"),
                        AuthResponse.class)
                .getBody();
        accessToken = response.accessToken();
    }

    protected HttpEntity<Void> withOrg(String organizationId) {
        HttpHeaders headers = authHeaders();
        headers.set("X-Organization-Id", organizationId);
        return new HttpEntity<>(headers);
    }

    protected <T> HttpEntity<T> withOrg(String organizationId, T body) {
        HttpHeaders headers = authHeaders();
        headers.set("X-Organization-Id", organizationId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /** For endpoints that need authentication but no organization context yet (e.g. creating an org). */
    protected <T> HttpEntity<T> authed(T body) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    protected <T> org.springframework.http.ResponseEntity<T> postAuthed(String path, Object body, Class<T> responseType) {
        return restTemplate.exchange(baseUrl() + path, HttpMethod.POST, authed(body), responseType);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return headers;
    }
}
