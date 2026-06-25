CREATE TABLE subscriptions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscriber_id        VARCHAR NOT NULL,
    subscriber_name      VARCHAR NOT NULL,
    subscription_version INT     NOT NULL DEFAULT 1,
    event_type           VARCHAR NOT NULL,
    field_paths          JSONB   NOT NULL,
    output_topic         VARCHAR NOT NULL,
    active               BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMP WITH TIME ZONE,
    approved_at          TIMESTAMP WITH TIME ZONE
);
