import { apiJson, apiFetch } from "./api.js";

const $ = (sel) => document.querySelector(sel);

let flashHideTimer = null;

function showFlash(message, kind = "info", durationMs = 6000) {
  const el = $("#global-flash");
  if (flashHideTimer) {
    clearTimeout(flashHideTimer);
    flashHideTimer = null;
  }
  el.textContent = message;
  el.className = `flash ${kind}`;
  el.classList.remove("hidden");
  if (kind === "ok" || kind === "info") {
    flashHideTimer = setTimeout(() => el.classList.add("hidden"), durationMs);
  }
}

function hideCheckoutSuccessPanel() {
  const p = document.getElementById("checkout-success-panel");
  if (p) p.classList.add("hidden");
}

function tryBrowserBookingNotification(data) {
  if (typeof Notification === "undefined") return;
  if (Notification.permission !== "granted") return;
  const code = data.bookingCode || "";
  const movie = data.showtime?.movieTitle || "Suất chiếu";
  try {
    new Notification("Đặt vé thành công", {
      body: `Mã đặt chỗ: ${code}\n${movie}`,
      tag: "cinema-booking-success",
    });
  } catch (_) {
    /* ignore */
  }
}

function showCheckoutSuccessPanel(data) {
  const panel = document.getElementById("checkout-success-panel");
  if (!panel) return;
  const codeEl = document.getElementById("success-booking-code");
  const movieEl = document.getElementById("success-movie-title");
  const totalEl = document.getElementById("success-total");
  const emailEl = document.getElementById("success-email-line");
  const seatsEl = document.getElementById("success-seats-line");
  if (codeEl) codeEl.textContent = data.bookingCode || "—";
  if (movieEl) movieEl.textContent = data.showtime?.movieTitle || "—";
  if (totalEl) totalEl.textContent = fmtMoney(data.totalAmount);
  const email = data.customer?.email || "";
  if (emailEl) {
    emailEl.innerHTML =
      email ? `Xác nhận đã gửi tới <strong>${escapeHtml(email)}</strong>.` : "—";
  }
  const tickets = data.tickets || [];
  if (seatsEl) {
    seatsEl.textContent =
      tickets.length > 0 ?
        "Ghế: " + tickets.map((t) => `${t.seatCode} (${t.ticketCode})`).join(", ")
      : "—";
  }
  panel.classList.remove("hidden");
  panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
  tryBrowserBookingNotification(data);
}

function hideFlash() {
  $("#global-flash").classList.add("hidden");
}

async function safeApi(fn) {
  try {
    hideFlash();
    return await fn();
  } catch (e) {
    const msg = e.body?.message || e.message || String(e);
    showFlash(msg + (e.status ? ` (HTTP ${e.status})` : ""), "err");
    throw e;
  }
}

/* —— Booking state —— */
const booking = {
  movie: null,
  showtime: null,
  seatMap: [],
  selectedIds: new Set(),
  hold: null,
};

function fmtMoney(n) {
  if (n == null) return "—";
  const x = Number(n);
  return new Intl.NumberFormat("vi-VN").format(x) + " ₫";
}

function fmtDt(iso) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("vi-VN");
  } catch {
    return iso;
  }
}

let holdCountdownInterval = null;

function stopHoldCountdown() {
  if (holdCountdownInterval) {
    clearInterval(holdCountdownInterval);
    holdCountdownInterval = null;
  }
  const banner = document.getElementById("hold-checkout-banner");
  if (banner) {
    banner.classList.add("hidden");
    banner.textContent = "";
    banner.classList.remove("hold-urgent", "hold-expired");
  }
  const checkout = document.getElementById("btn-checkout");
  if (checkout) checkout.disabled = false;
}

function parseExpiresAt(raw) {
  if (raw == null) return null;
  let s = String(raw).trim();
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+/.test(s)) {
    s = s.replace(/(\.\d{3})\d+/, "$1");
  }
  const ms = Date.parse(s);
  return Number.isNaN(ms) ? null : new Date(ms);
}

