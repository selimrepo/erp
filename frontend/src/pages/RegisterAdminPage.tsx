import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Button } from "primereact/button";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { Message } from "primereact/message";
import { useAuth } from "../auth";
import { api } from "../api";

export function RegisterAdminPage() {
 const { credentials }=useAuth(); const navigate=useNavigate(); const [username,setUsername]=useState(""); const [password,setPassword]=useState(""); const [confirmation,setConfirmation]=useState(""); const [adminToken,setAdminToken]=useState(""); const [error,setError]=useState("");
 if(credentials)return <Navigate to="/" replace />;
 async function submit(e:React.FormEvent){e.preventDefault();setError("");const name=username.trim();if(name.length<3||password.length<8||password!==confirmation||adminToken.length<16){setError("Check username, password, confirmation and admin token.");return;}try{await api.registerAdmin({username:name,password,adminToken});navigate("/login",{replace:true,state:{registered:true,username:name}})}catch(e){setError(e instanceof Error?e.message:"Admin registration failed.")}}
 return <div className="login-page"><div className="login-glow"/><form className="login-card" onSubmit={submit}><div className="brand-mark large">E</div><div className="eyebrow">ERPLANCERS ERP</div><h1>Create administrator</h1><p>Bootstrap an administrator with the server-side registration token.</p>{error&&<Message severity="error" text={error}/>}<label>Username<InputText value={username} onChange={e=>setUsername(e.target.value)} autoFocus/></label><label>Password<Password value={password} onChange={e=>setPassword(e.target.value)} feedback={false} toggleMask/></label><label>Confirm password<Password value={confirmation} onChange={e=>setConfirmation(e.target.value)} feedback={false} toggleMask/></label><label>Admin registration token<Password value={adminToken} onChange={e=>setAdminToken(e.target.value)} feedback={false} toggleMask/></label><Button label="Create administrator" icon="pi pi-shield" type="submit" className="w-full"/><small>Configure the token on the server. Never commit it.</small><Link to="/register">Create a standard account</Link><Link to="/login">Back to sign in</Link></form></div>;
}