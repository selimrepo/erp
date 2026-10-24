import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Button } from "primereact/button";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Message } from "primereact/message";
import { useAuth } from "../auth";
import { api } from "../api";

export function RegisterPage() {
  const { credentials } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState("");

  if (credentials) return <Navigate to="/" replace />;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    const name = username.trim();
    if (name.length < 3) {
      setError("Username must be at least 3 characters.");
      return;
    }
    if (password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (password !== confirmation) {
      setError("Passwords do not match.");
      return;
    }

    try {
      await api.register({ username: name, password });
      navigate("/login", { replace: true, state: { registered: true, username: name } });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Registration failed.");
    }
  }

  return (
    <div className="login-page">
      <div className="login-glow" />
      <form className="login-card" onSubmit={submit}>
        <div className="brand-mark large">E</div>
        <div className="eyebrow">ERPLANCERS ERP</div>
        <h1>Create account</h1>
        <p>Register a standard ERP user account.</p>
        {error && <Message severity="error" text={error} />}
        <label>Username<InputText value={username} onChange={e => setUsername(e.target.value)} autoFocus /></label>
        <label>Password<Password value={password} onChange={e => setPassword(e.target.value)} feedback={false} toggleMask /></label>
        <label>Confirm password<Password value={confirmation} onChange={e => setConfirmation(e.target.value)} feedback={false} toggleMask /></label>
        <Button label="Create account" icon="pi pi-user-plus" iconPos="right" type="submit" className="w-full" />
        <small>New accounts receive the standard USER role.</small>
        <Link to="/login">Already have an account? Sign in</Link>
      </form>
    </div>
  );
}
