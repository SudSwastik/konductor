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

export type Subscription = {
  subscriptionId: string;
  subscriptionVersion: number;
  subscriptionType: "EVENT" | "API_CALLBACK";
  status: string;
  basicInfo: {
    name: string;
    description: string | null;
    goLiveDate: string | null;
  };
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

type SubscriptionSummary = Omit<Subscription, "parameters" | "triggers">;

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
    summaries.map((item) => getSubscription(item.subscriptionId)),
  );

  return settled.map((result, index) =>
    result.status === "fulfilled"
      ? result.value
      : { ...summaries[index], parameters: [], triggers: [] },
  );
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
