package com.konductor.projector.controller;

import com.konductor.projector.dto.ReplaceParametersRequest;
import com.konductor.projector.dto.ReplaceTriggersRequest;
import com.konductor.projector.dto.SubscriptionCreateRequest;
import com.konductor.projector.dto.SubscriptionPatchRequest;
import com.konductor.projector.dto.SubscriptionResponse;
import com.konductor.projector.dto.SubscriptionStatusPatchRequest;
import com.konductor.projector.dto.SubscriptionSummaryResponse;
import com.konductor.projector.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {
    private static final String ACTOR_HEADER = "X-User-Email";

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(
            @Valid @RequestBody SubscriptionCreateRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.create(request, actor);
    }

    @GetMapping
    public List<SubscriptionSummaryResponse> list() {
        return subscriptionService.list();
    }

    @GetMapping("/{subscriptionId}")
    public SubscriptionResponse get(@PathVariable String subscriptionId) {
        return subscriptionService.get(subscriptionId);
    }

    @PatchMapping("/{subscriptionId}")
    public SubscriptionResponse patchBasic(
            @PathVariable String subscriptionId,
            @Valid @RequestBody SubscriptionPatchRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.patchBasic(subscriptionId, request, actor);
    }

    @PatchMapping("/{subscriptionId}/status")
    public SubscriptionResponse patchStatus(
            @PathVariable String subscriptionId,
            @Valid @RequestBody SubscriptionStatusPatchRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.patchStatus(subscriptionId, request, actor);
    }

    @PutMapping("/{subscriptionId}/parameters")
    public SubscriptionResponse replaceParameters(
            @PathVariable String subscriptionId,
            @Valid @RequestBody ReplaceParametersRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.replaceParameters(subscriptionId, request, actor);
    }

    @PutMapping("/{subscriptionId}/triggers")
    public SubscriptionResponse replaceTriggers(
            @PathVariable String subscriptionId,
            @Valid @RequestBody ReplaceTriggersRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.replaceTriggers(subscriptionId, request, actor);
    }

    @DeleteMapping("/{subscriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String subscriptionId,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        subscriptionService.softDelete(subscriptionId, actor);
    }
}
