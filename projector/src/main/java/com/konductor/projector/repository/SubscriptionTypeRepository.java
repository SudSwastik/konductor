package com.konductor.projector.repository;

import com.konductor.projector.entity.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionTypeRepository extends JpaRepository<SubscriptionType, Short> {
    List<SubscriptionType> findByActiveTrueOrderByIdAsc();

    boolean existsByIdAndActiveTrue(Short id);

    Optional<SubscriptionType> findByCodeAndActiveTrue(String code);
}
