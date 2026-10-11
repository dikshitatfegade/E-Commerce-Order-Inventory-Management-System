import { useEffect, useState } from "react";
import api, { errMsg, money } from "../api.js";
import { useCart } from "../context/CartContext.jsx";
import { useAuth } from "../context/AuthContext.jsx";

export default function Products() {
  const [page, setPage] = useState({ content: [], page: { totalPages: 0, number: 0 } });
  const [categories, setCategories] = useState([]);
  const [categoryId, setCategoryId] = useState("");
  const [q, setQ] = useState("");
  const [pageNo, setPageNo] = useState(0);
  const [error, setError] = useState("");
  const { add } = useCart();
  const { isAdmin } = useAuth();

  useEffect(() => {
    api.get("/categories").then((r) => setCategories(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  useEffect(() => {
    const params = { page: pageNo, size: 8 };
    if (categoryId) params.categoryId = categoryId;
    if (q.trim()) params.q = q.trim();
    api.get("/products", { params })
      .then((r) => setPage(r.data))
      .catch((e) => setError(errMsg(e)));
  }, [categoryId, q, pageNo]);

  const totalPages = page.page?.totalPages ?? 0;

  return (
    <>
      <div className="d-flex gap-2 mb-3">
        <input className="form-control" placeholder="Search products..." value={q}
          onChange={(e) => { setQ(e.target.value); setPageNo(0); }} />
        <select className="form-select w-auto" value={categoryId}
          onChange={(e) => { setCategoryId(e.target.value); setPageNo(0); }}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="row g-3">
        {page.content.map((p) => (
          <div className="col-md-3" key={p.id}>
            <div className="card h-100 shadow-sm">
              <div className="card-body d-flex flex-column">
                <span className="badge bg-secondary align-self-start mb-2">{p.categoryName || "General"}</span>
                <h6 className="card-title">{p.name}</h6>
                <p className="card-text small text-muted flex-grow-1">{p.description}</p>
                <div className="fw-bold">{money(p.price)}</div>
                <div className={`small mb-2 ${p.stock > 0 ? "text-success" : "text-danger"}`}>
                  {p.stock > 0 ? `${p.stock} in stock` : "Out of stock"}
                </div>
                {!isAdmin && (
                  <button className="btn btn-sm btn-primary" disabled={p.stock === 0} onClick={() => add(p)}>
                    Add to cart
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
        {page.content.length === 0 && <p className="text-muted">No products found.</p>}
      </div>
      {totalPages > 1 && (
        <div className="d-flex justify-content-center align-items-center gap-3 mt-4">
          <button className="btn btn-outline-secondary btn-sm" disabled={pageNo === 0} onClick={() => setPageNo(pageNo - 1)}>Prev</button>
          <span className="small">Page {pageNo + 1} of {totalPages}</span>
          <button className="btn btn-outline-secondary btn-sm" disabled={pageNo + 1 >= totalPages} onClick={() => setPageNo(pageNo + 1)}>Next</button>
        </div>
      )}
    </>
  );
}