function formatCountdown(remainingMs) {
  if (!Number.isFinite(remainingMs) || remainingMs <= 0) return "0:00";
  const sec = Math.floor(remainingMs / 1000);
  const m = Math.floor(sec / 60);
  const r = sec % 60;
  return `${m}:${String(r).padStart(2, "0")}`;
}

function startHoldCountdown(expiresAtRaw) {
  stopHoldCountdown();
  const end = parseExpiresAt(expiresAtRaw);
  const banner = document.getElementById("hold-checkout-banner");
  if (!banner) return;

  const enableCheckout = () => {
    const co = document.getElementById("btn-checkout");
    if (co) co.disabled = false;
  };

  if (!end || !Number.isFinite(end.getTime())) {
    banner.classList.remove("hidden", "hold-expired", "hold-urgent");
    banner.innerHTML =
      "<span>Giữ chỗ đang hiệu lực — hoàn tất thanh toán bên dưới.</span>";
    enableCheckout();
    return;
  }

  const tick = () => {
    if (!booking.hold) {
      stopHoldCountdown();
      return;
    }
    const rem = end.getTime() - Date.now();
    banner.classList.remove("hidden", "hold-urgent", "hold-expired");

    if (!Number.isFinite(rem)) {
      banner.innerHTML = "<span>Bạn có thể thanh toán nếu ghế vẫn đang được giữ.</span>";
      enableCheckout();
      return;
    }

    if (rem <= 0) {
      banner.classList.add("hold-expired");
      banner.innerHTML =
        "<strong>Hết thời giữ ghế.</strong> Quay lại bước chọn ghế và tạo hold mới.";
      const co = document.getElementById("btn-checkout");
      if (co) co.disabled = true;
      if (holdCountdownInterval) {
        clearInterval(holdCountdownInterval);
        holdCountdownInterval = null;
      }
      return;
    }

    if (rem < 60_000) banner.classList.add("hold-urgent");

    const endStr = end.toLocaleString("vi-VN");
    const cd = formatCountdown(rem);
    banner.innerHTML = `<span>Ghế giữ đến <strong>${endStr}</strong></span><span class="hold-ttl">Còn: ${cd}</span>`;
    enableCheckout();
  };

  tick();
  holdCountdownInterval = setInterval(tick, 1000);
}

function updateBookingSteps() {
  const steps = [
    { id: 1, label: "Phim", done: !!booking.movie },
    { id: 2, label: "Suất", done: !!booking.showtime },
    { id: 3, label: "Ghế & hold", done: !!booking.hold },
    { id: 4, label: "Thanh toán", done: false },
  ];
  const cur = !booking.movie ? 1 : !booking.showtime ? 2 : !booking.hold ? 3 : 4;
  $("#booking-steps").innerHTML = steps
    .map((s) => {
      const cl = s.done ? "done" : s.id === cur ? "current" : "";
      return `<span class="step ${cl}">${s.id}. ${s.label}</span>`;
    })
    .join("");
}

function showBookStep(n) {
  ["book-step-1", "book-step-2", "book-step-3", "book-step-4"].forEach((id, i) => {
    document.getElementById(id).classList.toggle("hidden", i + 1 !== n);
  });
}

function groupSeatsByRow(seats) {
  const rows = new Map();
  for (const s of seats) {
    const code = s.seatCode || "?";
    const m = code.match(/^([A-Za-z]+)(\d+)$/);
    const row = m ? m[1] : code.charAt(0);
    if (!rows.has(row)) rows.set(row, []);
    rows.get(row).push(s);
  }
  for (const arr of rows.values()) {
    arr.sort((a, b) => (a.seatCode || "").localeCompare(b.seatCode || "", undefined, { numeric: true }));
  }
  return [...rows.entries()].sort((a, b) => a[0].localeCompare(b[0]));
}

