"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { getCurrentUser, signOut } from "aws-amplify/auth";
import { useRouter } from "next/navigation";
import { configureAmplify } from "@/lib/amplify";
import {
  createSubscription,
  listParameters,
  listSubscriptions,
  listTriggers,
  MasterData,
  ParameterDefinition,
  patchSubscription,
  Subscription,
} from "@/lib/projector";
import styles from "./page.module.css";

const STATUS_NAMES: Record<number, string> = {
  1: "Active",
  2: "Paused",
  3: "Draft",
  4: "Archived",
};

type DeliveryMode = "HTTP" | "KAFKA";

type WizardForm = {
  name: string;
  description: string;
  deliveryMode: DeliveryMode;
  endpoint: string;
  parameterIds: number[];
  triggerIds: number[];
  startDate: string;
  endDate: string;
};

const emptyWizard: WizardForm = {
  name: "",
  description: "",
  deliveryMode: "HTTP",
  endpoint: "",
  parameterIds: [],
  triggerIds: [],
  startDate: new Date().toISOString().slice(0, 10),
  endDate: "",
};

function Icon({ children }: { children: string }) {
  const symbols: Record<string, string> = {
    hub: "◆",
    refresh: "↻",
    add: "+",
    logout: "↪",
    error: "!",
    inbox: "□",
    visibility: "◉",
    pause: "Ⅱ",
    play_arrow: "▶",
    webhook: "↗",
    bolt: "ϟ",
    link: "↗",
    tag: "#",
    check: "✓",
    arrow_forward: "→",
    close: "×",
  };
  return <span className={styles.icon} aria-hidden="true">{symbols[children] || "•"}</span>;
}

