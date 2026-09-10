package com.konductor.projector.repository;

import com.konductor.projector.entity.EventTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventTriggerTypeRepository extends JpaRepository<EventTriggerType, Short> {
    List<EventTriggerType> findByActiveTrueOrderByIdAsc();

    long countByIdInAndActiveTrue(Collection<Short> ids);

    List<EventTriggerType> findByCodeInAndActiveTrue(Collection<String> codes);

    List<EventTriggerType> findByIdInAndActiveTrue(Collection<Short> ids);

    Optional<EventTriggerType> findByCodeAndActiveTrue(String code);
}
