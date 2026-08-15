package com.konductor.projector.service;

import com.konductor.projector.dto.DeliveryConfigRequest;
import com.konductor.projector.dto.DeliveryConfigResponse;
import com.konductor.projector.dto.EventTriggerParameterRequest;
import com.konductor.projector.dto.EventTriggerSelectionResponse;
import com.konductor.projector.dto.ReplaceParametersRequest;
import com.konductor.projector.dto.ReplaceTriggersRequest;
import com.konductor.projector.dto.SubscriptionCreateRequest;
import com.konductor.projector.dto.SubscriptionPatchRequest;
import com.konductor.projector.dto.SubscriptionResponse;
import com.konductor.projector.dto.SubscriptionSummaryResponse;
import com.konductor.projector.entity.DeliveryConfig;
import com.konductor.projector.entity.EventTriggerSelection;
import com.konductor.projector.entity.ParameterSelection;
import com.konductor.projector.entity.Subscription;
import com.konductor.projector.repository.DeliveryConfigRepository;
import com.konductor.projector.repository.EventTriggerSelectionRepository;
import com.konductor.projector.repository.EventTriggerTypeRepository;
import com.konductor.projector.repository.ParameterDefinitionRepository;
import com.konductor.projector.repository.ParameterSelectionRepository;
import com.konductor.projector.repository.SubscriptionRepository;
import com.konductor.projector.repository.SubscriptionStatusRepository;
import com.konductor.projector.repository.SubscriptionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SubscriptionService {
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTypeRepository subscriptionTypeRepository;
    private final SubscriptionStatusRepository subscriptionStatusRepository;
    private final EventTriggerTypeRepository eventTriggerTypeRepository;
    private final EventTriggerSelectionRepository eventTriggerSelectionRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final ParameterSelectionRepository parameterSelectionRepository;
    private final DeliveryConfigRepository deliveryConfigRepository;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionTypeRepository subscriptionTypeRepository,
            SubscriptionStatusRepository subscriptionStatusRepository,
            EventTriggerTypeRepository eventTriggerTypeRepository,
            EventTriggerSelectionRepository eventTriggerSelectionRepository,
            ParameterDefinitionRepository parameterDefinitionRepository,
            ParameterSelectionRepository parameterSelectionRepository,
            DeliveryConfigRepository deliveryConfigRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionTypeRepository = subscriptionTypeRepository;
        this.subscriptionStatusRepository = subscriptionStatusRepository;
        this.eventTriggerTypeRepository = eventTriggerTypeRepository;
        this.eventTriggerSelectionRepository = eventTriggerSelectionRepository;
        this.parameterDefinitionRepository = parameterDefinitionRepository;
        this.parameterSelectionRepository = parameterSelectionRepository;
        this.deliveryConfigRepository = deliveryConfigRepository;
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionCreateRequest request, String actor) {
        actor = actorOrSystem(actor);
        validateSubscriptionType(request.subscriptionTypeId());
        validateSubscriptionStatus(request.subscriptionStatusId());

        Subscription subscription = new Subscription();
        subscription.setSubscriptionUid(newSubscriptionUid());
        subscription.setSubscriptionTypeId(request.subscriptionTypeId());
        subscription.setSubscriptionStatusId(request.subscriptionStatusId());
        subscription.setName(request.name());
        subscription.setDescription(request.description());
        subscription.setActivatedAt(request.activatedAt());
        subscription.setDeactivatedAt(request.deactivatedAt());
        subscription.markCreated(actor);
        subscription = subscriptionRepository.save(subscription);

        if (request.deliveryConfig() != null) {
            saveDeliveryConfig(subscription.getId(), request.deliveryConfig(), actor);
        }

        if (request.triggers() != null && !request.triggers().isEmpty()) {
            replaceParameters(subscription, new ReplaceParametersRequest(request.triggers()), actor);
        }

        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionSummaryResponse> list() {
        return subscriptionRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse get(String subscriptionUid) {
        return toResponse(findActiveSubscription(subscriptionUid));
    }

    @Transactional
    public SubscriptionResponse patchBasic(String subscriptionUid, SubscriptionPatchRequest request, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findActiveSubscription(subscriptionUid);

        if (request.subscriptionTypeId() != null) {
            validateSubscriptionType(request.subscriptionTypeId());
            subscription.setSubscriptionTypeId(request.subscriptionTypeId());
        }
        if (request.subscriptionStatusId() != null) {
            validateSubscriptionStatus(request.subscriptionStatusId());
            subscription.setSubscriptionStatusId(request.subscriptionStatusId());
        }
        if (request.name() != null) {
            subscription.setName(request.name());
        }
        if (request.description() != null) {
            subscription.setDescription(request.description());
        }
        if (request.activatedAt() != null) {
            subscription.setActivatedAt(request.activatedAt());
        }
        if (request.deactivatedAt() != null) {
            subscription.setDeactivatedAt(request.deactivatedAt());
        }

        subscription.markUpdated(actor);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse replaceTriggers(String subscriptionUid, ReplaceTriggersRequest request, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findActiveSubscription(subscriptionUid);
        List<Short> triggerTypeIds = distinctList(request.eventTriggerTypeIds());
        validateTriggerTypes(triggerTypeIds);

        List<EventTriggerSelection> existingSelections = eventTriggerSelectionRepository.findBySubscriptionId(subscription.getId());
        existingSelections.forEach(selection -> selection.setActive(false));

        for (Short triggerTypeId : triggerTypeIds) {
            EventTriggerSelection selection = firstOrNewSelection(existingSelections, subscription.getId(), triggerTypeId, actor);
            selection.setActive(true);
        }

        eventTriggerSelectionRepository.saveAll(existingSelections);
        List<Long> archivedTriggerSelectionIds = existingSelections.stream()
                .filter(selection -> !selection.isActive())
                .map(EventTriggerSelection::getId)
                .filter(id -> id != null)
                .toList();
        if (!archivedTriggerSelectionIds.isEmpty()) {
            List<ParameterSelection> archivedParameterSelections =
                    parameterSelectionRepository.findByEventTriggerSelectionIdIn(archivedTriggerSelectionIds);
            archivedParameterSelections.forEach(selection -> selection.setActive(false));
            parameterSelectionRepository.saveAll(archivedParameterSelections);
        }

        subscription.markUpdated(actor);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse replaceParameters(String subscriptionUid, ReplaceParametersRequest request, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findActiveSubscription(subscriptionUid);
        return replaceParameters(subscription, request, actor);
    }

    private SubscriptionResponse replaceParameters(Subscription subscription, ReplaceParametersRequest request, String actor) {
        List<EventTriggerParameterRequest> parameterGroups = request.parameters() == null ? List.of() : request.parameters();
        List<Short> triggerTypeIds = parameterGroups.stream()
                .map(EventTriggerParameterRequest::eventTriggerTypeId)
                .distinct()
                .toList();
        validateTriggerTypes(triggerTypeIds);
        validateParameterDefinitions(parameterGroups.stream()
                .map(EventTriggerParameterRequest::parameterDefinitionIds)
                .filter(ids -> ids != null)
                .flatMap(Collection::stream)
                .distinct()
                .toList());

        List<EventTriggerSelection> existingSelections = eventTriggerSelectionRepository.findBySubscriptionId(subscription.getId());

        for (EventTriggerParameterRequest group : parameterGroups) {
            EventTriggerSelection selection = firstOrNewSelection(
                    existingSelections,
                    subscription.getId(),
                    group.eventTriggerTypeId(),
                    actor
            );
            selection.setActive(true);
        }

        eventTriggerSelectionRepository.saveAll(existingSelections);
        List<Long> selectionIds = existingSelections.stream()
                .map(EventTriggerSelection::getId)
                .filter(id -> id != null)
                .toList();

        if (!selectionIds.isEmpty()) {
            List<ParameterSelection> existingParameterSelections =
                    parameterSelectionRepository.findByEventTriggerSelectionIdIn(selectionIds);
            existingParameterSelections.forEach(selection -> selection.setActive(false));

            for (EventTriggerParameterRequest group : parameterGroups) {
                EventTriggerSelection triggerSelection = firstOrNewSelection(
                        existingSelections,
                        subscription.getId(),
                        group.eventTriggerTypeId(),
                        actor
                );
                List<Long> parameterDefinitionIds = distinctList(group.parameterDefinitionIds());

                for (Long parameterDefinitionId : parameterDefinitionIds) {
                    ParameterSelection parameterSelection = firstOrNewParameterSelection(
                            existingParameterSelections,
                            triggerSelection.getId(),
                            parameterDefinitionId,
                            actor
                    );
                    parameterSelection.setActive(true);
                }
            }

            parameterSelectionRepository.saveAll(existingParameterSelections);
        }

        subscription.markUpdated(actor);
        return toResponse(subscriptionRepository.save(subscription));
    }

    private void saveDeliveryConfig(Long subscriptionId, DeliveryConfigRequest request, String actor) {
        DeliveryConfig deliveryConfig = new DeliveryConfig();
        deliveryConfig.setSubscriptionId(subscriptionId);
        deliveryConfig.setDeliveryType(request.deliveryType());
        deliveryConfig.setEndpointUrl(request.endpointUrl());
        deliveryConfig.setHttpMethod(request.httpMethod());
        if (request.timeoutSeconds() != null) {
            deliveryConfig.setTimeoutSeconds(request.timeoutSeconds());
        }
        if (request.maxRetryCount() != null) {
            deliveryConfig.setMaxRetryCount(request.maxRetryCount());
        }
        if (request.retryBackoffSeconds() != null) {
            deliveryConfig.setRetryBackoffSeconds(request.retryBackoffSeconds());
        }
        deliveryConfig.markCreated(actor);
        deliveryConfigRepository.save(deliveryConfig);
    }

    private EventTriggerSelection firstOrNewSelection(
            List<EventTriggerSelection> selections,
            Long subscriptionId,
            Short eventTriggerTypeId,
            String actor
    ) {
        for (EventTriggerSelection selection : selections) {
            if (selection.getEventTriggerTypeId().equals(eventTriggerTypeId)) {
                return selection;
            }
        }

        EventTriggerSelection selection = new EventTriggerSelection();
        selection.setSubscriptionId(subscriptionId);
        selection.setEventTriggerTypeId(eventTriggerTypeId);
        selection.markCreated(actor);
        selections.add(selection);
        return selection;
    }

    private ParameterSelection firstOrNewParameterSelection(
            List<ParameterSelection> selections,
            Long eventTriggerSelectionId,
            Long parameterDefinitionId,
            String actor
    ) {
        for (ParameterSelection selection : selections) {
            if (selection.getEventTriggerSelectionId().equals(eventTriggerSelectionId)
                    && selection.getParameterDefinitionId().equals(parameterDefinitionId)) {
                return selection;
            }
        }

        ParameterSelection selection = new ParameterSelection();
        selection.setEventTriggerSelectionId(eventTriggerSelectionId);
        selection.setParameterDefinitionId(parameterDefinitionId);
        selection.markCreated(actor);
        selections.add(selection);
        return selection;
    }

    private Subscription findActiveSubscription(String subscriptionUid) {
        return subscriptionRepository.findBySubscriptionUidAndActiveTrue(subscriptionUid)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Subscription not found"));
    }

    private void validateSubscriptionType(Short subscriptionTypeId) {
        if (!subscriptionTypeRepository.existsByIdAndActiveTrue(subscriptionTypeId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid subscriptionTypeId");
        }
    }

    private void validateSubscriptionStatus(Short subscriptionStatusId) {
        if (!subscriptionStatusRepository.existsByIdAndActiveTrue(subscriptionStatusId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid subscriptionStatusId");
        }
    }

    private void validateTriggerTypes(List<Short> triggerTypeIds) {
        if (triggerTypeIds.isEmpty()) {
            return;
        }
        long activeCount = eventTriggerTypeRepository.countByIdInAndActiveTrue(triggerTypeIds);
        if (activeCount != triggerTypeIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid eventTriggerTypeId");
        }
    }

    private void validateParameterDefinitions(List<Long> parameterDefinitionIds) {
        if (parameterDefinitionIds.isEmpty()) {
            return;
        }
        long activeCount = parameterDefinitionRepository.countByIdInAndActiveTrue(parameterDefinitionIds);
        if (activeCount != parameterDefinitionIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid parameterDefinitionId");
        }
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        List<EventTriggerSelection> triggerSelections =
                eventTriggerSelectionRepository.findBySubscriptionIdAndActiveTrue(subscription.getId());
        List<Long> triggerSelectionIds = triggerSelections.stream().map(EventTriggerSelection::getId).toList();
        List<ParameterSelection> parameterSelections = triggerSelectionIds.isEmpty()
                ? List.of()
                : parameterSelectionRepository.findByEventTriggerSelectionIdInAndActiveTrue(triggerSelectionIds);
        Map<Long, List<Long>> parameterIdsByTriggerSelectionId = new LinkedHashMap<>();
        for (ParameterSelection parameterSelection : parameterSelections) {
            parameterIdsByTriggerSelectionId
                    .computeIfAbsent(parameterSelection.getEventTriggerSelectionId(), ignored -> new ArrayList<>())
                    .add(parameterSelection.getParameterDefinitionId());
        }

        List<EventTriggerSelectionResponse> triggerResponses = triggerSelections.stream()
                .map(selection -> new EventTriggerSelectionResponse(
                        selection.getEventTriggerTypeId(),
                        parameterIdsByTriggerSelectionId.getOrDefault(selection.getId(), List.of())
                ))
                .toList();

        DeliveryConfigResponse deliveryConfig = deliveryConfigRepository
                .findFirstBySubscriptionIdAndActiveTrueOrderByIdDesc(subscription.getId())
                .map(this::toDeliveryConfigResponse)
                .orElse(null);

        return new SubscriptionResponse(
                subscription.getSubscriptionUid(),
                subscription.getSubscriptionTypeId(),
                subscription.getSubscriptionStatusId(),
                subscription.getName(),
                subscription.getDescription(),
                subscription.getActivatedAt(),
                subscription.getDeactivatedAt(),
                subscription.isActive(),
                subscription.getCreatedAt(),
                subscription.getCreatedBy(),
                subscription.getUpdatedAt(),
                subscription.getUpdatedBy(),
                triggerResponses,
                deliveryConfig
        );
    }

    private SubscriptionSummaryResponse toSummaryResponse(Subscription subscription) {
        return new SubscriptionSummaryResponse(
                subscription.getSubscriptionUid(),
                subscription.getSubscriptionTypeId(),
                subscription.getSubscriptionStatusId(),
                subscription.getName(),
                subscription.getDescription(),
                subscription.getActivatedAt(),
                subscription.getDeactivatedAt(),
                subscription.isActive()
        );
    }

    private DeliveryConfigResponse toDeliveryConfigResponse(DeliveryConfig deliveryConfig) {
        return new DeliveryConfigResponse(
                deliveryConfig.getDeliveryType(),
                deliveryConfig.getEndpointUrl(),
                deliveryConfig.getHttpMethod(),
                deliveryConfig.getTimeoutSeconds(),
                deliveryConfig.getMaxRetryCount(),
                deliveryConfig.getRetryBackoffSeconds()
        );
    }

    private String newSubscriptionUid() {
        return "sub_" + UUID.randomUUID().toString().replace("-", "");
    }

    private <T> List<T> distinctList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().distinct().toList();
    }

    public static String actorOrSystem(String actor) {
        if (actor == null || actor.isBlank()) {
            return SYSTEM_ACTOR;
        }
        return actor;
    }
}
