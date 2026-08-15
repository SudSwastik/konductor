INSERT INTO subscription_type (code, name, description)
VALUES
    ('EVENT', 'Event', 'Deliver projected events to a Kafka topic.');

INSERT INTO subscription_status (code, name, description)
VALUES
    ('ACTIVE', 'Active', 'Subscription is approved and receiving projected events.'),
    ('PAUSED', 'Paused', 'Subscription is temporarily stopped.'),
    ('DRAFT', 'Draft', 'Subscription has been not been  disabled.'),
    ('ARCHIVED', 'Archived', 'Subscription has been archived.');

INSERT INTO event_trigger_type (code, name, description)
VALUES
    ('ORDER_CREATED', 'Order Created', 'Triggered when a new order is created.'),
    ('ORDER_UPDATED', 'Order Updated', 'Triggered when order details change.'),
    ('ORDER_CANCELLED', 'Order Cancelled', 'Triggered when an order is cancelled.'),
    ('PAYMENT_UPDATED', 'Payment Updated', 'Triggered when order payment details change.'),
    ('SHIPMENT_UPDATED', 'Shipment Updated', 'Triggered when shipment details change.'),
    ('CUSTOMER_UPDATED', 'Customer Updated', 'Triggered when customer details linked to the order change.');

INSERT INTO event_status (code, name, description)
VALUES
    ('CREATED', 'Created', 'Projected event row has been created.'),
    ('PROJECTED', 'Projected', 'Projected payload has been created.'),
    ('DELIVERY_PENDING', 'Delivery Pending', 'Projected event is ready for downstream delivery.'),
    ('DELIVERY_IN_PROGRESS', 'Delivery In Progress', 'Projected event is currently being delivered.'),
    ('DELIVERED', 'Delivered', 'Projected event was delivered successfully.'),
    ('DELIVERY_FAILED', 'Delivery Failed', 'Latest delivery attempt failed.'),
    ('RETRY_SCHEDULED', 'Retry Scheduled', 'Delivery failed and is scheduled for retry.'),
    ('RETRY_EXHAUSTED', 'Retry Exhausted', 'Delivery failed permanently after retry limit.'),
    ('CANCELLED', 'Cancelled', 'Projected event delivery was cancelled.');

INSERT INTO parameter_data_type (code, name, description)
VALUES
    ('STRING', 'String', 'Text value.'),
    ('NUMBER', 'Number', 'Generic numeric value.'),
    ('INTEGER', 'Integer', 'Whole number value.'),
    ('DECIMAL', 'Decimal', 'Decimal value for money or precise quantities.'),
    ('BOOLEAN', 'Boolean', 'True or false value.'),
    ('DATE', 'Date', 'Calendar date without time.'),
    ('DATETIME', 'Date Time', 'Timestamp value.'),
    ('OBJECT', 'Object', 'Nested JSON object.'),
    ('ARRAY', 'Array', 'JSON array value.'),
    ('UUID', 'UUID', 'Universally unique identifier.'),
    ('EMAIL', 'Email', 'Email address.'),
    ('PHONE', 'Phone', 'Phone number.'),
    ('URL', 'URL', 'Web URL.');
