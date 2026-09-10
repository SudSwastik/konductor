package com.konductor.projector.repository;

import com.konductor.projector.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriptionUid(String subscriptionUid);

    List<Subscription> findAllByOrderByCreatedAtDesc();

    List<Subscription> findBySubscriptionStatusIdAndActivatedAtLessThanEqualAndActiveTrue(
            Short subscriptionStatusId,
            Instant activatedAt
    );
}
