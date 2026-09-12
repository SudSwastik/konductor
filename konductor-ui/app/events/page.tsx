"use client";

import { useEffect, useState } from "react";
import { getCurrentUser } from "aws-amplify/auth";
import { ArrowLeft, Eye, RefreshCw, Waypoints } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { configureAmplify } from "@/lib/amplify";
import { Event, getEvent, listEvents } from "@/lib/projector";
import styles from "../page.module.css";

const formatDate = (value: string | null) => value ? new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—";

export default function EventsPage() {
  const router = useRouter();
  const [events, setEvents] = useState<Event[]>([]);
  const [selected, setSelected] = useState<Event | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  async function load() { setLoading(true); try { setEvents(await listEvents()); setError(""); } catch (e) { setError(e instanceof Error ? e.message : "Unable to load events."); } finally { setLoading(false); } }
  useEffect(() => { void (async () => { try { configureAmplify(); await getCurrentUser(); await load(); } catch { router.replace("/login"); } })(); }, [router]);
  return <main className={styles.page}><div className={styles.shell}><header className={styles.header}><div><div className={styles.breadcrumb}><span className={styles.brandIcon}><Waypoints size={20} /></span><span className={styles.brandName}>Konductor</span><span className={styles.slash}>/</span><span className={styles.current}>Events</span></div><h1>Event delivery</h1><p>Track projected event delivery and failures.</p></div><div className={styles.headerActions}><Link className={styles.secondaryButton} href="/subscriptions"><ArrowLeft size={16} />Subscriptions</Link><button className={styles.secondaryButton} type="button" onClick={() => void load()}><RefreshCw size={16} />Refresh</button></div></header>{error ? <div className={styles.alert} role="alert">{error}</div> : null}<section><div className={styles.sectionHeading}><div><h2>Tracked events</h2><p>{events.length} event{events.length === 1 ? "" : "s"}</p></div></div><div className={styles.tableCard}>{loading ? <div className={styles.loadingState}>Loading events…</div> : events.length ? <div className={styles.tableScroller}><table className={styles.table}><thead><tr><th>Event</th><th>Trigger</th><th>Status</th><th>Attempts</th><th>Last attempt</th><th /></tr></thead><tbody>{events.map((event) => <tr key={event.eventUid}><td><strong>{event.eventUid}</strong><span>{event.sourceEventId}</span></td><td>{event.triggerType || "—"}</td><td>{event.status || "Unknown"}</td><td>{event.attemptCount}</td><td>{formatDate(event.lastAttemptAt)}</td><td><button type="button" aria-label={`View ${event.eventUid}`} onClick={() => void getEvent(event.eventUid).then(setSelected).catch(() => setError("Unable to load event details."))}><Eye size={16} /></button></td></tr>)}</tbody></table></div> : <div className={styles.emptyState}><h3>No tracked events</h3><p>Events will appear after projection.</p></div>}</div></section>{selected ? <div className={styles.overlay} onMouseDown={() => setSelected(null)}><section className={`${styles.modal} ${styles.detailModal}`} role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}><div className={styles.modalHeader}><div><span className={styles.eyebrow}>Event details</span><h2>{selected.eventUid}</h2><p>{selected.sourceEventId}</p></div><button className={styles.closeButton} type="button" onClick={() => setSelected(null)} aria-label="Close details">×</button></div><div className={styles.detailBody}><dl><div><dt>Status</dt><dd>{selected.status || "Unknown"}</dd></div><div><dt>Attempts</dt><dd>{selected.attemptCount}</dd></div><div><dt>Delivered</dt><dd>{formatDate(selected.deliveredAt)}</dd></div><div><dt>Response</dt><dd>{selected.responseStatusCode || "—"}</dd></div></dl>{selected.errorMessage ? <div className={styles.alert} role="alert">{selected.errorMessage}</div> : null}</div></section></div> : null}</div></main>;
}
