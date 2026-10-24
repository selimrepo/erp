import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { Button } from "primereact/button";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Message } from "primereact/message";
import { useAuth } from "../auth";
import { api } from "../api";

export function LoginPage() {
  const { credentials, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState(
    (location.state as { username?: string } | null)?.username ?? ""
  );
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  if (credentials) return <Navigate to="/" replace />;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    const next = { username: username.trim(), password };
    if (!next.username || !next.password) {
      setError("Username and password are required.");
      return;
    }
    try {
      await api.resources(next);
      login(next);
      navigate("/", { replace: true });
    } catch {
      setError("Authentication failed or the ERP API is unavailable.");
    }
  }

  return (
    <div className="login-page">
      <div className="login-background" />
      <div className="login-shell">
        <section className="login-showcase">
          <div className="login-brand">
            <div className="brand-mark login-brand-mark">E</div>
            <div>
              <strong>Erplancers</strong>
              <span>Enterprise ERP</span>
            </div>
          </div>

          <div className="login-showcase-content">
            <div className="eyebrow">ERP CONTROL CENTER</div>
            <h1>One workspace for your entire operation.</h1>
            <p>
              Manage your data, workflows, reporting and business intelligence
              from a single secure ERP platform.
            </p>
            <div className="login-features">
              <div><i className="pi pi-database" /><span>Unified business data</span></div>
              <div><i className="pi pi-chart-line" /><span>Analytics &amp; BI</span></div>
              <div><i className="pi pi-shield" /><span>Role-based security</span></div>
            </div>
          </div>

          <small className="login-version">ERPLANCERS ERP · Secure workspace</small>
        </section>

        <main className="login-panel">
          <form className="login-card" onSubmit={submit}>
            <div className="login-mobile-brand">
              <div className="brand-mark large">E</div>
            </div>

            <div className="eyebrow">WELCOME BACK</div>
            <h2>Sign in to your ERP</h2>
            <p className="login-subtitle">
              Enter your credentials to continue to the workspace.
            </p>

            {location.state?.registered && (
              <Message
                severity="success"
                text="Registration successful. You can now sign in."
              />
            )}
            {error && <Message severity="error" text={error} />}

            <div className="login-fields">
              <label>
                <span>Username</span>
                <span className="login-input">
                  <i className="pi pi-user" />
                  <InputText
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    autoFocus
                    autoComplete="username"
                    placeholder="Enter your username"
                  />
                </span>
              </label>

              <label>
                <span>Password</span>
                <span className="login-input">
                  <i className="pi pi-lock" />
                  <Password
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    feedback={false}
                    toggleMask
                    autoComplete="current-password"
                    placeholder="Enter your password"
                  />
                </span>
              </label>
            </div>

            <Button
              label="Sign in"
              icon="pi pi-arrow-right"
              iconPos="right"
              type="submit"
              className="login-submit"
            />

            <div className="login-links">
              <Link to="/register">Create a new account</Link>
              <Link to="/register-admin">Create administrator</Link>
            </div>

            <div className="login-security">
              <i className="pi pi-lock" />
              <span>Secure session · Use HTTPS in production</span>
            </div>
          </form>
        </main>
      </div>
    </div>
  );
}
