import { useCallback, useEffect, useState } from "react";
import api, { errMsg, money } from "../api.js";

const STATUSES = ["PLACED", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"];
const emptyProduct = { name: "", sku: "", price: "", description: "", categoryId: "", initialStock: 0 };

export default function Admin() {
  const [tab, setTab] = useState("products");
  const [msg, setMsg] = useState({ type: "", text: "" });
  const ok = (text) => setMsg({ type: "success", text });
  const fail = (e) => setMsg({ type: "danger", text: errMsg(e) });

  return (
    <>
      <h3>Admin dashboard</h3>
      <ul className="nav nav-tabs mb-3">
        {["products", "inventory", "orders"].map((t) => (
          <li className="nav-item" key={t}>
            <button className={`nav-link text-capitalize ${tab === t ? "active" : ""}`} onClick={() => { setTab(t); setMsg({ type: "", text: "" }); }}>
              {t}
            </button>
          </li>
        ))}
      </ul>
      {msg.text && <div className={`alert alert-${msg.type}`}>{msg.text}</div>}
      {tab === "products" && <ProductsTab ok={ok} fail={fail} />}
      {tab === "inventory" && <InventoryTab ok={ok} fail={fail} />}
      {tab === "orders" && <OrdersTab ok={ok} fail={fail} />}
    </>
  );
}

function ProductsTab({ ok, fail }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyProduct);
  const [editId, setEditId] = useState(null);
  const [newCategory, setNewCategory] = useState("");

  const load = useCallback(() => {
    api.get("/products", { params: { size: 100 } }).then((r) => setProducts(r.data.content)).catch(fail);
    api.get("/categories").then((r) => setCategories(r.data)).catch(fail);
  }, []);
  useEffect(() => { load(); }, [load]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const body = {
      ...form,
      price: Number(form.price),
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      initialStock: Number(form.initialStock) || 0,
    };
    try {
      if (editId) await api.put(`/admin/products/${editId}`, body);
      else await api.post("/admin/products", body);
      ok(editId ? "Product updated" : "Product created");
      setForm(emptyProduct);
      setEditId(null);
      load();
    } catch (err) { fail(err); }
  };

  const edit = (p) => {
    setEditId(p.id);
    setForm({
      name: p.name, sku: p.sku, price: p.price, description: p.description || "",
      categoryId: p.categoryId || "", initialStock: 0,
    });
  };

  const del = async (id) => {
    if (!window.confirm("Remove this product from the catalog?")) return;
    try { await api.delete(`/admin/products/${id}`); ok("Product removed"); load(); } catch (e) { fail(e); }
  };

  const addCategory = async (e) => {
    e.preventDefault();
    try { await api.post("/admin/categories", { name: newCategory }); setNewCategory(""); ok("Category added"); load(); } catch (err) { fail(err); }
  };

  return (
    <>
      <form onSubmit={submit} className="row g-2 mb-4">
        <div className="col-md-3"><input className="form-control" placeholder="Name" required value={form.name} onChange={set("name")} /></div>
        <div className="col-md-2"><input className="form-control" placeholder="SKU" required value={form.sku} onChange={set("sku")} /></div>
        <div className="col-md-2"><input className="form-control" type="number" step="0.01" min="0.01" placeholder="Price" required value={form.price} onChange={set("price")} /></div>
        <div className="col-md-3">
          <select className="form-select" value={form.categoryId} onChange={set("categoryId")}>
            <option value="">No category</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        {!editId && (
          <div className="col-md-2"><input className="form-control" type="number" min="0" placeholder="Initial stock" value={form.initialStock} onChange={set("initialStock")} /></div>
        )}
        <div className="col-md-9"><input className="form-control" placeholder="Description" value={form.description} onChange={set("description")} /></div>
        <div className="col-md-3 d-flex gap-2">
          <button className="btn btn-primary flex-grow-1">{editId ? "Update" : "Add product"}</button>
          {editId && <button type="button" className="btn btn-outline-secondary" onClick={() => { setEditId(null); setForm(emptyProduct); }}>Cancel</button>}
        </div>
      </form>

      <form onSubmit={addCategory} className="d-flex gap-2 mb-4" style={{ maxWidth: 400 }}>
        <input className="form-control form-control-sm" placeholder="New category" required value={newCategory} onChange={(e) => setNewCategory(e.target.value)} />
        <button className="btn btn-sm btn-outline-primary">Add</button>
      </form>

      <table className="table table-sm align-middle">
        <thead><tr><th>ID</th><th>Name</th><th>SKU</th><th>Category</th><th>Price</th><th>Stock</th><th /></tr></thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td><td>{p.name}</td><td>{p.sku}</td><td>{p.categoryName || "-"}</td>
              <td>{money(p.price)}</td><td>{p.stock}</td>
              <td className="text-end">
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => edit(p)}>Edit</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => del(p.id)}>Remove</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

