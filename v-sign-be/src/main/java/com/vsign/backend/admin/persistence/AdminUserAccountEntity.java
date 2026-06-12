package com.vsign.backend.admin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_user_accounts")
public class AdminUserAccountEntity {
    @Id
    private String id;

    private String email;

    @Column(name = "display_name")
    private String displayName;

    private String role;
    private String status;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected AdminUserAccountEntity() {
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getAccountType() {
        return accountType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
