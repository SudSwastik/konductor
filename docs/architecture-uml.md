# Konductor UML diagrams

These diagrams reflect the current repository structure. GitHub renders each Mermaid block directly in the document.

## System architecture

```mermaid
flowchart LR
    operator["Operator"] --> ui["Subscription UI"]
    ui -->|"auth"| cognito["Cognito"]
    ui -->|"REST"| projectorApi["Projector API"]
    source["Source client"] -->|"POST /api/v1/events"| producer["Producer API"]

    subgraph kafka["Kafka topics"]
        sourceTopic["source-events"]
        subscriptionTopic["subscription.{uid}"]
        ackTopic["consumer-acks"]
    end

    subgraph projector["Projector"]
        projection["Match + project"]
        persistence["Event + config persistence"]
    end

    consumer["Test consumer"]
    db[("PostgreSQL")]

    projectorApi --> persistence
    persistence --> db
    producer --> sourceTopic
    sourceTopic --> projection
    projection --> db
    projection --> subscriptionTopic
    subscriptionTopic --> consumer
    consumer -->|"ACKNOWLEDGED"| ackTopic
    ackTopic --> projection
```

## Event projection and acknowledgement sequence

```mermaid
sequenceDiagram
    actor Client as Source client
    participant Producer as Producer API
    participant SourceTopic as Source events topic
    participant Projector as Projector
    participant Database as PostgreSQL
    participant SubscriptionTopic as Subscription topic
    participant Consumer as Consumer
    participant AckTopic as Consumer acknowledgements

    Client->>Producer: Publish source event
    Producer->>SourceTopic: ProducerEventMessage
    Producer-->>Client: 202 Accepted
    SourceTopic->>Projector: Consume source event
    Projector->>Database: Find trigger and active subscriptions
    Database-->>Projector: Trigger, field, and delivery selections

    loop Each matching subscription
        Projector->>Database: Create event as DELIVERY_PENDING
        Projector->>Projector: Project selected JSON fields
        Projector->>SubscriptionTopic: ProjectedEventMessage
        Projector->>Database: Mark event DELIVERY_IN_PROGRESS
        SubscriptionTopic->>Consumer: Consume projected event
        Consumer->>AckTopic: ConsumerAckMessage
        AckTopic->>Projector: Consume acknowledgement
        Projector->>Database: Mark event DELIVERED for ACKNOWLEDGED
    end
```

## PostgreSQL tables

The physical schema is created by `projector/src/main/resources/db/migration/V1__create_tables.sql`. It contains 13 application tables in the `public` schema.

```mermaid
erDiagram
    subscription_type {
        smallint id PK
        string code UK
        string name
        string description
        boolean is_active
        datetime created_at
        string created_by
    }

    subscription_status {
        smallint id PK
        string code UK
        string name
        string description
        boolean is_active
        datetime created_at
        string created_by
    }

    event_trigger_type {
        smallint id PK
        string code UK
        string name
        string description
        boolean is_active
        datetime created_at
        string created_by
    }

    event_status {
        smallint id PK
        string code UK
        string name
        string description
        boolean is_active
        datetime created_at
        string created_by
    }

    parameter_data_type {
        smallint id PK
        string code UK
        string name
        string description
        boolean is_active
        datetime created_at
        string created_by
    }

    subscription {
        bigint id PK
        string subscription_uid UK
        smallint subscription_type_id FK
        smallint subscription_status_id FK
        string name
        string description
        datetime activated_at
        datetime deactivated_at
        boolean is_active
        datetime created_at
        string created_by
        datetime updated_at
        string updated_by
    }

    event_trigger_selection {
        bigint id PK
        bigint subscription_id FK
        smallint event_trigger_type_id FK
        boolean is_active
        datetime created_at
        string created_by
    }

    parameter_definition {
        bigint id PK
        smallint data_type_id FK
        string code UK
        string name
        string description
        string field_path
        boolean is_required
        boolean is_active
        datetime created_at
        string created_by
        datetime updated_at
        string updated_by
    }

    parameter_selection {
        bigint id PK
        bigint event_trigger_selection_id FK
        bigint parameter_definition_id FK
        boolean is_active
        datetime created_at
        string created_by
    }

    delivery_config {
        bigint id PK
        bigint subscription_id FK
        string delivery_type
        string endpoint_url
        string http_method
        string auth_type
        string auth_header_name
        string auth_secret_ref
        int timeout_seconds
        int max_retry_count
        int retry_backoff_seconds
        boolean is_active
        datetime created_at
        string created_by
        datetime updated_at
        string updated_by
    }

    event {
        bigint id PK
        string event_uid UK
        string source_event_id
        smallint event_trigger_type_id FK
        bigint subscription_id FK
        bigint event_trigger_selection_id FK
        bigint delivery_config_id FK
        smallint event_status_id FK
        int attempt_count
        datetime last_attempt_at
        datetime next_retry_at
        datetime delivered_at
        int response_status_code
        string error_message
        string payload_hash
        bigint payload_size_bytes
        boolean is_active
        datetime created_at
        string created_by
        datetime updated_at
        string updated_by
    }

    event_retry_log {
        bigint id PK
        bigint event_id FK
        int attempt_number
        string status
        datetime requested_at
        datetime completed_at
        int response_status_code
        string response_body
        string error_message
        bigint duration_ms
        boolean is_active
        datetime created_at
        string created_by
    }

    subscription_audit_log {
        bigint id PK
        bigint subscription_id FK
        string action
        string entity_type
        bigint entity_id
        json old_value
        json new_value
        datetime created_at
        string created_by
    }

    subscription_type ||--o{ subscription : categorizes
    subscription_status ||--o{ subscription : tracks
    subscription ||--o{ event_trigger_selection : selects
    event_trigger_type ||--o{ event_trigger_selection : identifies
    parameter_data_type ||--o{ parameter_definition : types
    event_trigger_selection ||--o{ parameter_selection : contains
    parameter_definition ||--o{ parameter_selection : selects
    subscription ||--o{ delivery_config : configures
    event_trigger_type ||--o{ event : triggers
    subscription ||--o{ event : receives
    event_trigger_selection ||--o{ event : matches
    delivery_config o|--o{ event : configures
    event_status ||--o{ event : tracks
    event ||--o{ event_retry_log : retries
    subscription o|--o{ subscription_audit_log : audits
```

