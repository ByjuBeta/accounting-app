package com.accountingapp.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * Immutable record of who changed what, when, and from where. Deliberately
 * not a {@code BaseEntity} — audit rows are append-only and never updated,
 * so there's no version/updatedAt to carry.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor
@SuperBuilder
public class AuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @Column(name = "action", nullable = false, updatable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 128)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "summary", updatable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
