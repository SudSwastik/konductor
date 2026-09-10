INSERT INTO subscription_type (code, name, description)
VALUES
    ('EVENT', 'Event', 'Deliver projected events to a Kafka topic.'),
    ('API_CALLBACK', 'API callback', 'Deliver projected events through a configured API callback.');

INSERT INTO subscription_status (code, name, description)
VALUES
    ('SCHEDULED', 'Scheduled', 'Subscription is waiting for its go-live date.'),
    ('ACTIVE', 'Active', 'Subscription is approved and receiving projected events.'),
    ('INACTIVE', 'Inactive', 'Subscription is retained but not delivering events.'),
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

INSERT INTO parameter_definition (code, data_type_id, name, description, field_path, is_required, created_by, updated_by)
VALUES
    ('EVENT_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Event ID', 'Unique source event identifier.', '$.event.eventId', TRUE, 'SYSTEM', 'SYSTEM'),
    ('EVENT_TRIGGER_TYPE', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Event Trigger Type', 'Type of source event trigger.', '$.event.triggerType', TRUE, 'SYSTEM', 'SYSTEM'),
    ('EVENT_OCCURRED_AT', (SELECT id FROM parameter_data_type WHERE code = 'DATETIME'), 'Event Occurred At', 'Timestamp when the event happened.', '$.event.occurredAt', TRUE, 'SYSTEM', 'SYSTEM'),
    ('EVENT_SOURCE_SYSTEM', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Event Source System', 'Name of the upstream system that produced the event.', '$.event.sourceSystem', TRUE, 'SYSTEM', 'SYSTEM'),
    ('CORRELATION_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Correlation ID', 'Correlation identifier for tracing across systems.', '$.event.correlationId', FALSE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Order ID', 'Internal order identifier.', '$.order.orderId', TRUE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_NUMBER', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Order Number', 'Human-readable order number.', '$.order.orderNumber', TRUE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_STATUS', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Order Status', 'Current order status.', '$.order.status', TRUE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_CREATED_AT', (SELECT id FROM parameter_data_type WHERE code = 'DATETIME'), 'Order Created At', 'Timestamp when the order was created.', '$.order.createdAt', TRUE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_UPDATED_AT', (SELECT id FROM parameter_data_type WHERE code = 'DATETIME'), 'Order Updated At', 'Timestamp when the order was last updated.', '$.order.updatedAt', FALSE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_TOTAL_AMOUNT', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'Order Total Amount', 'Total order amount.', '$.order.total.amount', TRUE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_CURRENCY', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Order Currency', 'Currency code for order amounts.', '$.order.total.currency', TRUE, 'SYSTEM', 'SYSTEM'),
    ('SUBTOTAL_AMOUNT', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'Subtotal Amount', 'Order subtotal before discounts and tax.', '$.order.subtotal.amount', FALSE, 'SYSTEM', 'SYSTEM'),
    ('TAX_AMOUNT', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'Tax Amount', 'Total tax amount.', '$.order.tax.amount', FALSE, 'SYSTEM', 'SYSTEM'),
    ('DISCOUNT_AMOUNT', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'Discount Amount', 'Total discount amount.', '$.order.discount.amount', FALSE, 'SYSTEM', 'SYSTEM'),
    ('SHIPPING_AMOUNT', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'Shipping Amount', 'Shipping charge amount.', '$.order.shipping.amount', FALSE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Customer ID', 'Internal customer identifier.', '$.customer.customerId', TRUE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_EXTERNAL_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Customer External ID', 'External customer identifier.', '$.customer.externalId', FALSE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_FIRST_NAME', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Customer First Name', 'Customer first name.', '$.customer.firstName', FALSE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_LAST_NAME', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Customer Last Name', 'Customer last name.', '$.customer.lastName', FALSE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_EMAIL', (SELECT id FROM parameter_data_type WHERE code = 'EMAIL'), 'Customer Email', 'Customer email address.', '$.customer.email', TRUE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_PHONE', (SELECT id FROM parameter_data_type WHERE code = 'PHONE'), 'Customer Phone', 'Customer phone number.', '$.customer.phone', FALSE, 'SYSTEM', 'SYSTEM'),
    ('CUSTOMER_MARKETING_OPT_IN', (SELECT id FROM parameter_data_type WHERE code = 'BOOLEAN'), 'Customer Marketing Opt In', 'Whether the customer opted into marketing.', '$.customer.marketingOptIn', FALSE, 'SYSTEM', 'SYSTEM'),
    ('ORDER_ITEMS', (SELECT id FROM parameter_data_type WHERE code = 'ARRAY'), 'Order Items', 'Array of order line items.', '$.items', TRUE, 'SYSTEM', 'SYSTEM'),
    ('ITEM_COUNT', (SELECT id FROM parameter_data_type WHERE code = 'INTEGER'), 'Item Count', 'Total number of order line items.', '$.items.length()', FALSE, 'SYSTEM', 'SYSTEM'),
    ('FIRST_ITEM_SKU', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'First Item SKU', 'SKU of the first order item.', '$.items[0].sku', FALSE, 'SYSTEM', 'SYSTEM'),
    ('FIRST_ITEM_NAME', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'First Item Name', 'Name of the first order item.', '$.items[0].name', FALSE, 'SYSTEM', 'SYSTEM'),
    ('FIRST_ITEM_QUANTITY', (SELECT id FROM parameter_data_type WHERE code = 'INTEGER'), 'First Item Quantity', 'Quantity of the first order item.', '$.items[0].quantity', FALSE, 'SYSTEM', 'SYSTEM'),
    ('FIRST_ITEM_UNIT_PRICE', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'First Item Unit Price', 'Unit price of the first order item.', '$.items[0].unitPrice.amount', FALSE, 'SYSTEM', 'SYSTEM'),
    ('PAYMENT_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Payment ID', 'Payment transaction identifier.', '$.payment.paymentId', FALSE, 'SYSTEM', 'SYSTEM'),
    ('PAYMENT_METHOD', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Payment Method', 'Payment method used for the order.', '$.payment.method', FALSE, 'SYSTEM', 'SYSTEM'),
    ('PAYMENT_STATUS', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Payment Status', 'Current payment status.', '$.payment.status', FALSE, 'SYSTEM', 'SYSTEM'),
    ('PAYMENT_AUTHORIZED_AMOUNT', (SELECT id FROM parameter_data_type WHERE code = 'DECIMAL'), 'Payment Authorized Amount', 'Authorized payment amount.', '$.payment.authorizedAmount.amount', FALSE, 'SYSTEM', 'SYSTEM'),
    ('BILLING_COUNTRY', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Billing Country', 'Billing address country code.', '$.billingAddress.country', FALSE, 'SYSTEM', 'SYSTEM'),
    ('BILLING_POSTAL_CODE', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Billing Postal Code', 'Billing address postal code.', '$.billingAddress.postalCode', FALSE, 'SYSTEM', 'SYSTEM'),
    ('SHIPPING_COUNTRY', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Shipping Country', 'Shipping address country code.', '$.shippingAddress.country', FALSE, 'SYSTEM', 'SYSTEM'),
    ('SHIPPING_POSTAL_CODE', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Shipping Postal Code', 'Shipping address postal code.', '$.shippingAddress.postalCode', FALSE, 'SYSTEM', 'SYSTEM'),
    ('SHIPPING_CARRIER', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Shipping Carrier', 'Carrier selected for shipping.', '$.shipment.carrier', FALSE, 'SYSTEM', 'SYSTEM'),
    ('TRACKING_NUMBER', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Tracking Number', 'Shipment tracking number.', '$.shipment.trackingNumber', FALSE, 'SYSTEM', 'SYSTEM'),
    ('ESTIMATED_DELIVERY_AT', (SELECT id FROM parameter_data_type WHERE code = 'DATETIME'), 'Estimated Delivery At', 'Estimated delivery timestamp.', '$.shipment.estimatedDeliveryAt', FALSE, 'SYSTEM', 'SYSTEM'),
    ('SALES_CHANNEL', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Sales Channel', 'Sales channel where the order was placed.', '$.channel.name', FALSE, 'SYSTEM', 'SYSTEM'),
    ('STORE_ID', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Store ID', 'Store or tenant identifier.', '$.channel.storeId', FALSE, 'SYSTEM', 'SYSTEM'),
    ('PROMO_CODE', (SELECT id FROM parameter_data_type WHERE code = 'STRING'), 'Promo Code', 'Promotion code applied to the order.', '$.promotions[0].code', FALSE, 'SYSTEM', 'SYSTEM'),
    ('METADATA', (SELECT id FROM parameter_data_type WHERE code = 'OBJECT'), 'Metadata', 'Additional event metadata.', '$.metadata', FALSE, 'SYSTEM', 'SYSTEM');
