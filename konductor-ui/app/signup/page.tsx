"use client";

import { FormEvent, useState } from "react";
import styles from "./signup.module.css";

const scopes = [
  {
    key: "read",
    label: "Read",
    description: "View subscriptions & event logs",
    icon: "V",
  },
  {
    key: "write",
    label: "Write",
    description: "Create & manage subscriptions",
    icon: "W",
  },
  {
    key: "admin",
    label: "Admin",
    description: "Full access & member control",
    icon: "A",
  },
];

export default function SignupPage() {
  const [step, setStep] = useState(1);
  const [selectedScope, setSelectedScope] = useState("read");
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
  });
  const [error, setError] = useState("");

  function updateField(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setError("");
  }

  function goToScopeStep(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!form.name.trim() || !form.email.trim() || !form.password.trim()) {
      setError("Please fill in all fields.");
      return;
    }

    setStep(2);
  }

  function submitSignup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
  }

  return (
    <main className={styles.page}>
      <section className={styles.brandPanel} aria-label="Konductor">
        <div className={styles.brandGlow} />
        <div className={styles.brandMark}>
          <span className={styles.logo}>K</span>
          <span>Konductor</span>
        </div>

        <div className={styles.brandCopy}>
          <h1>
            Route every event,
            <br />
            everywhere.
          </h1>
          <p>
            Connect callbacks and consumers to your workspace events with
            granular, scope-based access control.
          </p>
        </div>

        <p className={styles.copyright}>© 2026 Konductor</p>
      </section>

      <section className={styles.formPanel} aria-labelledby="signup-title">
        <form
          className={styles.signupForm}
          onSubmit={step === 1 ? goToScopeStep : submitSignup}
        >
          <div className={styles.progress} data-step={step} aria-hidden="true">
            <span />
            <span />
          </div>

          {step === 1 ? (
            <>
              <div className={styles.header}>
                <h2 id="signup-title">Create your account</h2>
                <p>Step 1 of 2 - your details.</p>
              </div>

              <label className={styles.field}>
                <span>Full name</span>
                <input
                  type="text"
                  name="name"
                  placeholder="Jane Doe"
                  value={form.name}
                  onChange={(event) => updateField("name", event.target.value)}
                />
              </label>

              <label className={styles.field}>
                <span>Email</span>
                <input
                  type="email"
                  name="email"
                  placeholder="you@company.com"
                  value={form.email}
                  onChange={(event) => updateField("email", event.target.value)}
                />
              </label>

              <label className={styles.field}>
                <span>Password</span>
                <input
                  type="password"
                  name="password"
                  placeholder="At least 6 characters"
                  value={form.password}
                  onChange={(event) =>
                    updateField("password", event.target.value)
                  }
                />
              </label>

              {error ? <p className={styles.error}>{error}</p> : null}

              <button className={styles.primaryButton} type="submit">
                Next
              </button>
            </>
          ) : (
            <>
              <div className={styles.header}>
                <h2 id="signup-title">Choose access scope</h2>
                <p>Step 2 of 2 - pick your permission level.</p>
              </div>

              <div className={styles.scopeList}>
                {scopes.map((scope) => {
                  const isSelected = selectedScope === scope.key;

                  return (
                    <button
                      className={styles.scopeCard}
                      data-selected={isSelected}
                      key={scope.key}
                      type="button"
                      onClick={() => setSelectedScope(scope.key)}
                    >
                      <span className={styles.scopeIcon}>{scope.icon}</span>
                      <span className={styles.scopeText}>
                        <span>{scope.label}</span>
                        <span>{scope.description}</span>
                      </span>
                      <span className={styles.scopeCheck}>
                        {isSelected ? "✓" : ""}
                      </span>
                    </button>
                  );
                })}
              </div>

              <div className={styles.actions}>
                <button
                  className={styles.secondaryButton}
                  type="button"
                  onClick={() => setStep(1)}
                >
                  Back
                </button>
                <button className={styles.primaryButton} type="submit">
                  Create account
                </button>
              </div>
            </>
          )}

          <p className={styles.switchAuth}>
            Already have an account? <a href="/login">Sign in</a>
          </p>
        </form>
      </section>
    </main>
  );
}
