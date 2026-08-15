package com.konductor.projector.controller;

import com.konductor.projector.dto.ReplaceParametersRequest;
import com.konductor.projector.dto.ReplaceTriggersRequest;
import com.konductor.projector.dto.SubscriptionCreateRequest;
import com.konductor.projector.dto.SubscriptionPatchRequest;
import com.konductor.projector.dto.SubscriptionResponse;
import com.konductor.projector.dto.SubscriptionSummaryResponse;
import com.konductor.projector.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping
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

    @GetMapping("/{subscriptionUid}")
    public SubscriptionResponse get(@PathVariable String subscriptionUid) {
        return subscriptionService.get(subscriptionUid);
    }

    @PatchMapping("/{subscriptionUid}")
    public SubscriptionResponse patchBasic(
            @PathVariable String subscriptionUid,
            @RequestBody SubscriptionPatchRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.patchBasic(subscriptionUid, request, actor);
    }

    @PutMapping("/{subscriptionUid}/parameters")
    public SubscriptionResponse replaceParameters(
            @PathVariable String subscriptionUid,
            @Valid @RequestBody ReplaceParametersRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.replaceParameters(subscriptionUid, request, actor);
    }

    @PutMapping("/{subscriptionUid}/triggers")
    public SubscriptionResponse replaceTriggers(
            @PathVariable String subscriptionUid,
            @RequestBody ReplaceTriggersRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor
    ) {
        return subscriptionService.replaceTriggers(subscriptionUid, request, actor);
    }
}
