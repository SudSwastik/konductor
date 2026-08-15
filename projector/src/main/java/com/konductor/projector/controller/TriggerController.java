package com.konductor.projector.controller;

import com.konductor.projector.dto.MasterDataResponse;
import com.konductor.projector.entity.EventTriggerType;
import com.konductor.projector.repository.EventTriggerTypeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TriggerController {
    private final EventTriggerTypeRepository eventTriggerTypeRepository;

    public TriggerController(EventTriggerTypeRepository eventTriggerTypeRepository) {
        this.eventTriggerTypeRepository = eventTriggerTypeRepository;
    }

    @GetMapping("/triggers")
    public List<MasterDataResponse> eventTriggerTypes() {
        return eventTriggerTypeRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toMasterDataResponse)
                .toList();
    }

    private MasterDataResponse toMasterDataResponse(EventTriggerType value) {
        return new MasterDataResponse(value.getId(), value.getCode(), value.getName(), value.getDescription());
    }
}
