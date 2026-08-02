package com.accountingapp.banking;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountDetailRepository extends JpaRepository<BankAccountDetail, UUID> {

    Optional<BankAccountDetail> findByAccountId(UUID accountId);
}
