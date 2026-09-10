"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { getCurrentUser, signOut } from "aws-amplify/auth";
import {
  ArrowRight,
  Check,
  CircleAlert,
  Eye,
  Inbox,
  LogOut,
  Pause,
  Pencil,
  Play,
  Plus,
  RefreshCw,
  Webhook,
  Waypoints,
  X,
  Zap,
  type LucideIcon,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { configureAmplify } from "@/lib/amplify";
import {
  createSubscription,
  getSubscription,
  listParameters,
  listSubscriptions,
  listTriggers,
  MasterData,
  ParameterDefinition,
  patchSubscription,
  patchSubscriptionStatus,
  replaceSubscriptionParameters,
  replaceSubscriptionTriggers,
  Subscription,
  SubscriptionSummary,
} from "@/lib/projector";
import styles from "../page.module.css";

const STATUS_NAMES: Record<string, string> = {
  ACTIVE: "Active",
  PAUSED: "Paused",
  DRAFT: "Draft",
  ARCHIVED: "Archived",
};

type DeliveryMode = "API_CALLBACK" | "EVENT";

type WizardForm = {
  name: string;
  description: string;
  deliveryMode: DeliveryMode;
  parameterCodes: string[];
  triggerCodes: string[];
  goLiveDate: string;
};

const emptyWizard: WizardForm = {
  name: "",
  description: "",
  deliveryMode: "API_CALLBACK",
  parameterCodes: [],
  triggerCodes: [],
  goLiveDate: new Date().toISOString().slice(0, 10),
};

function Icon({ children }: { children: string }) {
  const icons: Record<string, LucideIcon> = {
    hub: Waypoints,
    refresh: RefreshCw,
    add: Plus,
    logout: LogOut,
    error: CircleAlert,
    inbox: Inbox,
    visibility: Eye,
    pause: Pause,
    edit: Pencil,
    play_arrow: Play,
    webhook: Webhook,
    bolt: Zap,
    check: Check,
    arrow_forward: ArrowRight,
    close: X,
  };
  const Glyph = icons[children] || CircleAlert;
  return <span className={styles.icon} aria-hidden="true"><Glyph size={16} strokeWidth={2.25} /></span>;
}

function formatDate(value: string | null) {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat("en", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

function toSummary(subscription: Subscription): SubscriptionSummary {
  return {
    subscriptionId: subscription.subscriptionId,
    subscriptionVersion: subscription.subscriptionVersion,
    subscriptionType: subscription.subscriptionType,
    status: subscription.status,
    basicInfo: subscription.basicInfo,
  };
}

export default function SubscriptionsPage() {
  const router = useRouter();
  const [subscriptions, setSubscriptions] = useState<SubscriptionSummary[]>([]);
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
  const [editingSubscriptionId, setEditingSubscriptionId] = useState<string | null>(null);
  const [loadingSubscriptionId, setLoadingSubscriptionId] = useState<string | null>(null);

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
    { label: "Active subscriptions", value: subscriptions.filter((item) => item.status === "ACTIVE").length },
    { label: "API callbacks", value: subscriptions.filter((item) => item.subscriptionType === "API_CALLBACK").length },
    { label: "Event subscriptions", value: subscriptions.filter((item) => item.subscriptionType === "EVENT").length },
  ], [subscriptions]);

  function openWizard() {
    setEditingSubscriptionId(null);
    setWizard({
      ...emptyWizard,
      goLiveDate: new Date().toISOString().slice(0, 10),
      parameterCodes: parameters.filter((parameter) => parameter.required).map((parameter) => parameter.code),
    });
    setWizardStep(1);
    setWizardError("");
    setWizardOpen(true);
  }

  async function openSubscription(subscriptionId: string) {
    setError("");
    setLoadingSubscriptionId(subscriptionId);
    try {
      setSelectedSubscription(await getSubscription(subscriptionId));
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to load the subscription.");
    } finally {
      setLoadingSubscriptionId(null);
    }
  }

  async function openEditWizard(subscriptionId: string, loadedSubscription?: Subscription) {
    setError("");
    setLoadingSubscriptionId(subscriptionId);
    try {
      const subscription = loadedSubscription || await getSubscription(subscriptionId);
      setEditingSubscriptionId(subscription.subscriptionId);
      setWizard({
        name: subscription.basicInfo.name,
        description: subscription.basicInfo.description || "",
        deliveryMode: subscription.subscriptionType,
        parameterCodes: Array.from(new Set([
          ...parameters.filter((parameter) => parameter.required).map((parameter) => parameter.code),
          ...subscription.parameters.map((parameter) => parameter.code),
        ])),
        triggerCodes: subscription.triggers.map((trigger) => trigger.code),
        goLiveDate: subscription.basicInfo.goLiveDate || new Date().toISOString().slice(0, 10),
      });
      setWizardStep(1);
      setWizardError("");
      setSelectedSubscription(null);
      setWizardOpen(true);
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Unable to load the subscription.");
    } finally {
      setLoadingSubscriptionId(null);
    }
  }

  function closeWizard() {
    if (isSaving) return;
    setWizardOpen(false);
    setEditingSubscriptionId(null);
    setWizardError("");
  }

  function updateWizard<K extends keyof WizardForm>(key: K, value: WizardForm[K]) {
    setWizard((current) => ({ ...current, [key]: value }));
    setWizardError("");
  }

  function toggleCode(key: "parameterCodes" | "triggerCodes", value: string) {
    updateWizard(key, wizard[key].includes(value)
      ? wizard[key].filter((item) => item !== value)
      : [...wizard[key], value]);
  }

  function nextFromDetails() {
    if (!wizard.name.trim()) {
      setWizardError("Enter a subscription name.");
      return;
    }
    setWizardStep(2);
  }

  function nextFromFields() {
    if (!wizard.parameterCodes.length) {
      setWizardError("Select at least one field.");
      return;
    }
    setWizardStep(3);
  }

  async function saveSubscription() {
    if (!wizard.triggerCodes.length) {
      setWizardError("Select at least one trigger.");
      return;
    }
    if (!wizard.goLiveDate) {
      setWizardError("Choose a go-live date.");
      return;
    }

    setIsSaving(true);
    setWizardError("");
    try {
      if (editingSubscriptionId) {
        await patchSubscription(editingSubscriptionId, {
          basicInfo: {
            name: wizard.name.trim(),
            description: wizard.description.trim(),
            goLiveDate: wizard.goLiveDate,
          },
        }, actor);
        await replaceSubscriptionParameters(
          editingSubscriptionId,
          wizard.parameterCodes.map((code) => ({ code })),
          actor,
        );
        await replaceSubscriptionTriggers(
          editingSubscriptionId,
          wizard.triggerCodes.map((code) => ({ code })),
          actor,
        );
      } else {
        await createSubscription({
          subscriptionType: wizard.deliveryMode,
          basicInfo: {
            name: wizard.name.trim(),
            description: wizard.description.trim(),
            goLiveDate: wizard.goLiveDate,
          },
          parameters: wizard.parameterCodes.map((code) => ({ code })),
          triggers: wizard.triggerCodes.map((code) => ({ code })),
        }, actor);
      }
      setWizardOpen(false);
      setEditingSubscriptionId(null);
      await loadDashboard();
    } catch (caughtError) {
      setWizardError(caughtError instanceof Error ? caughtError.message : `Unable to ${editingSubscriptionId ? "update" : "create"} the subscription.`);
    } finally {
      setIsSaving(false);
    }
  }

  async function toggleStatus(subscription: SubscriptionSummary) {
    const newStatus = subscription.status === "ACTIVE" ? "PAUSED" : "ACTIVE";
    setError("");
    try {
      const updated = await patchSubscriptionStatus(
        subscription.subscriptionId,
        newStatus,
        actor,
      );
      setSubscriptions((current) => current.map((item) => item.subscriptionId === updated.subscriptionId ? toSummary(updated) : item));
      if (selectedSubscription?.subscriptionId === updated.subscriptionId) setSelectedSubscription(updated);
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
              <span className={styles.brandIcon} aria-hidden="true"><Waypoints size={20} strokeWidth={2.25} /></span>
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
                  <thead><tr><th>Subscription</th><th>Delivery</th><th>Go live</th><th>Status</th><th aria-label="Actions" /></tr></thead>
                  <tbody>{subscriptions.map((subscription) => {
                    const isHttp = subscription.subscriptionType === "API_CALLBACK";
                    return <tr key={subscription.subscriptionId}>
                      <td><button className={styles.nameButton} type="button" onClick={() => void openSubscription(subscription.subscriptionId)}><strong>{subscription.basicInfo.name}</strong><span>{subscription.basicInfo.description || subscription.subscriptionId}</span></button></td>
                      <td><span className={`${styles.typeBadge} ${isHttp ? styles.httpBadge : styles.eventBadge}`}><Icon>{isHttp ? "webhook" : "bolt"}</Icon>{isHttp ? "API callback" : "Event"}</span></td>
                      <td><span className={styles.windowDate}>{formatDate(subscription.basicInfo.goLiveDate)}</span></td>
                      <td><span className={`${styles.status} ${styles[`status${STATUS_NAMES[subscription.status] || "Draft"}`]}`}><i />{STATUS_NAMES[subscription.status] || subscription.status}</span></td>
                      <td><div className={styles.rowActions}>
                        <button type="button" title="View details" aria-label={`View ${subscription.basicInfo.name}`} disabled={loadingSubscriptionId === subscription.subscriptionId} onClick={() => void openSubscription(subscription.subscriptionId)}><Icon>visibility</Icon></button>
                        <button type="button" title="Edit subscription" aria-label={`Edit ${subscription.basicInfo.name}`} disabled={loadingSubscriptionId === subscription.subscriptionId} onClick={() => void openEditWizard(subscription.subscriptionId)}><Icon>edit</Icon></button>
                        <button type="button" title={subscription.status === "ACTIVE" ? "Pause subscription" : "Activate subscription"} aria-label={subscription.status === "ACTIVE" ? `Pause ${subscription.basicInfo.name}` : `Activate ${subscription.basicInfo.name}`} onClick={() => void toggleStatus(subscription)}><Icon>{subscription.status === "ACTIVE" ? "pause" : "play_arrow"}</Icon></button>
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
            <div><span className={styles.eyebrow}>{editingSubscriptionId ? "Edit subscription" : "New subscription"} · Step {wizardStep} of 3</span><h2 id="wizard-title">{wizardStep === 1 && (editingSubscriptionId ? "Update basic information" : "Choose a destination")}{wizardStep === 2 && "Select subscription fields"}{wizardStep === 3 && "Choose triggers and go-live date"}</h2><p>{wizardStep === 1 && (editingSubscriptionId ? "Update the subscription name, description, and go-live date." : "Name the subscription and choose where events should go.")}{wizardStep === 2 && "Choose the exact data included in every projected event."}{wizardStep === 3 && "Select the events that should publish this subscription."}</p></div>
            <button className={styles.closeButton} type="button" onClick={closeWizard} aria-label="Close wizard"><Icon>close</Icon></button>
          </div>
          <div className={styles.progress} aria-label={`Step ${wizardStep} of 3`}>{[1, 2, 3].map((step) => <span key={step} className={step <= wizardStep ? styles.progressActive : ""} />)}</div>
          <div className={styles.modalBody}>
            {wizardStep === 1 ? <div className={styles.formStack}>
              <label className={styles.field}><span>Subscription name</span><input autoFocus value={wizard.name} onChange={(event) => updateWizard("name", event.target.value)} placeholder="Order fulfillment sync" /></label>
              <label className={styles.field}><span>Description <em>Optional</em></span><textarea value={wizard.description} onChange={(event) => updateWizard("description", event.target.value)} placeholder="What this subscription is used for" rows={3} /></label>
              <fieldset className={styles.fieldset} disabled={Boolean(editingSubscriptionId)}><legend>Delivery type{editingSubscriptionId ? " (cannot be changed)" : ""}</legend><div className={styles.choiceGrid}>
                <button type="button" className={wizard.deliveryMode === "API_CALLBACK" ? styles.choiceSelected : ""} onClick={() => updateWizard("deliveryMode", "API_CALLBACK")}><span><Icon>webhook</Icon></span><strong>API callback</strong><small>Use the configured API callback</small></button>
                <button type="button" className={wizard.deliveryMode === "EVENT" ? styles.choiceSelected : ""} onClick={() => updateWizard("deliveryMode", "EVENT")}><span><Icon>bolt</Icon></span><strong>Event consumer</strong><small>Use the configured event destination</small></button>
              </div></fieldset>
            </div> : null}

            {wizardStep === 2 ? <div className={styles.optionsPanel}>
              <div className={styles.optionsToolbar}><span>{wizard.parameterCodes.length} of {parameters.length} selected</span><button type="button" onClick={() => updateWizard("parameterCodes", wizard.parameterCodes.length === parameters.length ? parameters.filter((parameter) => parameter.required).map((parameter) => parameter.code) : parameters.map((parameter) => parameter.code))}>{wizard.parameterCodes.length === parameters.length ? "Required only" : "Select all"}</button></div>
              <div className={styles.optionList}>{parameters.map((parameter) => {
                const selected = wizard.parameterCodes.includes(parameter.code);
                return <button type="button" key={parameter.code} className={selected ? styles.optionSelected : ""} onClick={() => !parameter.required && toggleCode("parameterCodes", parameter.code)}><span className={styles.checkBox}>{selected ? <Icon>check</Icon> : null}</span><span className={styles.optionCopy}><strong>{parameter.name}{parameter.required ? <em>Required</em> : null}</strong><small>{parameter.fieldPath}</small></span></button>;
              })}</div>
            </div> : null}

            {wizardStep === 3 ? <div className={styles.formStack}>
              <fieldset className={styles.fieldset}><legend>Event triggers</legend><div className={styles.triggerChoices}>{triggers.map((trigger) => {
                const selected = wizard.triggerCodes.includes(trigger.code);
                return <button type="button" key={trigger.code} className={selected ? styles.optionSelected : ""} onClick={() => toggleCode("triggerCodes", trigger.code)}><span className={styles.checkBox}>{selected ? <Icon>check</Icon> : null}</span><span><strong>{trigger.name}</strong><small>{trigger.description}</small></span></button>;
              })}</div></fieldset>
              <label className={styles.field}><span>Go-live date</span><input type="date" value={wizard.goLiveDate} onChange={(event) => updateWizard("goLiveDate", event.target.value)} /></label>
            </div> : null}
            {wizardError ? <p className={styles.wizardError} role="alert"><Icon>error</Icon>{wizardError}</p> : null}
          </div>
          <footer className={styles.modalFooter}>
            <button className={styles.textButton} type="button" onClick={closeWizard} disabled={isSaving}>Cancel</button>
            <div>{wizardStep > 1 ? <button className={styles.secondaryButton} type="button" onClick={() => { setWizardError(""); setWizardStep((current) => current - 1); }} disabled={isSaving}>Back</button> : null}<button className={styles.primaryButton} type="button" disabled={isSaving} onClick={wizardStep === 1 ? nextFromDetails : wizardStep === 2 ? nextFromFields : () => void saveSubscription()}>{isSaving ? <><span className={styles.buttonSpinner} />{editingSubscriptionId ? "Saving…" : "Creating…"}</> : wizardStep === 3 ? <><Icon>check</Icon>{editingSubscriptionId ? "Save changes" : "Create subscription"}</> : <>Continue<Icon>arrow_forward</Icon></>}</button></div>
          </footer>
        </section>
      </div> : null}

      {selectedSubscription ? <div className={styles.overlay} onMouseDown={() => setSelectedSubscription(null)}>
        <section className={`${styles.modal} ${styles.detailModal}`} role="dialog" aria-modal="true" aria-labelledby="detail-title" onMouseDown={(event) => event.stopPropagation()}>
          <div className={styles.modalHeader}><div><span className={styles.eyebrow}>Subscription details</span><h2 id="detail-title">{selectedSubscription.basicInfo.name}</h2><p>{selectedSubscription.basicInfo.description || "No description"}</p></div><button className={styles.closeButton} type="button" onClick={() => setSelectedSubscription(null)} aria-label="Close details"><Icon>close</Icon></button></div>
          <div className={styles.detailBody}>
            <dl><div><dt>Status</dt><dd>{STATUS_NAMES[selectedSubscription.status] || selectedSubscription.status}</dd></div><div><dt>Subscription type</dt><dd>{selectedSubscription.subscriptionType === "API_CALLBACK" ? "API callback" : "Event consumer"}</dd></div><div><dt>Go live</dt><dd>{formatDate(selectedSubscription.basicInfo.goLiveDate)}</dd></div></dl>
            <div className={styles.detailSection}><h3>Triggers</h3><div className={styles.detailTags}>{selectedSubscription.triggers.map((trigger) => <span key={trigger.code}>{trigger.name}</span>)}</div></div>
            <div className={styles.detailSection}><h3>Projected fields</h3><div className={styles.detailTags}>{selectedSubscription.parameters.map((parameter) => <span key={parameter.code}>{parameter.name}</span>)}</div></div>
          </div>
          <footer className={styles.modalFooter}><span /><div><button className={styles.secondaryButton} type="button" onClick={() => void openEditWizard(selectedSubscription.subscriptionId, selectedSubscription)}><Icon>edit</Icon>Edit subscription</button><button className={styles.primaryButton} type="button" onClick={() => void toggleStatus(selectedSubscription)}><Icon>{selectedSubscription.status === "ACTIVE" ? "pause" : "play_arrow"}</Icon>{selectedSubscription.status === "ACTIVE" ? "Pause subscription" : "Activate subscription"}</button></div></footer>
        </section>
      </div> : null}
    </main>
  );
}
