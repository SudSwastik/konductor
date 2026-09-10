package com.konductor.projector.service;

import com.konductor.projector.dto.ReplaceParametersRequest;
import com.konductor.projector.dto.ReplaceTriggersRequest;
import com.konductor.projector.dto.SubscriptionBasicInfoResponse;
import com.konductor.projector.dto.SubscriptionCreateRequest;
import com.konductor.projector.dto.SubscriptionPatchRequest;
import com.konductor.projector.dto.SubscriptionParameterRequest;
import com.konductor.projector.dto.SubscriptionParameterResponse;
import com.konductor.projector.dto.SubscriptionResponse;
import com.konductor.projector.dto.SubscriptionStatusPatchRequest;
import com.konductor.projector.dto.SubscriptionSummaryResponse;
import com.konductor.projector.dto.SubscriptionTriggerRequest;
import com.konductor.projector.dto.SubscriptionTriggerResponse;
import com.konductor.projector.entity.EventTriggerType;
import com.konductor.projector.entity.EventTriggerSelection;
import com.konductor.projector.entity.ParameterDefinition;
import com.konductor.projector.entity.ParameterSelection;
import com.konductor.projector.entity.Subscription;
import com.konductor.projector.entity.SubscriptionStatus;
import com.konductor.projector.entity.SubscriptionType;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
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
    private final SubscriptionLifecyclePolicy lifecyclePolicy;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionTypeRepository subscriptionTypeRepository,
            SubscriptionStatusRepository subscriptionStatusRepository,
            EventTriggerTypeRepository eventTriggerTypeRepository,
            EventTriggerSelectionRepository eventTriggerSelectionRepository,
            ParameterDefinitionRepository parameterDefinitionRepository,
            ParameterSelectionRepository parameterSelectionRepository,
            SubscriptionLifecyclePolicy lifecyclePolicy
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionTypeRepository = subscriptionTypeRepository;
        this.subscriptionStatusRepository = subscriptionStatusRepository;
        this.eventTriggerTypeRepository = eventTriggerTypeRepository;
        this.eventTriggerSelectionRepository = eventTriggerSelectionRepository;
        this.parameterDefinitionRepository = parameterDefinitionRepository;
        this.parameterSelectionRepository = parameterSelectionRepository;
        this.lifecyclePolicy = lifecyclePolicy;
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionCreateRequest request, String actor) {
        actor = actorOrSystem(actor);
        SubscriptionType subscriptionType = findSubscriptionType(request.subscriptionType());
        SubscriptionStatus initialStatus = findSubscriptionStatus(
                lifecyclePolicy.initialStatus(request.basicInfo().goLiveDate())
        );

        Subscription subscription = new Subscription();
        subscription.setSubscriptionUid(newSubscriptionUid());
        subscription.setSubscriptionTypeId(subscriptionType.getId());
        subscription.setSubscriptionStatusId(initialStatus.getId());
        subscription.setName(request.basicInfo().name());
        subscription.setDescription(request.basicInfo().description());
        subscription.setActivatedAt(request.basicInfo().goLiveDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        subscription.markCreated(actor);
        subscription = subscriptionRepository.save(subscription);

        replaceSelections(
                subscription,
                resolveTriggerIds(request.triggers()),
                resolveParameterIds(request.parameters()),
                actor
        );

        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionSummaryResponse> list() {
        return subscriptionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse get(String subscriptionUid) {
        return toResponse(findSubscription(subscriptionUid));
    }

    @Transactional
    public SubscriptionResponse patchBasic(String subscriptionUid, SubscriptionPatchRequest request, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findSubscription(subscriptionUid);

        if (request.basicInfo().name() != null) {
            subscription.setName(request.basicInfo().name());
        }
        if (request.basicInfo().description() != null) {
            subscription.setDescription(request.basicInfo().description());
        }
        if (request.basicInfo().goLiveDate() != null) {
            subscription.setActivatedAt(request.basicInfo().goLiveDate().atStartOfDay().toInstant(ZoneOffset.UTC));
        }

        subscription.markUpdated(actor);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse patchStatus(String subscriptionUid, SubscriptionStatusPatchRequest request, String actor) {
        return transitionLifecycle(subscriptionUid, request.status(), actor);
    }

    @Transactional
    public SubscriptionResponse transitionLifecycle(
            String subscriptionUid,
            String requestedStatus,
            String actor
    ) {
        actor = actorOrSystem(actor);
        Subscription subscription = findSubscription(subscriptionUid);
        String targetStatus = normalizeCode(requestedStatus);
        String currentStatus = subscriptionStatusCode(subscription.getSubscriptionStatusId());
        lifecyclePolicy.validateTransition(currentStatus, targetStatus);
        subscription.setSubscriptionStatusId(findSubscriptionStatus(targetStatus).getId());
        if (SubscriptionLifecyclePolicy.INACTIVE.equals(targetStatus)
                || SubscriptionLifecyclePolicy.ARCHIVED.equals(targetStatus)) {
            subscription.setDeactivatedAt(Instant.now());
        } else if (SubscriptionLifecyclePolicy.ACTIVE.equals(targetStatus)) {
            subscription.setDeactivatedAt(null);
        }
        subscription.markUpdated(actor);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse replaceTriggers(String subscriptionUid, ReplaceTriggersRequest request, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findSubscription(subscriptionUid);
        List<Long> activeSelectionIds = eventTriggerSelectionRepository
                .findBySubscriptionIdAndActiveTrue(subscription.getId()).stream()
                .map(EventTriggerSelection::getId)
                .toList();
        List<Long> parameterIds = activeSelectionIds.isEmpty()
                ? List.of()
                : parameterSelectionRepository.findByEventTriggerSelectionIdInAndActiveTrue(activeSelectionIds).stream()
                        .map(ParameterSelection::getParameterDefinitionId)
                        .distinct()
                        .toList();
        return replaceSelections(subscription, resolveTriggerIds(request.triggers()), parameterIds, actor);
    }

    @Transactional
    public SubscriptionResponse replaceParameters(String subscriptionUid, ReplaceParametersRequest request, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findSubscription(subscriptionUid);
        List<Short> triggerTypeIds = eventTriggerSelectionRepository
                .findBySubscriptionIdAndActiveTrue(subscription.getId()).stream()
                .map(EventTriggerSelection::getEventTriggerTypeId)
                .distinct()
                .toList();
        return replaceSelections(subscription, triggerTypeIds, resolveParameterIds(request.parameters()), actor);
    }

    private SubscriptionResponse replaceSelections(
            Subscription subscription,
            List<Short> triggerTypeIds,
            List<Long> parameterDefinitionIds,
            String actor
    ) {
        List<EventTriggerSelection> existingSelections = eventTriggerSelectionRepository.findBySubscriptionId(subscription.getId());
        existingSelections.forEach(selection -> selection.setActive(false));

        for (Short triggerTypeId : triggerTypeIds) {
            EventTriggerSelection selection = firstOrNewSelection(
                    existingSelections,
                    subscription.getId(),
                    triggerTypeId,
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

            for (Short triggerTypeId : triggerTypeIds) {
                EventTriggerSelection triggerSelection = firstOrNewSelection(
                        existingSelections,
                        subscription.getId(),
                        triggerTypeId,
                        actor
                );

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

    @Transactional
    public void softDelete(String subscriptionUid, String actor) {
        actor = actorOrSystem(actor);
        Subscription subscription = findSubscription(subscriptionUid);
        String currentStatus = subscriptionStatusCode(subscription.getSubscriptionStatusId());
        lifecyclePolicy.validateTransition(currentStatus, SubscriptionLifecyclePolicy.ARCHIVED);
        subscription.setSubscriptionStatusId(findSubscriptionStatus(SubscriptionLifecyclePolicy.ARCHIVED).getId());
        subscription.setDeactivatedAt(Instant.now());
        subscription.markUpdated(actor);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public int activateDueScheduledSubscriptions() {
        SubscriptionStatus scheduled = subscriptionStatusRepository
                .findByCodeAndActiveTrue(SubscriptionLifecyclePolicy.SCHEDULED)
                .orElse(null);
        if (scheduled == null) {
            return 0;
        }
        List<Subscription> dueSubscriptions = subscriptionRepository
                .findBySubscriptionStatusIdAndActivatedAtLessThanEqualAndActiveTrue(scheduled.getId(), Instant.now());
        if (dueSubscriptions.isEmpty()) {
            return 0;
        }

        SubscriptionStatus active = findSubscriptionStatus(SubscriptionLifecyclePolicy.ACTIVE);
        dueSubscriptions.forEach(subscription -> {
            subscription.setSubscriptionStatusId(active.getId());
            subscription.setDeactivatedAt(null);
            subscription.markUpdated(SYSTEM_ACTOR);
        });
        subscriptionRepository.saveAll(dueSubscriptions);
        return dueSubscriptions.size();
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

    private Subscription findSubscription(String subscriptionUid) {
        return subscriptionRepository.findBySubscriptionUid(subscriptionUid)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Subscription not found"));
    }

    private SubscriptionType findSubscriptionType(String code) {
        return subscriptionTypeRepository.findByCodeAndActiveTrue(normalizeCode(code))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid subscriptionType"));
    }

    private SubscriptionStatus findSubscriptionStatus(String code) {
        return subscriptionStatusRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Subscription status is not configured"));
    }

    private List<Long> resolveParameterIds(List<SubscriptionParameterRequest> parameters) {
        List<String> codes = parameters.stream()
                .map(SubscriptionParameterRequest::code)
                .map(this::normalizeCode)
                .distinct()
                .toList();
        List<ParameterDefinition> definitions = parameterDefinitionRepository.findByCodeInAndActiveTrue(codes);
        if (definitions.size() != codes.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid parameter code");
        }
        return definitions.stream().map(ParameterDefinition::getId).toList();
    }

    private List<Short> resolveTriggerIds(List<SubscriptionTriggerRequest> triggers) {
        List<String> codes = triggers.stream()
                .map(SubscriptionTriggerRequest::code)
                .map(this::normalizeCode)
                .distinct()
                .toList();
        List<EventTriggerType> triggerTypes = eventTriggerTypeRepository.findByCodeInAndActiveTrue(codes);
        if (triggerTypes.size() != codes.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid trigger code");
        }
        return triggerTypes.stream().map(EventTriggerType::getId).toList();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        List<EventTriggerSelection> triggerSelections =
                eventTriggerSelectionRepository.findBySubscriptionIdAndActiveTrue(subscription.getId());
        List<Short> triggerTypeIds = triggerSelections.stream()
                .map(EventTriggerSelection::getEventTriggerTypeId)
                .distinct()
                .toList();
        List<Long> triggerSelectionIds = triggerSelections.stream().map(EventTriggerSelection::getId).toList();
        List<ParameterSelection> parameterSelections = triggerSelectionIds.isEmpty()
                ? List.of()
                : parameterSelectionRepository.findByEventTriggerSelectionIdInAndActiveTrue(triggerSelectionIds);
        List<Long> parameterDefinitionIds = parameterSelections.stream()
                .map(ParameterSelection::getParameterDefinitionId)
                .distinct()
                .toList();
        List<SubscriptionParameterResponse> parameters = parameterDefinitionIds.isEmpty()
                ? List.of()
                : parameterDefinitionRepository.findByIdInAndActiveTrue(parameterDefinitionIds).stream()
                        .map(value -> new SubscriptionParameterResponse(
                                value.getCode(),
                                value.getName(),
                                value.getDescription(),
                                value.getFieldPath(),
                                value.isRequired()
                        ))
                        .toList();
        List<SubscriptionTriggerResponse> triggers = triggerTypeIds.isEmpty()
                ? List.of()
                : eventTriggerTypeRepository.findByIdInAndActiveTrue(triggerTypeIds).stream()
                        .map(value -> new SubscriptionTriggerResponse(
                                value.getCode(),
                                value.getName(),
                                value.getDescription()
                        ))
                        .toList();

        return new SubscriptionResponse(
                subscription.getSubscriptionUid(),
                subscription.getSubscriptionVersion(),
                subscriptionTypeCode(subscription.getSubscriptionTypeId()),
                subscriptionStatusCode(subscription.getSubscriptionStatusId()),
                basicInfo(subscription),
                parameters,
                triggers
        );
    }

    private SubscriptionSummaryResponse toSummaryResponse(Subscription subscription) {
        return new SubscriptionSummaryResponse(
                subscription.getSubscriptionUid(),
                subscription.getSubscriptionVersion(),
                subscriptionTypeCode(subscription.getSubscriptionTypeId()),
                subscriptionStatusCode(subscription.getSubscriptionStatusId()),
                basicInfo(subscription)
        );
    }

    private SubscriptionBasicInfoResponse basicInfo(Subscription subscription) {
        LocalDate goLiveDate = subscription.getActivatedAt() == null
                ? null
                : LocalDate.ofInstant(subscription.getActivatedAt(), ZoneOffset.UTC);
        return new SubscriptionBasicInfoResponse(
                subscription.getName(),
                subscription.getDescription(),
                goLiveDate
        );
    }

    private String subscriptionTypeCode(Short id) {
        return subscriptionTypeRepository.findById(id)
                .map(SubscriptionType::getCode)
                .orElseThrow(() -> new IllegalStateException("Subscription type is not configured"));
    }

    private String subscriptionStatusCode(Short id) {
        return subscriptionStatusRepository.findById(id)
                .map(SubscriptionStatus::getCode)
                .orElseThrow(() -> new IllegalStateException("Subscription status is not configured"));
    }

    private String newSubscriptionUid() {
        return "sub_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String actorOrSystem(String actor) {
        if (actor == null || actor.isBlank()) {
            return SYSTEM_ACTOR;
        }
        return actor;
    }
}
