package com.konductor.projector.controller;

import com.konductor.projector.dto.MasterDataResponse;
import com.konductor.projector.dto.ParameterDefinitionResponse;
import com.konductor.projector.entity.ParameterDataType;
import com.konductor.projector.entity.ParameterDefinition;
import com.konductor.projector.repository.ParameterDataTypeRepository;
import com.konductor.projector.repository.ParameterDefinitionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ParameterController {
    private final ParameterDataTypeRepository parameterDataTypeRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;

    public ParameterController(
            ParameterDataTypeRepository parameterDataTypeRepository,
            ParameterDefinitionRepository parameterDefinitionRepository
    ) {
        this.parameterDataTypeRepository = parameterDataTypeRepository;
        this.parameterDefinitionRepository = parameterDefinitionRepository;
    }

    @GetMapping("/parameters")
    public List<ParameterDefinitionResponse> parameterDefinitions() {
        return parameterDefinitionRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toParameterDefinitionResponse)
                .toList();
    }

    private MasterDataResponse toMasterDataResponse(ParameterDataType value) {
        return new MasterDataResponse(value.getId(), value.getCode(), value.getName(), value.getDescription());
    }

    private ParameterDefinitionResponse toParameterDefinitionResponse(ParameterDefinition value) {
        return new ParameterDefinitionResponse(
                value.getId(),
                value.getDataTypeId(),
                value.getName(),
                value.getDescription(),
                value.getFieldPath(),
                value.isRequired()
        );
    }
}