## Projector domain model

The Java entities store foreign-key identifiers rather than JPA object references. The associations below represent the database relationships those identifiers create.

```mermaid
classDiagram
    class AuditFields {
        <<abstract>>
        +Instant createdAt
        +String createdBy
        +markCreated(user)
    }

    class MutableAuditFields {
        <<abstract>>
        +boolean active
        +Instant updatedAt
        +String updatedBy
        +markUpdated(user)
    }

    class Subscription {
        +Long id
        +String subscriptionUid
        +Short subscriptionTypeId
        +Short subscriptionStatusId
        +String name
        +String description
        +Instant activatedAt
        +Instant deactivatedAt
    }

    class SubscriptionType {
        +Short id
        +String code
        +String name
        +boolean active
    }

    class SubscriptionStatus {
        +Short id
        +String code
        +String name
        +boolean active
    }

    class DeliveryConfig {
        +Long id
        +Long subscriptionId
        +String deliveryType
        +String endpointUrl
        +String httpMethod
        +String authType
        +int timeoutSeconds
        +int maxRetryCount
        +int retryBackoffSeconds
    }

    class EventTriggerType {
        +Short id
        +String code
        +String name
        +boolean active
    }

    class EventTriggerSelection {
        +Long id
        +Long subscriptionId
        +Short eventTriggerTypeId
        +boolean active
    }

    class ParameterDataType {
        +Short id
        +String code
        +String name
        +boolean active
    }

    class ParameterDefinition {
        +Long id
        +Short dataTypeId
        +String code
        +String name
        +String fieldPath
        +boolean required
    }

    class ParameterSelection {
        +Long id
        +Long eventTriggerSelectionId
        +Long parameterDefinitionId
        +boolean active
    }

    class EventStatus {
        +Short id
        +String code
        +String name
        +boolean active
    }

    class Event {
        +Long id
        +String eventUid
        +String sourceEventId
        +Short eventTriggerTypeId
        +Long subscriptionId
        +Long eventTriggerSelectionId
        +Long deliveryConfigId
        +Short eventStatusId
        +int attemptCount
        +Instant lastAttemptAt
        +Instant nextRetryAt
        +Instant deliveredAt
        +String payloadHash
        +Long payloadSizeBytes
    }

    AuditFields <|-- MutableAuditFields
    MutableAuditFields <|-- Subscription
    MutableAuditFields <|-- DeliveryConfig
    MutableAuditFields <|-- ParameterDefinition
    MutableAuditFields <|-- Event
    AuditFields <|-- SubscriptionType
    AuditFields <|-- SubscriptionStatus
    AuditFields <|-- EventTriggerType
    AuditFields <|-- EventTriggerSelection
    AuditFields <|-- ParameterDataType
    AuditFields <|-- ParameterSelection
    AuditFields <|-- EventStatus

    SubscriptionType "1" <-- "0..*" Subscription : type
    SubscriptionStatus "1" <-- "0..*" Subscription : status
    Subscription "1" *-- "0..*" DeliveryConfig : delivery history
    Subscription "1" *-- "0..*" EventTriggerSelection : selects
    EventTriggerType "1" <-- "0..*" EventTriggerSelection : trigger
    EventTriggerSelection "1" *-- "0..*" ParameterSelection : projects
    ParameterDefinition "1" <-- "0..*" ParameterSelection : field
    ParameterDataType "1" <-- "0..*" ParameterDefinition : data type
    Subscription "1" *-- "0..*" Event : receives
    EventTriggerType "1" <-- "0..*" Event : caused by
    EventTriggerSelection "1" <-- "0..*" Event : matched selection
    DeliveryConfig "0..1" <-- "0..*" Event : delivery settings
    EventStatus "1" <-- "0..*" Event : status
```
