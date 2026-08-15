package com.konductor.projector.repository;

import com.konductor.projector.entity.EventTriggerSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EventTriggerSelectionRepository extends JpaRepository<EventTriggerSelection, Long> {
    List<EventTriggerSelection> findBySubscriptionId(Long subscriptionId);

    List<EventTriggerSelection> findBySubscriptionIdAndActiveTrue(Long subscriptionId);

    List<EventTriggerSelection> findBySubscriptionIdAndEventTriggerTypeIdOrderByIdAsc(Long subscriptionId, Short eventTriggerTypeId);

    List<EventTriggerSelection> findByIdInAndActiveTrue(Collection<Long> ids);
}
