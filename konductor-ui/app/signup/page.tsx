"use client";

import { FormEvent, useState } from "react";
import { signUp } from "aws-amplify/auth";
import { configureAmplify } from "@/lib/amplify";
import styles from "./signup.module.css";

type AccessScope = "read" | "write" | "admin";

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
    description: "Request full access review",
    icon: "A",
  },
] satisfies Array<{
  key: AccessScope;
  label: string;
  description: string;
  icon: string;
}>;

export default function SignupPage() {
  const [step, setStep] = useState(1);
  const [selectedScope, setSelectedScope] = useState<AccessScope>("read");
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

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

    if (form.password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    setStep(2);
  }

  async function submitSignup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      configureAmplify();

      await signUp({
        username: form.email,
        password: form.password,
        options: {
          userAttributes: {
            email: form.email,
            name: form.name,
          },
          clientMetadata: {
            requestedScope: selectedScope,
          },
        },
      });

      setSuccessMessage(
        `Account created. Check your email to verify it. Your ${selectedScope} access request will be reviewed.`,
      );
    } catch (caughtError) {
      setError(getSignupErrorMessage(caughtError));
    } finally {
      setIsSubmitting(false);
    }
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
                  placeholder="At least 8 characters"
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
                <h2 id="signup-title">Request access level</h2>
                <p>Step 2 of 2 - choose the access you want reviewed.</p>
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
                  {isSubmitting ? "Creating..." : "Create account"}
                </button>
              </div>
            </>
          )}

          {step === 2 && error ? <p className={styles.error}>{error}</p> : null}
          {successMessage ? (
            <p className={styles.success}>{successMessage}</p>
          ) : null}

          <p className={styles.switchAuth}>
            Already have an account? <a href="/login">Sign in</a>
          </p>
        </form>
      </section>
    </main>
  );
}

function getSignupErrorMessage(error: unknown) {
  if (error instanceof Error) {
    if (error.name === "UsernameExistsException") {
      return "An account already exists for this email.";
    }

    if (error.name === "InvalidPasswordException") {
      return "Password must match the Cognito password policy.";
    }

    if (error.name === "InvalidParameterException") {
      return "Check your signup details and try again.";
    }

    return error.message;
  }

  return "Unable to create account. Please try again.";
}
