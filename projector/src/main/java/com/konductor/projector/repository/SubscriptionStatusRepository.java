package com.konductor.projector.repository;

import com.konductor.projector.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionStatusRepository extends JpaRepository<SubscriptionStatus, Short> {
    List<SubscriptionStatus> findByActiveTrueOrderByIdAsc();

    boolean existsByIdAndActiveTrue(Short id);

    Optional<SubscriptionStatus> findByCodeAndActiveTrue(String code);
}