function InventoryTab({ ok, fail }) {
  const [rows, setRows] = useState([]);
  const [edits, setEdits] = useState({});

  const load = useCallback(() => {
    api.get("/admin/inventory").then((r) => setRows(r.data)).catch(fail);
  }, []);
  useEffect(() => { load(); }, [load]);

  const save = async (productId) => {
    try {
      await api.put(`/admin/inventory/${productId}`, { quantity: Number(edits[productId]) });
      ok("Stock updated");
      setEdits((prev) => { const n = { ...prev }; delete n[productId]; return n; });
      load();
    } catch (e) { fail(e); }
  };

  return (
    <table className="table table-sm align-middle">
      <thead><tr><th>Product</th><th>SKU</th><th>Quantity</th><th style={{ width: 220 }}>Set quantity</th></tr></thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.productId} className={r.quantity < 10 ? "table-warning" : ""}>
            <td>{r.productName}</td><td>{r.sku}</td><td>{r.quantity}</td>
            <td>
              <div className="input-group input-group-sm">
                <input type="number" min="0" className="form-control" value={edits[r.productId] ?? ""}
                  onChange={(e) => setEdits({ ...edits, [r.productId]: e.target.value })} />
                <button className="btn btn-primary" disabled={edits[r.productId] === undefined || edits[r.productId] === ""}
                  onClick={() => save(r.productId)}>Save</button>
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function OrdersTab({ ok, fail }) {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState("");

  const load = useCallback(() => {
    api.get("/admin/orders", { params: filter ? { status: filter } : {} })
      .then((r) => setOrders(r.data)).catch(fail);
  }, [filter]);
  useEffect(() => { load(); }, [load]);

  const changeStatus = async (id, status) => {
    try { await api.put(`/admin/orders/${id}/status`, { status }); ok(`Order #${id} → ${status}`); load(); } catch (e) { fail(e); load(); }
  };

  return (
    <>
      <select className="form-select w-auto mb-3" value={filter} onChange={(e) => setFilter(e.target.value)}>
        <option value="">All statuses</option>
        {STATUSES.map((s) => <option key={s}>{s}</option>)}
      </select>
      <table className="table table-sm align-middle">
        <thead><tr><th>#</th><th>Customer</th><th>Items</th><th>Total</th><th>Payment</th><th>Date</th><th>Status</th></tr></thead>
        <tbody>
          {orders.map((o) => {
            const locked = o.status === "CANCELLED" || o.status === "DELIVERED";
            return (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.customerName}<div className="small text-muted">{o.customerEmail}</div></td>
                <td className="small">{o.items.map((i) => `${i.productName} ×${i.quantity}`).join(", ")}</td>
                <td>{money(o.totalAmount)}</td>
                <td>{o.paymentMethod}</td>
                <td className="small">{new Date(o.createdAt).toLocaleString()}</td>
                <td>
                  <select className="form-select form-select-sm" value={o.status} disabled={locked}
                    onChange={(e) => changeStatus(o.id, e.target.value)}>
                    {STATUSES.map((s) => <option key={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {orders.length === 0 && <p className="text-muted">No orders.</p>}
    </>
  );
}
