# Subscription lifecycle policy

This document is the canonical definition of subscription status, transition rules, and go-live behavior. Any API, UI, scheduler, or operational implementation must follow this policy.

## Statuses

| Code | Meaning | Delivery behavior |
| --- | --- | --- |
| `SCHEDULED` | Approved for a future go-live date and waiting for activation | Does not deliver events before the go-live date |
| `ACTIVE` | Enabled and delivering matching events | Delivers events |
| `INACTIVE` | Intentionally disabled but retained for review or later activation | Does not deliver events |
| `ARCHIVED` | Permanently retired from normal operation | Does not deliver events |

Status is derived and controlled by the subscription service. Clients must request a transition; they must not write status or audit fields directly.

## Creation and go-live

- A new subscription starts in `SCHEDULED` when its go-live date is in the future.
- A new subscription starts in `ACTIVE` when its go-live date is today or earlier, unless the caller explicitly requests another permitted initial state.
- A scheduled subscription automatically changes to `ACTIVE` at the start of its go-live date in UTC.
- A scheduled subscription may be scheduled again: changing its go-live date keeps it `SCHEDULED` and replaces the pending activation time.
- The scheduled-to-active job must be idempotent and must update the normal audit fields.
- A subscription that is `INACTIVE` or `ARCHIVED` is never automatically reactivated by the go-live scheduler.
- Changing the go-live date on an `ACTIVE` subscription does not silently deactivate it. An explicit transition to `SCHEDULED` is required when the owner wants to wait for the new date.

## Allowed transitions

| Current | Allowed next status | How |
| --- | --- | --- |
| `SCHEDULED` | `SCHEDULED` | Manual rescheduling; remains scheduled for the new go-live date |
| `SCHEDULED` | `ACTIVE` | Manual activation or automatic activation on/after go-live date |
| `SCHEDULED` | `INACTIVE` | Manual deactivation |
| `SCHEDULED` | `ARCHIVED` | Archive from any state |
| `ACTIVE` | `INACTIVE` | Manual deactivation |
| `ACTIVE` | `SCHEDULED` | Manual rescheduling; activates automatically on/after go-live date |
| `ACTIVE` | `ARCHIVED` | Archive from any state |
| `INACTIVE` | `ACTIVE` | Manual activation immediately |
| `INACTIVE` | `SCHEDULED` | Manual rescheduling; activates automatically on/after go-live date |
| `INACTIVE` | `ARCHIVED` | Archive from any state |
| `ARCHIVED` | `INACTIVE` | Unarchive only; requires review before reactivation |

No other transitions are valid. In particular:

- `ARCHIVED` cannot transition directly to `ACTIVE` or `SCHEDULED`.
- Unarchive always results in `INACTIVE`, never an automatically active subscription.
- A transition to `ARCHIVED` is terminal until an explicit unarchive action is approved.

## Operational invariants

- Only `ACTIVE` subscriptions participate in event delivery.
- `SCHEDULED`, `INACTIVE`, and `ARCHIVED` subscriptions remain queryable for audit and review.
- Every transition records the actor, timestamp, previous status, and new status.
- Repeating a lifecycle transition is rejected as a no-op or handled idempotently; rescheduling a `SCHEDULED` subscription is allowed when its go-live date changes.
- Status transition failures must leave the subscription in its previous status.
- The API should expose the status code from the status master data and reject unknown status codes.

## UI expectations

- Show all four statuses with distinct labels and visual treatments.
- Offer only transitions allowed from the current status.
- Show the go-live date for `SCHEDULED` subscriptions and explain that activation is automatic on that date.
- Require an explicit confirmation for archive and unarchive actions.
- After unarchive, show the subscription as `INACTIVE` and direct the user to review it before activation or rescheduling.
