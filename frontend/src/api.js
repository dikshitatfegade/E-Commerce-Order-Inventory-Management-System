import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
});

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("token");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

export const errMsg = (e) => {
  const d = e?.response?.data;
  if (d?.errors) return Object.values(d.errors).join(", ");
  return d?.message || e?.message || "Something went wrong";
};

export const money = (v) => `₹${Number(v).toFixed(2)}`;

export default api;
