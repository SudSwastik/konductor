package com.konductor.projector.repository;

import com.konductor.projector.entity.EventTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EventTriggerTypeRepository extends JpaRepository<EventTriggerType, Short> {
    List<EventTriggerType> findByActiveTrueOrderByIdAsc();

    long countByIdInAndActiveTrue(Collection<Short> ids);
}