function renderSeatMap() {
  const root = $("#seat-map-root");
  root.innerHTML = "";
  const grouped = groupSeatsByRow(booking.seatMap);
  for (const [row, list] of grouped) {
    const rowEl = document.createElement("div");
    rowEl.className = "seat-row";
    const rl = document.createElement("div");
    rl.className = "row-label";
    rl.textContent = row;
    rowEl.appendChild(rl);
    for (const s of list) {
      const st = (s.status || "").toUpperCase();
      const div = document.createElement("div");
      div.className = "seat";
      div.dataset.seatId = String(s.seatId);
      div.title = `${s.seatCode} · ${st} · ${fmtMoney(s.price)}`;
      div.textContent = (s.seatCode || "").replace(/^[A-Z]+/i, "");
      if (st === "FREE") {
        div.classList.add("free");
        if (booking.selectedIds.has(s.seatId)) div.classList.add("selected");
        div.addEventListener("click", () => toggleSeat(s));
      } else if (st === "HELD") {
        div.classList.add("held");
      } else {
        div.classList.add("booked");
      }
      rowEl.appendChild(div);
    }
    root.appendChild(rowEl);
  }
  refreshSeatSummary();
}

function toggleSeat(s) {
  if ((s.status || "").toUpperCase() !== "FREE") return;
  const id = s.seatId;
  if (booking.selectedIds.has(id)) booking.selectedIds.delete(id);
  else booking.selectedIds.add(id);
  booking.hold = null;
  stopHoldCountdown();
  showBookStep(3);
  renderSeatMap();
  $("#btn-create-hold").disabled = booking.selectedIds.size === 0;
}

function refreshSeatSummary() {
  const ids = [...booking.selectedIds];
  const seats = booking.seatMap.filter((x) => ids.includes(x.seatId));
  const sum = seats.reduce((a, s) => a + Number(s.price || 0), 0);
  $("#seat-selection-summary").textContent =
    ids.length === 0 ?
      "Chưa chọn ghế."
    : `Đã chọn ${ids.length} ghế: ${seats.map((s) => s.seatCode).join(", ")} · Tổng ${fmtMoney(sum)}`;
  $("#co-total").value = sum > 0 ? String(sum) : "";
}

async function loadMoviesForBooking() {
  const data = await apiJson("/movies?status=" + encodeURIComponent("ACTIVE"));
  const grid = $("#movie-pick-grid");
  grid.innerHTML = "";
  (data || []).forEach((m) => {
    const card = document.createElement("div");
    card.className = "movie-card";
    card.innerHTML = `
      <h4>${escapeHtml(m.title)}</h4>
      <div class="meta">${escapeHtml(m.genre || "")} · ${m.durationMinutes || "?"} phút · ★ ${m.rating ?? "—"}</div>
      <p class="hint" style="margin:0.5rem 0 0;font-size:0.8rem">${escapeHtml((m.description || "").slice(0, 120))}${(m.description || "").length > 120 ? "…" : ""}</p>
    `;
    card.addEventListener("click", () => selectMovie(m));
    grid.appendChild(card);
  });
  if (!data || data.length === 0) {
    grid.innerHTML = "<p>Không có phim đang chiếu.</p>";
  }
}

