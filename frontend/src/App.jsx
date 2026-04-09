import { useCallback, useEffect, useMemo, useState } from "react";
import "./App.css";

const apiBase = import.meta.env.VITE_API_BASE || "http://localhost:8080";
const SIZE_OPTIONS = ["S", "M", "L"];

const ORDER_STATUS_VI = {
  PLACED: "Đã đặt",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chế biến",
  READY: "Sẵn sàng",
  OUT_FOR_DELIVERY: "Đang giao",
  DELIVERED: "Hoàn tất",
  CANCELLED: "Đã hủy",
};

const DELIVERY_STATUS_VI = {
  PENDING: "Chờ lấy hàng",
  PICKED_UP: "Đã lấy hàng",
  EN_ROUTE: "Đang trên đường",
  DELIVERED: "Đã giao",
};

const ORDER_FLOW = ["PLACED", "CONFIRMED", "PREPARING", "READY", "OUT_FOR_DELIVERY", "DELIVERED"];
const DELIVERY_FLOW = ["PENDING", "PICKED_UP", "EN_ROUTE", "DELIVERED"];

async function apiJson(path, { method = "GET", body } = {}) {
  const opts = { method };
  if (body !== undefined) {
    opts.body = JSON.stringify(body);
    opts.headers = { "Content-Type": "application/json" };
  }
  const res = await fetch(`${apiBase}${path}`, opts);
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { error: text };
    }
  }
  if (!res.ok) {
    throw new Error(data?.error || `HTTP ${res.status}`);
  }
  return data;
}

async function fetchPaymentLatest(orderId) {
  const res = await fetch(`${apiBase}/delivery/payments/orders/${orderId}/latest`);
  const text = await res.text();
  if (!res.ok) return null;
  if (!text) return null;
  try {
    const j = JSON.parse(text);
    return j?.payment ?? null;
  } catch {
    return null;
  }
}

async function fetchDeliveryByOrder(orderId) {
  const res = await fetch(`${apiBase}/delivery/deliveries/orders/${orderId}`);
  const text = await res.text();
  if (!res.ok) return null;
  if (!text) return null;
  try {
    const j = JSON.parse(text);
    return j?.delivery ?? null;
  } catch {
    return null;
  }
}

async function fetchFoodPrice(foodId, size) {
  const s = size || "M";
  return await apiJson(`/menu/foods/${foodId}/price?size=${encodeURIComponent(s)}`);
}

async function fetchComboPrice(comboId) {
  return await apiJson(`/menu/combos/${comboId}/price`);
}

