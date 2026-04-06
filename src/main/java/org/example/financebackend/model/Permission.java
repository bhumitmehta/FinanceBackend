package org.example.financebackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique dotted name, e.g. "records:write" */
    @Column(nullable = false, unique = true)
    private String name;

    /** Coarse grouping, e.g. "records" */
    @Column(nullable = false)
    private String resource;

    /** Fine-grained verb, e.g. "write" */
    @Column(nullable = false)
    private String action;

    @Column
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