function escapeHtml(s) {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

async function selectMovie(m) {
  booking.movie = m;
  booking.showtime = null;
  booking.seatMap = [];
  booking.selectedIds.clear();
  booking.hold = null;
  stopHoldCountdown();
  hideCheckoutSuccessPanel();
  updateBookingSteps();
  const sts = await apiJson(`/movies/${m.id}/showtimes`);
  $("#book-movie-title").textContent = `Phim: ${m.title}`;
  const list = $("#showtime-pick-list");
  list.innerHTML = "";
  (sts || []).forEach((st) => {
    const row = document.createElement("div");
    row.className = "showtime-item";
    row.innerHTML = `
      <div>
        <strong>${fmtDt(st.startTime)}</strong> — ${escapeHtml(st.cinemaName || "")} · ${escapeHtml(st.auditoriumName || "")}
        <div class="hint">Giá từ ${fmtMoney(st.basePrice)}</div>
      </div>
    `;
    const go = document.createElement("button");
    go.type = "button";
    go.className = "btn";
    go.textContent = "Chọn suất này";
    go.addEventListener("click", () => selectShowtime(st));
    row.appendChild(go);
    list.appendChild(row);
  });
  if (!sts || sts.length === 0) {
    list.innerHTML = "<p>Chưa có suất chiếu cho phim này.</p>";
  }
  showBookStep(2);
}

async function selectShowtime(st) {
  booking.showtime = st;
  booking.seatMap = [];
  booking.selectedIds.clear();
  booking.hold = null;
  stopHoldCountdown();
  hideCheckoutSuccessPanel();
  updateBookingSteps();
  $("#book-showtime-detail").textContent = `${st.movieTitle || ""} · ${fmtDt(st.startTime)} · ${st.cinemaName} — ${st.auditoriumName}`;
  const map = await apiJson(`/showtimes/${st.id}/seats`);
  booking.seatMap = map.seats || [];
  renderSeatMap();
  $("#btn-create-hold").disabled = true;
  showBookStep(3);
}

async function initBookingUi() {
  $("#btn-reload-movies").addEventListener("click", () => safeApi(() => loadMoviesForBooking()));
  $("#btn-back-movies").addEventListener("click", () => {
    booking.showtime = null;
    booking.seatMap = [];
    booking.selectedIds.clear();
    booking.hold = null;
    stopHoldCountdown();
    hideCheckoutSuccessPanel();
    updateBookingSteps();
    showBookStep(1);
  });

  $("#btn-create-hold").addEventListener("click", async () => {
    await safeApi(async () => {
      const st = booking.showtime;
      if (!st || booking.selectedIds.size === 0) return;
      const hold = await apiJson("/holds", {
        method: "POST",
        body: JSON.stringify({
          showtimeId: st.id,
          seatIds: [...booking.selectedIds],
        }),
      });
      booking.hold = hold;
      const sumHold = (hold.seats || []).reduce((a, s) => a + Number(s.price || 0), 0);
      if (sumHold > 0) $("#co-total").value = String(sumHold);
      updateBookingSteps();
      showBookStep(4);
      startHoldCountdown(hold.expiresAt);
      showFlash("Đã giữ ghế — thanh toán trong thời gian hiển thị bên dưới.", "ok");
      const map = await apiJson(`/showtimes/${st.id}/seats`);
      booking.seatMap = map.seats || [];
      renderSeatMap();
    });
  });

  $("#btn-checkout").addEventListener("click", async () => {
    await safeApi(async () => {
      const st = booking.showtime;
      const hold = booking.hold;
      if (!st || !hold) {
        showFlash("Chưa có suất chiếu hoặc ghế chưa được giữ.", "err");
        return;
      }
      const total = $("#co-total").value.trim();
      const payload = {
        holdId: hold.holdId,
        showtimeId: st.id,
        customer: {
          fullName: $("#co-name").value.trim(),
          email: $("#co-email").value.trim(),
          phone: $("#co-phone").value.trim(),
        },
        payment: {
          method: $("#co-pay-method").value,
          cardNumber: $("#co-card").value.trim() || undefined,
          cardHolderName: $("#co-cardholder").value.trim(),
          totalAmount: Number(total),
        },
      };
      const idem = crypto.randomUUID();
      const { res, body: resp } = await apiFetch("/bookings/checkout", {
        method: "POST",
        headers: { "X-Idempotency-Key": idem },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const err = new Error(resp?.message || "Checkout failed");
        err.status = res.status;
        err.body = resp;
        throw err;
      }
      const data = resp?.data ?? resp;
      stopHoldCountdown();
      showFlash("Đặt vé thành công.", "ok", 14000);
      showCheckoutSuccessPanel(data);
    });
  });

  document.getElementById("btn-checkout-success-close")?.addEventListener("click", hideCheckoutSuccessPanel);

  updateBookingSteps();
  loadMoviesForBooking().catch((e) => showFlash(e.message, "err"));
}

function init() {
  initBookingUi();
}

init();
