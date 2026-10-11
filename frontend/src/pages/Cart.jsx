import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errMsg, money } from "../api.js";
import { useAuth } from "../context/AuthContext.jsx";
import { useCart } from "../context/CartContext.jsx";

export default function Cart() {
  const { items, setQty, remove, clear, total } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [address, setAddress] = useState("");
  const [payment, setPayment] = useState("COD");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (items.length === 0) {
    return <p>Your cart is empty. <Link to="/">Browse products</Link></p>;
  }

  const checkout = async (e) => {
    e.preventDefault();
    if (!user) return navigate("/login");
    setBusy(true);
    setError("");
    try {
      await api.post("/orders", {
        items: items.map((i) => ({ productId: i.product.id, quantity: i.quantity })),
        shippingAddress: address,
        paymentMethod: payment,
      });
      clear();
      navigate("/orders");
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h3>Your cart</h3>
      <table className="table align-middle">
        <thead><tr><th>Product</th><th>Price</th><th style={{ width: 120 }}>Qty</th><th>Subtotal</th><th /></tr></thead>
        <tbody>
          {items.map((i) => (
            <tr key={i.product.id}>
              <td>{i.product.name}</td>
              <td>{money(i.product.price)}</td>
              <td>
                <input type="number" min="1" max={i.product.stock} className="form-control form-control-sm"
                  value={i.quantity} onChange={(e) => setQty(i.product.id, Number(e.target.value) || 1)} />
              </td>
              <td>{money(i.quantity * i.product.price)}</td>
              <td><button className="btn btn-sm btn-outline-danger" onClick={() => remove(i.product.id)}>Remove</button></td>
            </tr>
          ))}
        </tbody>
        <tfoot><tr><th colSpan="3" className="text-end">Total</th><th>{money(total)}</th><th /></tr></tfoot>
      </table>

      {error && <div className="alert alert-danger">{error}</div>}
      <form onSubmit={checkout} className="row g-2">
        <div className="col-md-8">
          <textarea className="form-control" rows="2" placeholder="Shipping address" required
            value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
        <div className="col-md-2">
          <select className="form-select" value={payment} onChange={(e) => setPayment(e.target.value)}>
            <option value="COD">Cash on delivery</option>
            <option value="MOCK_CARD">Mock card</option>
          </select>
        </div>
        <div className="col-md-2">
          <button className="btn btn-success w-100" disabled={busy}>{busy ? "Placing..." : "Place order"}</button>
        </div>
      </form>
    </>
  );
}
