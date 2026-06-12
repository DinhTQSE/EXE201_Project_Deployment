package com.vsign.backend.admin.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserAccountRepository extends JpaRepository<AdminUserAccountEntity, String> {
    List<AdminUserAccountEntity> findAllByOrderByIdAsc();

    int countByStatus(String status);

    int countByAccountType(String accountType);
}
