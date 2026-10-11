import { useEffect, useState } from "react";
import api, { errMsg, money } from "../api.js";

const badge = {
  PLACED: "primary", CONFIRMED: "info", SHIPPED: "warning", DELIVERED: "success", CANCELLED: "secondary",
};

export default function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");

  const load = () =>
    api.get("/orders/my").then((r) => setOrders(r.data)).catch((e) => setError(errMsg(e)));

  useEffect(() => { load(); }, []);

  const cancel = async (id) => {
    if (!window.confirm("Cancel this order?")) return;
    try {
      await api.post(`/orders/${id}/cancel`);
      load();
    } catch (e) {
      setError(errMsg(e));
    }
  };

  return (
    <>
      <h3>My orders</h3>
      {error && <div className="alert alert-danger">{error}</div>}
      {orders.length === 0 && <p className="text-muted">You have not placed any orders yet.</p>}
      {orders.map((o) => (
        <div className="card mb-3" key={o.id}>
          <div className="card-header d-flex justify-content-between">
            <span>Order #{o.id} · {new Date(o.createdAt).toLocaleString()}</span>
            <span className={`badge bg-${badge[o.status]}`}>{o.status}</span>
          </div>
          <div className="card-body">
            <ul className="mb-2">
              {o.items.map((i) => (
                <li key={i.productId}>{i.productName} × {i.quantity} — {money(i.unitPrice * i.quantity)}</li>
              ))}
            </ul>
            <div className="d-flex justify-content-between align-items-center">
              <div>
                <strong>Total: {money(o.totalAmount)}</strong>
                <span className="text-muted small"> · {o.paymentMethod} · {o.shippingAddress}</span>
              </div>
              {(o.status === "PLACED" || o.status === "CONFIRMED") && (
                <button className="btn btn-sm btn-outline-danger" onClick={() => cancel(o.id)}>Cancel</button>
              )}
            </div>
          </div>
        </div>
      ))}
    </>
  );
}
