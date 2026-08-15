package com.konductor.projector.repository;

import com.konductor.projector.entity.ParameterDataType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParameterDataTypeRepository extends JpaRepository<ParameterDataType, Short> {
    List<ParameterDataType> findByActiveTrueOrderByIdAsc();
}