function formatDate(value: string | null) {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat("en", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

export default function SubscriptionsPage() {
  const router = useRouter();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [triggers, setTriggers] = useState<MasterData[]>([]);
  const [parameters, setParameters] = useState<ParameterDefinition[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [actor, setActor] = useState("");
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(1);
  const [wizard, setWizard] = useState<WizardForm>(emptyWizard);
  const [wizardError, setWizardError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [selectedSubscription, setSelectedSubscription] = useState<Subscription | null>(null);

  const loadDashboard = useCallback(async () => {
    setError("");
    setIsLoading(true);
    try {
      const [subscriptionData, triggerData, parameterData] = await Promise.all([
        listSubscriptions(),
        listTriggers(),
        listParameters(),
      ]);
      setSubscriptions(subscriptionData);
      setTriggers(triggerData);
      setParameters(parameterData);
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to load subscriptions.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    async function boot() {
      try {
        configureAmplify();
        const user = await getCurrentUser();
        setActor(user.signInDetails?.loginId || user.username);
        await loadDashboard();
      } catch {
        router.replace("/login");
      }
    }
    boot();
  }, [loadDashboard, router]);

  const stats = useMemo(() => [
    { label: "Total subscriptions", value: subscriptions.length },
    { label: "Active subscriptions", value: subscriptions.filter((item) => item.subscriptionStatusId === 1).length },
    { label: "API callbacks", value: subscriptions.filter((item) => item.deliveryConfig?.deliveryType === "HTTP").length },
    { label: "Event subscriptions", value: subscriptions.filter((item) => item.deliveryConfig?.deliveryType !== "HTTP").length },
  ], [subscriptions]);

  const triggerName = useMemo(
    () => new Map(triggers.map((trigger) => [trigger.id, trigger.name])),
    [triggers],
  );

  function openWizard() {
    setWizard({
      ...emptyWizard,
      startDate: new Date().toISOString().slice(0, 10),
      parameterIds: parameters.filter((parameter) => parameter.required).map((parameter) => parameter.id),
    });
    setWizardStep(1);
    setWizardError("");
    setWizardOpen(true);
  }

  function closeWizard() {
    if (isSaving) return;
    setWizardOpen(false);
    setWizardError("");
  }

  function updateWizard<K extends keyof WizardForm>(key: K, value: WizardForm[K]) {
    setWizard((current) => ({ ...current, [key]: value }));
    setWizardError("");
  }

  function toggleNumber(key: "parameterIds" | "triggerIds", value: number) {
    updateWizard(key, wizard[key].includes(value)
      ? wizard[key].filter((item) => item !== value)
      : [...wizard[key], value]);
  }

  function nextFromDetails() {
    if (!wizard.name.trim()) {
      setWizardError("Enter a subscription name.");
      return;
    }
    if (!wizard.endpoint.trim()) {
      setWizardError(wizard.deliveryMode === "HTTP" ? "Enter a callback URL." : "Enter a Kafka topic.");
      return;
    }
    if (wizard.deliveryMode === "HTTP" && !/^https?:\/\//i.test(wizard.endpoint)) {
      setWizardError("Enter a valid http:// or https:// callback URL.");
      return;
    }
    setWizardStep(2);
  }

  function nextFromFields() {
    if (!wizard.parameterIds.length) {
      setWizardError("Select at least one field.");
      return;
    }
    setWizardStep(3);
  }

  async function saveSubscription() {
    if (!wizard.triggerIds.length) {
      setWizardError("Select at least one trigger.");
      return;
    }
    if (!wizard.startDate) {
      setWizardError("Choose a start date.");
      return;
    }
    if (wizard.endDate && wizard.endDate < wizard.startDate) {
      setWizardError("End date must be on or after the start date.");
      return;
    }

    setIsSaving(true);
    setWizardError("");
    try {
      await createSubscription({
        subscriptionTypeId: 1,
        subscriptionStatusId: 1,
        name: wizard.name.trim(),
        description: wizard.description.trim(),
        activatedAt: new Date(`${wizard.startDate}T00:00:00`).toISOString(),
        deactivatedAt: wizard.endDate ? new Date(`${wizard.endDate}T23:59:59`).toISOString() : null,
        triggers: wizard.triggerIds.map((eventTriggerTypeId) => ({
          eventTriggerTypeId,
          parameterDefinitionIds: wizard.parameterIds,
        })),
        deliveryConfig: {
          deliveryType: wizard.deliveryMode,
          endpointUrl: wizard.endpoint.trim(),
          httpMethod: wizard.deliveryMode === "HTTP" ? "POST" : null,
          timeoutSeconds: 30,
          maxRetryCount: 3,
          retryBackoffSeconds: 10,
        },
      }, actor);
      setWizardOpen(false);
      await loadDashboard();
    } catch (caughtError) {
      setWizardError(caughtError instanceof Error ? caughtError.message : "Unable to create the subscription.");
    } finally {
      setIsSaving(false);
    }
  }

  async function toggleStatus(subscription: Subscription) {
    const newStatus = subscription.subscriptionStatusId === 1 ? 2 : 1;
    setError("");
    try {
      const updated = await patchSubscription(subscription.subscriptionUid, { subscriptionStatusId: newStatus }, actor);
      setSubscriptions((current) => current.map((item) => item.subscriptionUid === updated.subscriptionUid ? updated : item));
      if (selectedSubscription?.subscriptionUid === updated.subscriptionUid) setSelectedSubscription(updated);
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to update the subscription.");
    }
  }

  async function logOut() {
    await signOut();
    router.replace("/login");
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.header}>
          <div>
            <div className={styles.breadcrumb}>
              <span className={styles.brandIcon}>K</span>
              <span className={styles.brandName}>Konductor</span><span className={styles.slash}>/</span><span className={styles.current}>Subscriptions</span>
            </div>
            <h1>Data subscriptions</h1>
            <p>Route generated events to callbacks and event consumers across your workspace.</p>
          </div>
          <div className={styles.headerActions}>
            <button className={styles.secondaryButton} onClick={() => void loadDashboard()} type="button"><Icon>refresh</Icon>Refresh</button>
            <button className={styles.primaryButton} onClick={openWizard} type="button"><Icon>add</Icon>Add subscription</button>
            <button className={styles.secondaryButton} onClick={() => void logOut()} type="button"><Icon>logout</Icon>Sign out</button>
          </div>
        </header>

        <section className={styles.stats} aria-label="Subscription summary">
          {stats.map((stat) => <article className={styles.statCard} key={stat.label}><span>{stat.label}</span><strong>{stat.value.toLocaleString()}</strong></article>)}
        </section>

        <section aria-labelledby="subscription-list-title">
          <div className={styles.sectionHeading}>
            <div><h2 id="subscription-list-title">Your subscriptions</h2><p>{subscriptions.length} configured across this workspace</p></div>
          </div>

          {error ? <div className={styles.alert} role="alert"><Icon>error</Icon><div><strong>Couldn&apos;t load the subscription service</strong><span>{error}</span></div><button type="button" onClick={() => void loadDashboard()}>Try again</button></div> : null}

          <div className={styles.tableCard}>
            {isLoading ? <div className={styles.loadingState} aria-live="polite"><span className={styles.spinner} />Loading subscriptions…</div>
              : subscriptions.length ? <div className={styles.tableScroller}>
                <table className={styles.table}>
                  <thead><tr><th>Subscription</th><th>Delivery</th><th>Fields</th><th>Triggers</th><th>Live window</th><th>Status</th><th aria-label="Actions" /></tr></thead>
                  <tbody>{subscriptions.map((subscription) => {
                    const parameterCount = new Set(subscription.triggers.flatMap((trigger) => trigger.parameterDefinitionIds)).size;
                    const isHttp = subscription.deliveryConfig?.deliveryType === "HTTP";
                    return <tr key={subscription.subscriptionUid}>
                      <td><button className={styles.nameButton} type="button" onClick={() => setSelectedSubscription(subscription)}><strong>{subscription.name}</strong><span>{subscription.description || subscription.subscriptionUid}</span></button></td>
                      <td><span className={`${styles.typeBadge} ${isHttp ? styles.httpBadge : styles.eventBadge}`}><Icon>{isHttp ? "webhook" : "bolt"}</Icon>{isHttp ? "API callback" : "Event"}</span></td>
                      <td><span className={styles.countBadge}>{parameterCount}</span></td>
                      <td><div className={styles.triggerList}>{subscription.triggers.slice(0, 2).map((trigger) => <span key={trigger.eventTriggerTypeId}>{triggerName.get(trigger.eventTriggerTypeId) || `Trigger ${trigger.eventTriggerTypeId}`}</span>)}{subscription.triggers.length > 2 ? <span>+{subscription.triggers.length - 2}</span> : null}</div></td>
                      <td><span className={styles.windowDate}>{formatDate(subscription.activatedAt)}</span><span className={styles.windowEnd}>to {subscription.deactivatedAt ? formatDate(subscription.deactivatedAt) : "No end date"}</span></td>
                      <td><span className={`${styles.status} ${styles[`status${STATUS_NAMES[subscription.subscriptionStatusId] || "Draft"}`]}`}><i />{STATUS_NAMES[subscription.subscriptionStatusId] || "Unknown"}</span></td>
                      <td><div className={styles.rowActions}>
                        <button type="button" title="View details" aria-label={`View ${subscription.name}`} onClick={() => setSelectedSubscription(subscription)}><Icon>visibility</Icon></button>
                        <button type="button" title={subscription.subscriptionStatusId === 1 ? "Pause subscription" : "Activate subscription"} aria-label={subscription.subscriptionStatusId === 1 ? `Pause ${subscription.name}` : `Activate ${subscription.name}`} onClick={() => void toggleStatus(subscription)}><Icon>{subscription.subscriptionStatusId === 1 ? "pause" : "play_arrow"}</Icon></button>
                      </div></td>
                    </tr>;
                  })}</tbody>
                </table>
              </div> : <div className={styles.emptyState}><span className={styles.emptyIcon}><Icon>inbox</Icon></span><h3>No subscriptions yet</h3><p>Create one to start routing workspace events.</p><button className={styles.primaryButton} type="button" onClick={openWizard}><Icon>add</Icon>Add subscription</button></div>}
          </div>
        </section>
      </div>

      {wizardOpen ? <div className={styles.overlay} onMouseDown={closeWizard}>
        <section className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="wizard-title" onMouseDown={(event) => event.stopPropagation()}>
          <div className={styles.modalHeader}>
            <div><span className={styles.eyebrow}>New subscription · Step {wizardStep} of 3</span><h2 id="wizard-title">{wizardStep === 1 && "Choose a destination"}{wizardStep === 2 && "Select subscription fields"}{wizardStep === 3 && "Choose triggers and schedule"}</h2><p>{wizardStep === 1 && "Name the subscription and choose where events should go."}{wizardStep === 2 && "Choose the exact data included in every projected event."}{wizardStep === 3 && "Select the events that should publish this subscription."}</p></div>
            <button className={styles.closeButton} type="button" onClick={closeWizard} aria-label="Close wizard"><Icon>close</Icon></button>
          </div>
          <div className={styles.progress} aria-label={`Step ${wizardStep} of 3`}>{[1, 2, 3].map((step) => <span key={step} className={step <= wizardStep ? styles.progressActive : ""} />)}</div>
          <div className={styles.modalBody}>
            {wizardStep === 1 ? <div className={styles.formStack}>
              <label className={styles.field}><span>Subscription name</span><input autoFocus value={wizard.name} onChange={(event) => updateWizard("name", event.target.value)} placeholder="Order fulfillment sync" /></label>
              <label className={styles.field}><span>Description <em>Optional</em></span><textarea value={wizard.description} onChange={(event) => updateWizard("description", event.target.value)} placeholder="What this subscription is used for" rows={3} /></label>
              <fieldset className={styles.fieldset}><legend>Delivery type</legend><div className={styles.choiceGrid}>
                <button type="button" className={wizard.deliveryMode === "HTTP" ? styles.choiceSelected : ""} onClick={() => updateWizard("deliveryMode", "HTTP")}><span><Icon>webhook</Icon></span><strong>API callback</strong><small>POST events to an HTTPS endpoint</small></button>
                <button type="button" className={wizard.deliveryMode === "KAFKA" ? styles.choiceSelected : ""} onClick={() => updateWizard("deliveryMode", "KAFKA")}><span><Icon>bolt</Icon></span><strong>Event consumer</strong><small>Publish events to a Kafka topic</small></button>
              </div></fieldset>
              <label className={styles.field}><span>{wizard.deliveryMode === "HTTP" ? "Callback URL" : "Kafka topic"}</span><div className={styles.inputWithIcon}><Icon>{wizard.deliveryMode === "HTTP" ? "link" : "tag"}</Icon><input value={wizard.endpoint} onChange={(event) => updateWizard("endpoint", event.target.value)} placeholder={wizard.deliveryMode === "HTTP" ? "https://api.example.com/events" : "orders.projected"} /></div></label>
            </div> : null}

            {wizardStep === 2 ? <div className={styles.optionsPanel}>
              <div className={styles.optionsToolbar}><span>{wizard.parameterIds.length} of {parameters.length} selected</span><button type="button" onClick={() => updateWizard("parameterIds", wizard.parameterIds.length === parameters.length ? parameters.filter((parameter) => parameter.required).map((parameter) => parameter.id) : parameters.map((parameter) => parameter.id))}>{wizard.parameterIds.length === parameters.length ? "Required only" : "Select all"}</button></div>
              <div className={styles.optionList}>{parameters.map((parameter) => {
                const selected = wizard.parameterIds.includes(parameter.id);
                return <button type="button" key={parameter.id} className={selected ? styles.optionSelected : ""} onClick={() => !parameter.required && toggleNumber("parameterIds", parameter.id)}><span className={styles.checkBox}>{selected ? <Icon>check</Icon> : null}</span><span className={styles.optionCopy}><strong>{parameter.name}{parameter.required ? <em>Required</em> : null}</strong><small>{parameter.fieldPath}</small></span></button>;
              })}</div>
            </div> : null}

            {wizardStep === 3 ? <div className={styles.formStack}>
              <fieldset className={styles.fieldset}><legend>Event triggers</legend><div className={styles.triggerChoices}>{triggers.map((trigger) => {
                const selected = wizard.triggerIds.includes(trigger.id);
                return <button type="button" key={trigger.id} className={selected ? styles.optionSelected : ""} onClick={() => toggleNumber("triggerIds", trigger.id)}><span className={styles.checkBox}>{selected ? <Icon>check</Icon> : null}</span><span><strong>{trigger.name}</strong><small>{trigger.description}</small></span></button>;
              })}</div></fieldset>
              <fieldset className={styles.fieldset}><legend>Live window</legend><div className={styles.dateGrid}><label className={styles.field}><span>Start date</span><input type="date" value={wizard.startDate} onChange={(event) => updateWizard("startDate", event.target.value)} /></label><label className={styles.field}><span>End date <em>Optional</em></span><input type="date" min={wizard.startDate} value={wizard.endDate} onChange={(event) => updateWizard("endDate", event.target.value)} /></label></div></fieldset>
            </div> : null}
            {wizardError ? <p className={styles.wizardError} role="alert"><Icon>error</Icon>{wizardError}</p> : null}
          </div>
          <footer className={styles.modalFooter}>
            <button className={styles.textButton} type="button" onClick={closeWizard} disabled={isSaving}>Cancel</button>
            <div>{wizardStep > 1 ? <button className={styles.secondaryButton} type="button" onClick={() => { setWizardError(""); setWizardStep((current) => current - 1); }} disabled={isSaving}>Back</button> : null}<button className={styles.primaryButton} type="button" disabled={isSaving} onClick={wizardStep === 1 ? nextFromDetails : wizardStep === 2 ? nextFromFields : () => void saveSubscription()}>{isSaving ? <><span className={styles.buttonSpinner} />Creating…</> : wizardStep === 3 ? <><Icon>check</Icon>Create subscription</> : <>Continue<Icon>arrow_forward</Icon></>}</button></div>
          </footer>
        </section>
      </div> : null}

      {selectedSubscription ? <div className={styles.overlay} onMouseDown={() => setSelectedSubscription(null)}>
        <section className={`${styles.modal} ${styles.detailModal}`} role="dialog" aria-modal="true" aria-labelledby="detail-title" onMouseDown={(event) => event.stopPropagation()}>
          <div className={styles.modalHeader}><div><span className={styles.eyebrow}>Subscription details</span><h2 id="detail-title">{selectedSubscription.name}</h2><p>{selectedSubscription.description || "No description"}</p></div><button className={styles.closeButton} type="button" onClick={() => setSelectedSubscription(null)} aria-label="Close details"><Icon>close</Icon></button></div>
          <div className={styles.detailBody}>
            <dl><div><dt>Status</dt><dd>{STATUS_NAMES[selectedSubscription.subscriptionStatusId] || "Unknown"}</dd></div><div><dt>Delivery</dt><dd>{selectedSubscription.deliveryConfig?.deliveryType === "HTTP" ? "API callback" : "Event consumer"}</dd></div><div className={styles.detailWide}><dt>Destination</dt><dd className={styles.mono}>{selectedSubscription.deliveryConfig?.endpointUrl || "Managed subscription topic"}</dd></div><div><dt>Starts</dt><dd>{formatDate(selectedSubscription.activatedAt)}</dd></div><div><dt>Ends</dt><dd>{selectedSubscription.deactivatedAt ? formatDate(selectedSubscription.deactivatedAt) : "No end date"}</dd></div></dl>
            <div className={styles.detailSection}><h3>Triggers</h3><div className={styles.detailTags}>{selectedSubscription.triggers.map((trigger) => <span key={trigger.eventTriggerTypeId}>{triggerName.get(trigger.eventTriggerTypeId) || `Trigger ${trigger.eventTriggerTypeId}`}</span>)}</div></div>
            <div className={styles.detailSection}><h3>Projected fields</h3><p>{new Set(selectedSubscription.triggers.flatMap((trigger) => trigger.parameterDefinitionIds)).size.toLocaleString()} fields selected</p></div>
          </div>
          <footer className={styles.modalFooter}><span /><button className={styles.primaryButton} type="button" onClick={() => void toggleStatus(selectedSubscription)}><Icon>{selectedSubscription.subscriptionStatusId === 1 ? "pause" : "play_arrow"}</Icon>{selectedSubscription.subscriptionStatusId === 1 ? "Pause subscription" : "Activate subscription"}</button></footer>
        </section>
      </div> : null}
    </main>
  );
}