export default function App() {
  const [tab, setTab] = useState("menu");
  const [foods, setFoods] = useState([]);
  const [quantities, setQuantities] = useState({});
  const [sizes, setSizes] = useState({});
  const [combos, setCombos] = useState([]);
  const [comboQty, setComboQty] = useState({});
  const [quotes, setQuotes] = useState({});
  const [customerName, setCustomerName] = useState("Khách");
  const [menuLoading, setMenuLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [foodModalId, setFoodModalId] = useState(null);
  const [foodModal, setFoodModal] = useState(null);
  const [foodModalLoading, setFoodModalLoading] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [reviewDraft, setReviewDraft] = useState({ rating: 5, authorName: "Khách", comment: "" });

  const showToast = useCallback((type, text) => {
    setToast({ type, text });
    setTimeout(() => setToast(null), 5000);
  }, []);

  const loadMenu = useCallback(async () => {
    setMenuLoading(true);
    try {
      const [foodData, comboData] = await Promise.all([
        apiJson("/menu/foods?availableOnly=true"),
        apiJson("/menu/combos"),
      ]);
      setFoods(foodData);
      setCombos(comboData);
      setQuantities((prev) => {
        const next = { ...prev };
        foodData.forEach((f) => {
          if (next[f.id] === undefined) next[f.id] = 0;
        });
        return next;
      });
      setSizes((prev) => {
        const next = { ...prev };
        foodData.forEach((f) => {
          if (!next[f.id]) next[f.id] = "M";
        });
        return next;
      });
      setComboQty((prev) => {
        const next = { ...prev };
        comboData.forEach((c) => {
          if (next[c.id] === undefined) next[c.id] = 0;
        });
        return next;
      });
    } catch {
      showToast("error", "Không tải được menu. Kiểm tra gateway (port 8080).");
    } finally {
      setMenuLoading(false);
    }
  }, [showToast]);

  const loadOrders = useCallback(async () => {
    setOrdersLoading(true);
    try {
      const data = await apiJson("/orders");
      setOrders(data.sort((a, b) => b.id - a.id));
    } catch {
      showToast("error", "Không tải được danh sách đơn hàng.");
    } finally {
      setOrdersLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    loadMenu();
  }, [loadMenu]);

  useEffect(() => {
    if (tab === "orders") loadOrders();
  }, [tab, loadOrders]);

  const refreshDetail = useCallback(
    async (orderId) => {
      setDetailLoading(true);
      try {
        const [order, payment, delivery] = await Promise.all([
          apiJson(`/orders/${orderId}`),
          fetchPaymentLatest(orderId),
          fetchDeliveryByOrder(orderId),
        ]);
        setDetail({ order, payment, delivery });
      } catch (e) {
        showToast("error", e.message || "Lỗi tải chi tiết đơn");
        setDetail(null);
      } finally {
        setDetailLoading(false);
      }
    },
    [showToast]
  );

  const toggleExpand = async (id) => {
    if (expandedId === id) {
      setExpandedId(null);
      setDetail(null);
      return;
    }
    setExpandedId(id);
    setDetail(null);
    await refreshDetail(id);
  };

  const cartLines = useMemo(() => {
    const foodLines = foods
      .map((f) => ({ kind: "FOOD", id: f.id, name: f.name, qty: quantities[f.id] ?? 0, size: sizes[f.id] || "M" }))
      .filter((x) => x.qty > 0);
    const comboLines = combos
      .map((c) => ({ kind: "COMBO", id: c.id, name: c.name, qty: comboQty[c.id] ?? 0 }))
      .filter((x) => x.qty > 0);
    return [...foodLines, ...comboLines];
  }, [foods, quantities, sizes, combos, comboQty]);

  const cartTotal = useMemo(() => {
    return cartLines.reduce((s, l) => {
      const key = l.kind === "FOOD" ? `F:${l.id}:${l.size}` : `C:${l.id}`;
      const unit = Number(quotes[key]?.unitPrice ?? 0);
      return s + unit * (l.qty ?? 0);
    }, 0);
  }, [cartLines, quotes]);

  const refreshQuotes = useCallback(async () => {
    const pending = [];
    for (const l of cartLines) {
      if (l.kind === "FOOD") {
        const key = `F:${l.id}:${l.size}`;
        if (!quotes[key]) pending.push({ key, kind: "FOOD", id: l.id, size: l.size });
      } else {
        const key = `C:${l.id}`;
        if (!quotes[key]) pending.push({ key, kind: "COMBO", id: l.id });
      }
    }
    if (pending.length === 0) return;
    try {
      const results = await Promise.all(
        pending.map(async (p) => {
          const q = p.kind === "FOOD" ? await fetchFoodPrice(p.id, p.size) : await fetchComboPrice(p.id);
          return [p.key, q];
        })
      );
      setQuotes((prev) => Object.fromEntries([...Object.entries(prev), ...results]));
    } catch {
      // ignore quote errors; order submission will show the real error
    }
  }, [cartLines, quotes]);

  useEffect(() => {
    refreshQuotes();
  }, [refreshQuotes]);

  function setQty(id, delta) {
    setQuantities((prev) => ({
      ...prev,
      [id]: Math.max(0, (prev[id] ?? 0) + delta),
    }));
  }

  function setSize(id, size) {
    setSizes((prev) => ({ ...prev, [id]: size }));
    setQuotes((prev) => {
      const next = { ...prev };
      Object.keys(next)
        .filter((k) => k.startsWith(`F:${id}:`))
        .forEach((k) => delete next[k]);
      return next;
    });
  }

  function setComboQuantity(id, delta) {
    setComboQty((prev) => ({
      ...prev,
      [id]: Math.max(0, (prev[id] ?? 0) + delta),
    }));
  }

  const openFoodModal = useCallback(
    async (id) => {
      setFoodModalId(id);
      setFoodModal(null);
      setReviews([]);
      setFoodModalLoading(true);
      setReviewsLoading(true);
      try {
        const [food, revs] = await Promise.all([apiJson(`/menu/foods/${id}`), apiJson(`/menu/foods/${id}/reviews`)]);
        setFoodModal(food);
        setReviews(revs);
      } catch (e) {
        showToast("error", e.message || "Không tải được chi tiết món");
        setFoodModalId(null);
      } finally {
        setFoodModalLoading(false);
        setReviewsLoading(false);
      }
    },
    [showToast]
  );

  async function submitReview() {
    if (!foodModalId) return;
    try {
      await apiJson(`/menu/foods/${foodModalId}/review`, { method: "POST", body: reviewDraft });
      showToast("success", "Đã gửi review.");
      setReviewDraft((p) => ({ ...p, comment: "" }));
      const [food, revs] = await Promise.all([
        apiJson(`/menu/foods/${foodModalId}`),
        apiJson(`/menu/foods/${foodModalId}/reviews`),
      ]);
      setFoodModal(food);
      setReviews(revs);
      await loadMenu();
    } catch (e) {
      showToast("error", e.message || "Gửi review thất bại");
    }
  }

  async function submitOrder() {
    const lines = [];
    Object.entries(quantities)
      .filter(([, q]) => q > 0)
      .forEach(([menuItemId, quantity]) => {
        lines.push({ menuItemId: Number(menuItemId), quantity, size: sizes[Number(menuItemId)] || "M" });
      });
    Object.entries(comboQty)
      .filter(([, q]) => q > 0)
      .forEach(([comboId, quantity]) => {
        lines.push({ comboId: Number(comboId), quantity });
      });
    if (lines.length === 0) {
      showToast("error", "Chọn ít nhất một món.");
      return;
    }
    try {
      const created = await apiJson("/orders", {
        method: "POST",
        body: { customerName, lines },
      });
      showToast("success", `Đặt hàng thành công — Đơn #${created.id} (${Number(created.totalAmount).toLocaleString("vi-VN")} ₫)`);
      setQuantities((prev) => {
        const next = { ...prev };
        foods.forEach((f) => {
          next[f.id] = 0;
        });
        return next;
      });
      setComboQty((prev) => {
        const next = { ...prev };
        combos.forEach((c) => {
          next[c.id] = 0;
        });
        return next;
      });
      await loadOrders();
      setTab("orders");
      setExpandedId(created.id);
      await refreshDetail(created.id);
    } catch (e) {
      showToast("error", e.message || "Đặt hàng thất bại");
    }
  }

  function removeCartLine(line) {
    if (line.kind === "FOOD") {
      setQuantities((prev) => ({ ...prev, [line.id]: 0 }));
      setQuotes((prev) => {
        const next = { ...prev };
        Object.keys(next)
          .filter((k) => k.startsWith(`F:${line.id}:`))
          .forEach((k) => delete next[k]);
        return next;
      });
      return;
    }
    setComboQty((prev) => ({ ...prev, [line.id]: 0 }));
    setQuotes((prev) => {
      const next = { ...prev };
      delete next[`C:${line.id}`];
      return next;
    });
  }

  async function mockPay(orderId, amount) {
    try {
      await apiJson("/delivery/payments/mock", {
        method: "POST",
        body: { orderId, amount: Number(amount) },
      });
      showToast("success", "Thanh toán mock thành công.");
      if (expandedId === orderId) await refreshDetail(orderId);
      await loadOrders();
    } catch (e) {
      showToast("error", e.message || "Thanh toán thất bại");
    }
  }

  async function startDelivery(orderId) {
    try {
      await apiJson("/delivery/deliveries", {
        method: "POST",
        body: { orderId },
      });
      showToast("success", "Đã tạo giao hàng — theo dõi mã vận đơn bên dưới.");
      if (expandedId === orderId) await refreshDetail(orderId);
    } catch (e) {
      showToast("error", e.message || "Không tạo được giao hàng");
    }
  }

  async function patchDeliveryStatus(deliveryId, status, orderId) {
    try {
      await apiJson(`/delivery/deliveries/${deliveryId}/status`, {
        method: "PATCH",
        body: { status },
      });
      showToast("info", `Trạng thái giao hàng: ${DELIVERY_STATUS_VI[status] || status}`);
      await refreshDetail(orderId);
    } catch (e) {
      showToast("error", e.message || "Cập nhật thất bại");
    }
  }

  async function patchOrderStatus(orderId, status) {
    try {
      await apiJson(`/orders/${orderId}/status`, {
        method: "PATCH",
        body: { status },
      });
      showToast("info", `Trạng thái đơn: ${ORDER_STATUS_VI[status] || status}`);
      await loadOrders();
      if (expandedId === orderId) await refreshDetail(orderId);
    } catch (e) {
      showToast("error", e.message || "Cập nhật đơn thất bại");
    }
  }

  async function deleteOrderItem(orderId, itemId) {
    try {
      await apiJson(`/orders/${orderId}/items/${itemId}`, { method: "DELETE" });
      showToast("info", "Đã xoá món khỏi đơn.");
      await loadOrders();
      if (expandedId === orderId) await refreshDetail(orderId);
    } catch (e) {
      showToast("error", e.message || "Không xoá được món");
    }
  }

  if (menuLoading && tab === "menu") {
    return (
      <div className="app loading-screen">
        <div>
          <div className="spinner" />
          <p>Đang tải menu…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="brand">
          <div className="brand-mark" aria-hidden>
            🍽
          </div>
          <div>
            <h1>FoodOrder</h1>
            <p>Đặt món · Thanh toán · Giao hàng</p>
          </div>
        </div>
        <div className="api-pill" title="Gateway URL">
          {apiBase}
        </div>
      </header>

      {toast ? (
        <div className={`toast toast-${toast.type}`} role="status">
          {toast.text}
        </div>
      ) : null}

      <nav className="tabs" aria-label="Điều hướng">
        <button type="button" className={`tab ${tab === "menu" ? "tab-active" : ""}`} onClick={() => setTab("menu")}>
          Thực đơn
        </button>
        <button
          type="button"
          className={`tab ${tab === "orders" ? "tab-active" : ""}`}
          onClick={() => setTab("orders")}
        >
          Đơn hàng của tôi
        </button>
      </nav>

      {tab === "menu" && (
        <>
          <div className="grid-menu">
            {foods.map((f) => (
              <article key={f.id} className="card-food">
                <span className="cat">{f.categoryName}</span>
                <h3 style={{ display: "flex", gap: "0.5rem", alignItems: "center", justifyContent: "space-between" }}>
                  <span>{f.name}</span>
                  <button type="button" className="btn btn-ghost" onClick={() => openFoodModal(f.id)}>
                    Chi tiết
                  </button>
                </h3>
                <p className="desc">{f.description || "Món ngon mỗi ngày"}</p>
                <div className="order-meta" style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
                  <span>
                    ⭐ {f.averageRating ? Number(f.averageRating).toFixed(1) : "—"} ({f.reviewCount ?? 0})
                  </span>
                  <span className="order-meta">Size</span>
                  <select value={sizes[f.id] || "M"} onChange={(e) => setSize(f.id, e.target.value)}>
                    {SIZE_OPTIONS.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="card-food-footer">
                  <span className="price">
                    {(() => {
                      const key = `F:${f.id}:${sizes[f.id] || "M"}`;
                      const unit = quotes[key]?.unitPrice;
                      return unit !== undefined ? `${Number(unit).toLocaleString("vi-VN")} ₫` : `${Number(f.price).toLocaleString("vi-VN")} ₫`;
                    })()}
                  </span>
                  <div className="qty-control">
                    <button type="button" className="qty-btn" onClick={() => setQty(f.id, -1)} aria-label="Giảm">
                      −
                    </button>
                    <span className="qty-val">{quantities[f.id] ?? 0}</span>
                    <button type="button" className="qty-btn" onClick={() => setQty(f.id, 1)} aria-label="Tăng">
                      +
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <section className="panel-checkout" style={{ marginTop: "1rem" }}>
            <h2>Combo / Bundle</h2>
            {combos.length === 0 ? (
              <p className="order-meta">Chưa có combo.</p>
            ) : (
              <div className="orders-grid" style={{ marginTop: "0.75rem" }}>
                {combos.map((c) => (
                  <div key={c.id} className="order-card">
                    <div className="order-card-head" style={{ cursor: "default" }}>
                      <div>
                        <div className="order-id">{c.name}</div>
                        <div className="order-meta">{c.description || "Combo ưu đãi"}</div>
                        <div className="order-meta" style={{ marginTop: "0.35rem" }}>
                          {c.items?.map((it) => `${it.foodName} × ${it.quantity}`).join(" · ")}
                        </div>
                      </div>
                      <div style={{ textAlign: "right" }}>
                        <div style={{ fontWeight: 800 }}>
                          {(() => {
                            const key = `C:${c.id}`;
                            const unit = quotes[key]?.unitPrice;
                            return unit !== undefined
                              ? `${Number(unit).toLocaleString("vi-VN")} ₫`
                              : `${Number(c.bundlePrice).toLocaleString("vi-VN")} ₫`;
                          })()}
                        </div>
                        <div className="qty-control" style={{ marginTop: "0.5rem", justifyContent: "flex-end" }}>
                          <button type="button" className="qty-btn" onClick={() => setComboQuantity(c.id, -1)} aria-label="Giảm">
                            −
                          </button>
                          <span className="qty-val">{comboQty[c.id] ?? 0}</span>
                          <button type="button" className="qty-btn" onClick={() => setComboQuantity(c.id, 1)} aria-label="Tăng">
                            +
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="panel-checkout">
            <h2>Giỏ hàng & đặt món</h2>
            <div className="field">
              <label htmlFor="cust">Tên khách</label>
              <input
                id="cust"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder="Nhập tên hiển thị trên đơn"
              />
            </div>
            {cartLines.length === 0 ? (
              <p className="order-meta">Chưa chọn món nào.</p>
            ) : (
              <>
                {cartLines.map((l) => {
                  const key = l.kind === "FOOD" ? `F:${l.id}:${l.size}` : `C:${l.id}`;
                  const unit = quotes[key]?.unitPrice;
                  const lineTotal = unit !== undefined ? Number(unit) * l.qty : 0;
                  return (
                    <div key={key} className="cart-line">
                      <span>
                        {l.kind === "FOOD" ? `${l.name} (${l.size})` : `${l.name} (combo)`} × {l.qty}
                      </span>
                      <span className="cart-line-right">
                        <span>{lineTotal ? `${lineTotal.toLocaleString("vi-VN")} ₫` : "—"}</span>
                        <button type="button" className="icon-btn" onClick={() => removeCartLine(l)} aria-label="Xoá khỏi giỏ">
                          ×
                        </button>
                      </span>
                    </div>
                  );
                })}
                <div className="cart-total">
                  <span>Tạm tính</span>
                  <span>{cartTotal.toLocaleString("vi-VN")} ₫</span>
                </div>
              </>
            )}
            <div className="btn-row">
              <button type="button" className="btn btn-primary" onClick={submitOrder} disabled={cartLines.length === 0}>
                Đặt hàng
              </button>
              <button type="button" className="btn btn-secondary" onClick={loadMenu}>
                Làm mới menu
              </button>
            </div>
          </section>
        </>
      )}

      {tab === "orders" && (
        <>
          <div className="refresh-bar">
            <p className="order-meta" style={{ margin: 0 }}>
              {ordersLoading ? "Đang tải…" : `${orders.length} đơn hàng`}
            </p>
            <button type="button" className="btn btn-secondary" onClick={loadOrders} disabled={ordersLoading}>
              Làm mới danh sách
            </button>
          </div>

          {orders.length === 0 && !ordersLoading ? (
            <div className="empty-state">
              <p>Chưa có đơn nào. Quay lại tab Thực đơn để đặt món.</p>
            </div>
          ) : (
            <div className="orders-grid">
              {orders.map((o) => (
                <div key={o.id} className="order-card">
                  <button type="button" className="order-card-head" onClick={() => toggleExpand(o.id)}>
                    <div>
                      <div className="order-id">Đơn #{o.id}</div>
                      <div className="order-meta">
                        {o.customerName} · {new Date(o.createdAt).toLocaleString("vi-VN")}
                      </div>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <span className="badge badge-order">{ORDER_STATUS_VI[o.status] || o.status}</span>
                      <div style={{ marginTop: "0.35rem", fontWeight: 700 }}>
                        {Number(o.totalAmount).toLocaleString("vi-VN")} ₫
                      </div>
                    </div>
                  </button>

                  {expandedId === o.id && (
                    <div className="order-body">
                      {detailLoading && !detail ? (
                        <p className="order-meta">Đang tải chi tiết…</p>
                      ) : detail?.order?.id === o.id ? (
                        <>
                          <div className="section-title">Món trong đơn</div>
                          <ul className="order-items" style={{ paddingLeft: "1.1rem", margin: 0 }}>
                            {detail.order.items.map((it) => (
                              <li key={it.id}>
                                {it.menuItemName}
                                {it.itemKind === "FOOD" && it.size ? ` (${it.size})` : it.itemKind === "COMBO" ? " (combo)" : ""} ×{" "}
                                {it.quantity} —{" "}
                                {Number(it.lineTotal).toLocaleString("vi-VN")} ₫
                                {detail.order.status !== "CANCELLED" && detail.order.status !== "DELIVERED" ? (
                                  <button
                                    type="button"
                                    className="btn btn-ghost"
                                    style={{ marginLeft: "0.5rem" }}
                                    onClick={() => deleteOrderItem(o.id, it.id)}
                                  >
                                    Xoá
                                  </button>
                                ) : null}
                              </li>
                            ))}
                          </ul>

                          <div className="section-title">Thanh toán (delivery-service)</div>
                          {detail.payment ? (
                            <p className="order-meta">
                              <span className="badge badge-pay">{detail.payment.status}</span> ·{" "}
                              {Number(detail.payment.amount).toLocaleString("vi-VN")} ₫ ·{" "}
                              {new Date(detail.payment.createdAt).toLocaleString("vi-VN")}
                            </p>
                          ) : (
                            <div className="btn-row">
                              <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => mockPay(o.id, o.totalAmount)}
                              >
                                Thanh toán mock
                              </button>
                              <span className="order-meta" style={{ alignSelf: "center" }}>
                                Giả lập thanh toán thành công ngay
                              </span>
                            </div>
                          )}

                          <div className="section-title">Giao hàng</div>
                          {detail.delivery ? (
                            <>
                              <p className="order-meta">
                                Mã vận đơn: <strong>{detail.delivery.trackingNumber}</strong>
                              </p>
                              <p className="order-meta">
                                Trạng thái:{" "}
                                <span className="badge badge-wait">
                                  {DELIVERY_STATUS_VI[detail.delivery.status] || detail.delivery.status}
                                </span>
                              </p>
                              <p className="order-meta" style={{ fontSize: "0.78rem" }}>
                                Cập nhật: {new Date(detail.delivery.updatedAt).toLocaleString("vi-VN")}
                              </p>
                              <div className="status-chips">
                                {DELIVERY_FLOW.map((st) => (
                                  <button
                                    key={st}
                                    type="button"
                                    className={`chip ${detail.delivery.status === st ? "chip-active" : ""}`}
                                    onClick={() => patchDeliveryStatus(detail.delivery.id, st, o.id)}
                                  >
                                    {DELIVERY_STATUS_VI[st]}
                                  </button>
                                ))}
                              </div>
                            </>
                          ) : (
                            <div className="btn-row">
                              <button type="button" className="btn btn-ghost" onClick={() => startDelivery(o.id)}>
                                Bắt đầu giao hàng
                              </button>
                            </div>
                          )}

                          <div className="section-title">Luồng trạng thái đơn (demo)</div>
                          <p className="order-meta" style={{ fontSize: "0.78rem", marginBottom: "0.5rem" }}>
                            Mô phỏng bếp / cửa hàng — gọi API order-service
                          </p>
                          <div className="status-chips">
                            {ORDER_FLOW.map((st) => (
                              <button
                                key={st}
                                type="button"
                                className={`chip ${detail.order.status === st ? "chip-active" : ""}`}
                                onClick={() => patchOrderStatus(o.id, st)}
                              >
                                {ORDER_STATUS_VI[st]}
                              </button>
                            ))}
                          </div>
                        </>
                      ) : (
                        <p className="order-meta">Không tải được chi tiết.</p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {foodModalId ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal">
            <div className="modal-head">
              <div>
                <div className="order-id">Chi tiết món</div>
                <div className="order-meta">ID #{foodModalId}</div>
              </div>
              <button type="button" className="btn btn-secondary" onClick={() => setFoodModalId(null)}>
                Đóng
              </button>
            </div>

            {foodModalLoading || !foodModal ? (
              <p className="order-meta">Đang tải…</p>
            ) : (
              <>
                <h3 style={{ marginTop: 0 }}>{foodModal.name}</h3>
                <p className="order-meta">{foodModal.description || "Món ngon mỗi ngày"}</p>
                <p className="order-meta">
                  ⭐ {foodModal.averageRating ? Number(foodModal.averageRating).toFixed(1) : "—"} ({foodModal.reviewCount ?? 0})
                </p>

                <div className="section-title">Giá theo size (dynamic)</div>
                <div className="status-chips">
                  {SIZE_OPTIONS.map((s) => (
                    <button
                      key={s}
                      type="button"
                      className={`chip ${sizes[foodModalId] === s ? "chip-active" : ""}`}
                      onClick={() => setSize(foodModalId, s)}
                    >
                      Size {s}
                    </button>
                  ))}
                </div>
                <p className="order-meta" style={{ marginTop: "0.5rem" }}>
                  {(() => {
                    const key = `F:${foodModalId}:${sizes[foodModalId] || "M"}`;
                    const q = quotes[key];
                    if (!q) return "Chưa có báo giá.";
                    return `Unit: ${Number(q.unitPrice).toLocaleString("vi-VN")} ₫ · Rules: ${(q.appliedRules || []).join(", ")}`;
                  })()}
                </p>

                <div className="section-title">Reviews</div>
                {reviewsLoading ? (
                  <p className="order-meta">Đang tải reviews…</p>
                ) : reviews.length === 0 ? (
                  <p className="order-meta">Chưa có review.</p>
                ) : (
                  <div className="reviews">
                    {reviews.map((r) => (
                      <div key={r.id} className="review-card">
                        <div style={{ display: "flex", justifyContent: "space-between", gap: "0.5rem" }}>
                          <strong>{r.authorName || "Khách"}</strong>
                          <span className="badge badge-wait">⭐ {r.rating}/5</span>
                        </div>
                        <div className="order-meta" style={{ marginTop: "0.25rem" }}>
                          {r.comment || "(Không có nội dung)"}
                        </div>
                        <div className="order-meta" style={{ marginTop: "0.25rem", fontSize: "0.75rem" }}>
                          {new Date(r.createdAt).toLocaleString("vi-VN")}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <div className="section-title">Thêm review</div>
                <div className="field">
                  <label>Rating</label>
                  <select value={reviewDraft.rating} onChange={(e) => setReviewDraft((p) => ({ ...p, rating: Number(e.target.value) }))}>
                    {[5, 4, 3, 2, 1].map((x) => (
                      <option key={x} value={x}>
                        {x}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>Tên</label>
                  <input
                    value={reviewDraft.authorName}
                    onChange={(e) => setReviewDraft((p) => ({ ...p, authorName: e.target.value }))}
                    placeholder="Tên hiển thị"
                  />
                </div>
                <div className="field">
                  <label>Nội dung</label>
                  <textarea
                    rows={3}
                    value={reviewDraft.comment}
                    onChange={(e) => setReviewDraft((p) => ({ ...p, comment: e.target.value }))}
                    placeholder="Viết cảm nhận…"
                  />
                </div>
                <div className="btn-row">
                  <button type="button" className="btn btn-primary" onClick={submitReview}>
                    Gửi review
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
}
