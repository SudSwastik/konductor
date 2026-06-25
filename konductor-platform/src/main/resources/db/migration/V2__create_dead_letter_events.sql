CREATE TABLE dead_letter_events (
    id             UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       UUID,
    event_type     VARCHAR,
    original_event JSONB,
    subscriber_id  VARCHAR,
    failure_reason TEXT,
    retry_count    INT     NOT NULL DEFAULT 0,
    last_retry_at  TIMESTAMP WITH TIME ZONE,
    status         VARCHAR NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP WITH TIME ZONE
);
