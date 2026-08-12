"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { getCurrentUser, signIn } from "aws-amplify/auth";
import { configureAmplify } from "@/lib/amplify";
import styles from "./login.module.css";

export default function LoginPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    email: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    async function redirectSignedInUser() {
      try {
        configureAmplify();
        await getCurrentUser();
        router.replace("/");
      } catch {
        // No active session. Keep the user on the login page.
      }
    }

    redirectSignedInUser();
  }, [router]);

  function updateField(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setError("");
    setSuccessMessage("");
  }

  async function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccessMessage("");

    if (!form.email.trim() || !form.password.trim()) {
      setError("Enter your email and password.");
      return;
    }

    setIsSubmitting(true);

    try {
      configureAmplify();

      await signIn({
        username: form.email,
        password: form.password,
      });

      router.push("/");
    } catch (caughtError) {
      setError(getLoginErrorMessage(caughtError));
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

      <section className={styles.formPanel} aria-labelledby="login-title">
        <form className={styles.loginForm} onSubmit={submitLogin}>
          <div className={styles.header}>
            <h2 id="login-title">Welcome back</h2>
            <p>Sign in to manage your data subscriptions.</p>
          </div>

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
              placeholder="Enter your password"
              value={form.password}
              onChange={(event) => updateField("password", event.target.value)}
            />
          </label>

          {error ? <p className={styles.error}>{error}</p> : null}
          {successMessage ? (
            <p className={styles.success}>{successMessage}</p>
          ) : null}

          <button className={styles.primaryButton} type="submit">
            {isSubmitting ? "Signing in..." : "Sign in"}
          </button>

          <p className={styles.switchAuth}>
            Don&apos;t have an account? <Link href="/signup">Create one</Link>
          </p>
        </form>
      </section>
    </main>
  );
}

function getLoginErrorMessage(error: unknown) {
  if (error instanceof Error) {
    if (error.name === "NotAuthorizedException") {
      return "Email or password is incorrect.";
    }

    if (error.name === "UserNotConfirmedException") {
      return "Verify your email before signing in.";
    }

    if (error.name === "UserNotFoundException") {
      return "Email or password is incorrect.";
    }

    if (error.name === "UserAlreadyAuthenticatedException") {
      return "You are already signed in.";
    }

    return error.message;
  }

  return "Unable to sign in. Please try again.";
}
