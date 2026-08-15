package com.konductor.projector.repository;

import com.konductor.projector.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriptionUidAndActiveTrue(String subscriptionUid);

    List<Subscription> findByActiveTrueOrderByCreatedAtDesc();
}
