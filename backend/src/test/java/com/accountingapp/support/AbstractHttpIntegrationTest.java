package com.accountingapp.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

    protected String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    protected HttpEntity<Void> withOrg(String organizationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Organization-Id", organizationId);
        return new HttpEntity<>(headers);
    }

    protected <T> HttpEntity<T> withOrg(String organizationId, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Organization-Id", organizationId);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
