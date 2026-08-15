package com.konductor.projector.repository;

import com.konductor.projector.entity.DeliveryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryConfigRepository extends JpaRepository<DeliveryConfig, Long> {
    Optional<DeliveryConfig> findFirstBySubscriptionIdAndActiveTrueOrderByIdDesc(Long subscriptionId);

    List<DeliveryConfig> findBySubscriptionIdOrderByIdDesc(Long subscriptionId);
}
