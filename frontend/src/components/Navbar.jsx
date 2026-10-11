import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { useCart } from "../context/CartContext.jsx";

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();

  return (
    <nav className="navbar navbar-expand navbar-dark bg-dark mb-4">
      <div className="container">
        <Link className="navbar-brand" to="/">OMS Store</Link>
        <div className="navbar-nav me-auto">
          <Link className="nav-link" to="/">Products</Link>
          {user && !isAdmin && <Link className="nav-link" to="/orders">My Orders</Link>}
          {isAdmin && <Link className="nav-link" to="/admin">Admin</Link>}
        </div>
        <div className="d-flex align-items-center gap-2">
          {!isAdmin && (
            <Link className="btn btn-outline-light btn-sm" to="/cart">
              Cart <span className="badge bg-warning text-dark">{count}</span>
            </Link>
          )}
          {user ? (
            <>
              <span className="text-light small">{user.name}</span>
              <button
                className="btn btn-light btn-sm"
                onClick={() => {
                  logout();
                  navigate("/login");
                }}
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link className="btn btn-light btn-sm" to="/login">Login</Link>
              <Link className="btn btn-outline-light btn-sm" to="/register">Register</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
