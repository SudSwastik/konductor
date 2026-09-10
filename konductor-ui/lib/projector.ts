export type MasterData = {
  code: string;
  name: string;
  description: string;
};

export type ParameterDefinition = {
  code: string;
  dataType: string;
  name: string;
  description: string;
  fieldPath: string;
  required: boolean;
};

export type SubscriptionSummary = {
  subscriptionId: string;
  subscriptionVersion: number;
  subscriptionType: "EVENT" | "API_CALLBACK";
  status: string;
  basicInfo: {
    name: string;
    description: string | null;
    goLiveDate: string | null;
  };
};

export type Subscription = SubscriptionSummary & {
  parameters: Array<{
    code: string;
    name: string;
    description: string | null;
    fieldPath: string;
    required: boolean;
  }>;
  triggers: Array<{
    code: string;
    name: string;
    description: string | null;
  }>;
};

export type CreateSubscriptionInput = {
  subscriptionType: "EVENT" | "API_CALLBACK";
  basicInfo: {
    name: string;
    description: string;
    goLiveDate: string;
  };
  parameters: Array<{ code: string }>;
  triggers: Array<{ code: string }>;
};

const apiRoot = "/projector";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiRoot}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as {
        detail?: string;
        message?: string;
        error?: string;
        errors?: Array<{ defaultMessage?: string; field?: string }>;
      };
      const validationMessage = body.errors?.
        map((error) => [error.field, error.defaultMessage].filter(Boolean).join(": "))
        .filter(Boolean)
        .join(", ");
      message = body.detail || body.message || validationMessage || body.error || message;
    } catch {
      // Preserve the status-based message for non-JSON responses.
    }
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export function listSubscriptions() {
  return request<SubscriptionSummary[]>("/subscriptions");
}

export function getSubscription(subscriptionId: string) {
  return request<Subscription>(`/subscriptions/${subscriptionId}`);
}

export function listTriggers() {
  return request<MasterData[]>("/triggers");
}

export function listParameters() {
  return request<ParameterDefinition[]>("/parameters");
}

export function createSubscription(
  input: CreateSubscriptionInput,
  actor?: string,
) {
  return request<Subscription>("/subscriptions", {
    method: "POST",
    headers: actor ? { "X-User-Email": actor } : undefined,
    body: JSON.stringify(input),
  });
}

export function patchSubscription(
  subscriptionId: string,
  input: Record<string, unknown>,
  actor?: string,
) {
  return request<Subscription>(`/subscriptions/${subscriptionId}`, {
    method: "PATCH",
    headers: actor ? { "X-User-Email": actor } : undefined,
    body: JSON.stringify(input),
  });
}

export function replaceSubscriptionParameters(
  subscriptionId: string,
  parameters: Array<{ code: string }>,
  actor?: string,
) {
  return request<Subscription>(`/subscriptions/${subscriptionId}/parameters`, {
    method: "PUT",
    headers: actor ? { "X-User-Email": actor } : undefined,
    body: JSON.stringify({ parameters }),
  });
}

export function replaceSubscriptionTriggers(
  subscriptionId: string,
  triggers: Array<{ code: string }>,
  actor?: string,
) {
  return request<Subscription>(`/subscriptions/${subscriptionId}/triggers`, {
    method: "PUT",
    headers: actor ? { "X-User-Email": actor } : undefined,
    body: JSON.stringify({ triggers }),
  });
}

export function patchSubscriptionStatus(
  subscriptionId: string,
  status: "ACTIVE" | "PAUSED",
  actor?: string,
) {
  return request<Subscription>(`/subscriptions/${subscriptionId}/status`, {
    method: "PATCH",
    headers: actor ? { "X-User-Email": actor } : undefined,
    body: JSON.stringify({ status }),
  });
}
