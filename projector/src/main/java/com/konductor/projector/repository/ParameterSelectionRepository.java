package com.konductor.projector.repository;

import com.konductor.projector.entity.ParameterSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ParameterSelectionRepository extends JpaRepository<ParameterSelection, Long> {
    List<ParameterSelection> findByEventTriggerSelectionIdInAndActiveTrue(Collection<Long> eventTriggerSelectionIds);

    List<ParameterSelection> findByEventTriggerSelectionIdAndActiveTrue(Long eventTriggerSelectionId);

    List<ParameterSelection> findByEventTriggerSelectionIdIn(Collection<Long> eventTriggerSelectionIds);

    List<ParameterSelection> findByEventTriggerSelectionIdAndParameterDefinitionIdOrderByIdAsc(
            Long eventTriggerSelectionId,
            Long parameterDefinitionId
    );
}
