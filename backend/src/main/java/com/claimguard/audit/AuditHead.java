package com.claimguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_head")
@Getter
@Setter
public class AuditHead {

    public static final String ID = "GLOBAL";

    @Id
    @Column(length = 16)
    private String id;

    @Column(nullable = false)
    private long seq;

    @Column(nullable = false, length = 64)
    private String hash;
}
