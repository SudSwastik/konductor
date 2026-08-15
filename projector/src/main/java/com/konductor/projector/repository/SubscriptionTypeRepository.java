package com.konductor.projector.repository;

import com.konductor.projector.entity.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionTypeRepository extends JpaRepository<SubscriptionType, Short> {
    List<SubscriptionType> findByActiveTrueOrderByIdAsc();

    boolean existsByIdAndActiveTrue(Short id);
}
