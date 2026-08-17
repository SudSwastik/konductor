package com.konductor.projector.repository;

import com.konductor.projector.entity.ParameterDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ParameterDefinitionRepository extends JpaRepository<ParameterDefinition, Long> {
    List<ParameterDefinition> findByActiveTrueOrderByIdAsc();

    List<ParameterDefinition> findByIdInAndActiveTrue(Collection<Long> ids);

    long countByIdInAndActiveTrue(Collection<Long> ids);
}
