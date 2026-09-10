export type MasterData = {
  id: number;
  code: string;
  name: string;
  description: string;
};

export type ParameterDefinition = {
  id: number;
  dataTypeId: number;
  name: string;
  description: string;
  fieldPath: string;
  required: boolean;
};

export type TriggerSelection = {
  eventTriggerTypeId: number;
  parameterDefinitionIds: number[];
};

export type DeliveryConfig = {
  deliveryType: string;
  endpointUrl: string | null;
  httpMethod: string | null;
  timeoutSeconds: number;
  maxRetryCount: number;
  retryBackoffSeconds: number;
};

export type Subscription = {
  subscriptionUid: string;
  subscriptionTypeId: number;
  subscriptionStatusId: number;
  name: string;
  description: string | null;
  activatedAt: string | null;
  deactivatedAt: string | null;
  active: boolean;
  createdAt?: string;
  createdBy?: string;
  updatedAt?: string;
  updatedBy?: string;
  triggers: TriggerSelection[];
  deliveryConfig: DeliveryConfig | null;
};

type SubscriptionSummary = Omit<Subscription, "triggers" | "deliveryConfig">;

export type CreateSubscriptionInput = {
  subscriptionTypeId: number;
  subscriptionStatusId: number;
  name: string;
  description: string;
  activatedAt: string;
  deactivatedAt: string | null;
  triggers: TriggerSelection[];
  deliveryConfig: {
    deliveryType: string;
    endpointUrl?: string | null;
    httpMethod?: string | null;
    timeoutSeconds?: number;
    maxRetryCount?: number;
    retryBackoffSeconds?: number;
  };
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
      const body = (await response.json()) as { detail?: string; message?: string };
      message = body.detail || body.message || message;
    } catch {
      // Preserve the status-based message for non-JSON responses.
    }
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export async function listSubscriptions(): Promise<Subscription[]> {
  const summaries = await request<SubscriptionSummary[]>("/subscriptions");
  const settled = await Promise.allSettled(
    summaries.map((item) => getSubscription(item.subscriptionUid)),
  );

  return settled.map((result, index) =>
    result.status === "fulfilled"
      ? result.value
      : { ...summaries[index], triggers: [], deliveryConfig: null },
  );
}

export function getSubscription(subscriptionUid: string) {
  return request<Subscription>(`/subscriptions/${subscriptionUid}`);
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
  subscriptionUid: string,
  input: Record<string, unknown>,
  actor?: string,
) {
  return request<Subscription>(`/subscriptions/${subscriptionUid}`, {
    method: "PATCH",
    headers: actor ? { "X-User-Email": actor } : undefined,
    body: JSON.stringify(input),
  });
}
