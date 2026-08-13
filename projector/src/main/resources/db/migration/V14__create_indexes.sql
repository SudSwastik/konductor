CREATE INDEX idx_subscription_status_id ON subscription(subscription_status_id);
CREATE INDEX idx_subscription_type_id ON subscription(subscription_type_id);
CREATE INDEX idx_event_trigger_selection_subscription_id ON event_trigger_selection(subscription_id);
CREATE INDEX idx_event_trigger_selection_event_trigger_type_id ON event_trigger_selection(event_trigger_type_id);
CREATE INDEX idx_parameter_definition_data_type_id ON parameter_definition(data_type_id);
CREATE INDEX idx_parameter_selection_event_trigger_selection_id ON parameter_selection(event_trigger_selection_id);
CREATE INDEX idx_parameter_selection_parameter_definition_id ON parameter_selection(parameter_definition_id);
CREATE INDEX idx_delivery_config_subscription_id ON delivery_config(subscription_id);
CREATE INDEX idx_event_source_event_id ON event(source_event_id);
CREATE INDEX idx_event_subscription_id ON event(subscription_id);
CREATE INDEX idx_event_event_status_id ON event(event_status_id);
CREATE INDEX idx_event_next_retry_at ON event(next_retry_at);
CREATE INDEX idx_event_retry_log_event_id ON event_retry_log(event_id);
CREATE INDEX idx_subscription_audit_log_subscription_id ON subscription_audit_log(subscription_id);
CREATE INDEX idx_subscription_audit_log_created_at ON subscription_audit_log(created_at);

CREATE UNIQUE INDEX uk_event_trigger_selection_active
    ON event_trigger_selection(subscription_id, event_trigger_type_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_parameter_definition_field_path_active
    ON parameter_definition(field_path)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_parameter_selection_active
    ON parameter_selection(event_trigger_selection_id, parameter_definition_id)
    WHERE deleted_at IS NULL;
