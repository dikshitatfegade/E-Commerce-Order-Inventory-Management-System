import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errMsg } from "../api.js";
import { useAuth } from "../context/AuthContext.jsx";

export default function Login() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    try {
      const { data } = await api.post("/auth/login", form);
      login(data);
      navigate(data.role === "ADMIN" ? "/admin" : "/");
    } catch (err) {
      setError(errMsg(err));
    }
  };

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <h3 className="mb-3">Login</h3>
        {error && <div className="alert alert-danger">{error}</div>}
        <form onSubmit={submit}>
          <input className="form-control mb-2" type="email" placeholder="Email" required
            value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <input className="form-control mb-3" type="password" placeholder="Password" required
            value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <button className="btn btn-primary w-100">Login</button>
        </form>
        <p className="mt-3 small">
          New here? <Link to="/register">Create an account</Link>
        </p>
        <div className="alert alert-secondary small">
          Demo — Admin: admin@oms.com / Admin@123 &nbsp;|&nbsp; Customer: customer@oms.com / Customer@123
        </div>
      </div>
    </div>
  );
}
