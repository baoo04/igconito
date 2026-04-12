import { getApiBase, setApiBase, apiJson, apiFetch } from "./api.js";

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

/** Thông báo hệ điều hành (nếu user đã cho phép). */
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
      email ?
        `Hệ thống đã gửi <strong>email xác nhận (mock)</strong> tới <strong>${escapeHtml(email)}</strong> qua notification-service.`
      : "—";
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

function showJson(preId, data) {
  const pre = document.getElementById(preId);
  if (!pre) return;
  pre.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
  pre.classList.remove("hidden");
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

/* —— Nav —— */
function initNav() {
  $("#api-base-input").value = getApiBase();
  $("#api-save-btn").addEventListener("click", () => {
    setApiBase($("#api-base-input").value);
    $("#api-base-input").value = getApiBase();
    showFlash("Đã lưu API Gateway: " + getApiBase(), "ok");
  });

  document.querySelectorAll("#main-nav button").forEach((btn) => {
    btn.addEventListener("click", () => {
      const view = btn.dataset.view;
      document.querySelectorAll("#main-nav button").forEach((b) => b.classList.toggle("active", b === btn));
      document.querySelectorAll("main > section").forEach((sec) => {
        sec.classList.toggle("hidden", sec.id !== `view-${view}`);
      });
    });
  });
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

/** Đồng hồ đếm ngược theo `expiresAt` từ POST /holds (TTL do server, thường 5 phút). */
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

/**
 * Java LocalDateTime thường serialize nano giây (9 chữ số). Date.parse() của trình duyệt
 * không ổn định với chuỗi đó → Invalid Date / so sánh sai → báo “hết hạn” nhầm. Chuẩn hoá về ms.
 */
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
      "<span>Không hiển thị đếm ngược (định dạng thời gian từ server). Hold vừa tạo vẫn có thể thanh toán bình thường.</span>";
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
      banner.innerHTML =
        "<span>Không đồng bộ đồng hồ đếm ngược. Bạn vẫn có thể thanh toán nếu hold còn ACTIVE trên server.</span>";
      enableCheckout();
      return;
    }

    if (rem <= 0) {
      banner.classList.add("hold-expired");
      banner.innerHTML =
        "<strong>Hold đã hết hạn.</strong> Quay lại bước chọn ghế và bấm «Tạo hold» lần nữa để giữ chỗ mới.";
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
    banner.innerHTML = `<span>Ghế đang được giữ đến <strong>${endStr}</strong></span><span class="hold-ttl">Còn lại: ${cd}</span><span style="display:block;font-size:0.8rem;color:var(--muted);margin-top:0.35rem">Sau thời điểm này hold hết hiệu lực — hoàn tất thanh toán trước khi hết giờ.</span>`;
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
    { id: 4, label: "Checkout", done: false },
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
  $("#hold-result").classList.add("hidden");
  showBookStep(3);
  renderSeatMap();
  $("#btn-create-hold").disabled = booking.selectedIds.size === 0;
  $("#btn-check-availability").disabled = booking.selectedIds.size === 0;
}

function refreshSeatSummary() {
  const ids = [...booking.selectedIds];
  const seats = booking.seatMap.filter((x) => ids.includes(x.seatId));
  const sum = seats.reduce((a, s) => a + Number(s.price || 0), 0);
  $("#seat-selection-summary").textContent =
    ids.length === 0
      ? "Chưa chọn ghế."
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
    grid.innerHTML = "<p>Không có phim ACTIVE.</p>";
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
        <div class="hint">Giá từ ${fmtMoney(st.basePrice)} · ID suất: ${st.id}</div>
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
  $("#hold-result").classList.add("hidden");
  $("#checkout-result").classList.add("hidden");
  renderSeatMap();
  $("#btn-create-hold").disabled = true;
  $("#btn-check-availability").disabled = true;
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

  $("#btn-check-availability").addEventListener("click", async () => {
    await safeApi(async () => {
      const st = booking.showtime;
      if (!st) return;
      await apiJson(`/showtimes/${st.id}/availability`, {
        method: "POST",
        body: JSON.stringify({ seatIds: [...booking.selectedIds] }),
      });
      showFlash("Tất cả ghế đều còn trống (available: true).", "ok");
    });
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
      $("#hold-lookup-id").value = hold.holdId;
      const sumHold = (hold.seats || []).reduce((a, s) => a + Number(s.price || 0), 0);
      if (sumHold > 0) $("#co-total").value = String(sumHold);
      showJson("hold-result", hold);
      updateBookingSteps();
      showBookStep(4);
      startHoldCountdown(hold.expiresAt);
      showFlash("Đã tạo hold — xem thời gian còn lại ở khung phía trên.", "ok");
      const map = await apiJson(`/showtimes/${st.id}/seats`);
      booking.seatMap = map.seats || [];
      renderSeatMap();
    });
  });

  $("#btn-validate-customer").addEventListener("click", async () => {
    await safeApi(async () => {
      const body = {
        fullName: $("#co-name").value.trim(),
        email: $("#co-email").value.trim(),
        phone: $("#co-phone").value.trim(),
      };
      const { res, body: resp } = await apiFetch("/customers/validate", {
        method: "POST",
        body: JSON.stringify(body),
      });
      showJson("checkout-result", resp);
      if (!res.ok) showFlash("Validate thất bại", "err");
      else showFlash("Validate OK", "ok");
    });
  });

  $("#btn-checkout").addEventListener("click", async () => {
    await safeApi(async () => {
      const st = booking.showtime;
      const hold = booking.hold;
      if (!st || !hold) {
        showFlash("Thiếu suất chiếu hoặc hold.", "err");
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
      showJson("checkout-result", resp);
      if (!res.ok) {
        const err = new Error(resp?.message || "Checkout failed");
        err.status = res.status;
        err.body = resp;
        throw err;
      }
      const data = resp?.data ?? resp;
      sessionStorage.setItem("last_booking", JSON.stringify(data));
      stopHoldCountdown();
      showFlash("Đặt vé thành công — xem khung xanh bên dưới.", "ok", 14000);
      showCheckoutSuccessPanel(data);
      $("#lookup-booking-id").value = data.bookingId || "";
    });
  });

  document.getElementById("btn-checkout-success-close")?.addEventListener("click", hideCheckoutSuccessPanel);
  document.getElementById("btn-request-notification-perm")?.addEventListener("click", async () => {
    if (typeof Notification === "undefined") {
      showFlash("Trình duyệt không hỗ trợ Notification API.", "info");
      return;
    }
    const p = await Notification.requestPermission();
    if (p === "granted") showFlash("Đã bật thông báo — lần đặt vé sau sẽ có popup của hệ điều hành.", "ok", 10000);
    else showFlash("Chưa cấp quyền thông báo (hoặc đã chặn).", "info", 8000);
  });

  $("#btn-get-booking").addEventListener("click", async () => {
    const id = $("#lookup-booking-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/bookings/${id}`);
      showJson("booking-read-result", d);
    });
  });
  $("#btn-get-tickets").addEventListener("click", async () => {
    const id = $("#lookup-booking-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/bookings/${id}/tickets`);
      showJson("booking-read-result", d);
    });
  });
  $("#btn-cancel-booking").addEventListener("click", async () => {
    const id = $("#lookup-booking-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/bookings/${id}/cancel`, { method: "POST", body: "{}" });
      showJson("booking-read-result", d);
      showFlash("Đã gửi yêu cầu huỷ.", "ok");
    });
  });

  updateBookingSteps();
  loadMoviesForBooking().catch((e) => showFlash(e.message, "err"));
}

/* —— Catalog —— */
function initCatalog() {
  $("#btn-list-movies-admin").addEventListener("click", async () => {
    await safeApi(async () => {
      const status = $("#movie-filter-status").value;
      const genre = $("#movie-filter-genre").value.trim();
      const params = new URLSearchParams();
      if (status) params.set("status", status);
      if (genre) params.set("genre", genre);
      const path = params.toString() ? `/movies?${params.toString()}` : "/movies";
      const data = await apiJson(path);
      const el = $("#movie-list-admin");
      el.innerHTML =
        "<table class='data'><thead><tr><th>ID</th><th>Tiêu đề</th><th>Genre</th><th>Trạng thái</th></tr></thead><tbody>" +
        (data || [])
          .map(
            (m) =>
              `<tr><td>${m.id}</td><td>${escapeHtml(m.title)}</td><td>${escapeHtml(m.genre || "")}</td><td>${m.status}</td></tr>`
          )
          .join("") +
        "</tbody></table>";
    });
  });

  $("#btn-movie-create").addEventListener("click", async () => {
    await safeApi(async () => {
      const body = {
        title: $("#movie-title").value.trim(),
        description: $("#movie-desc").value.trim() || undefined,
        genre: $("#movie-genre").value.trim() || undefined,
        durationMinutes: numOrUndef("#movie-duration"),
        rating: numOrUndef("#movie-rating"),
        posterUrl: $("#movie-poster").value.trim() || undefined,
        releaseDate: $("#movie-release").value || undefined,
      };
      const r = await apiJson("/movies", { method: "POST", body: JSON.stringify(body) });
      showFlash("Tạo phim OK id=" + r.id, "ok");
    });
  });

  $("#btn-movie-update").addEventListener("click", async () => {
    await safeApi(async () => {
      const id = $("#movie-edit-id").value.trim();
      if (!id) throw Object.assign(new Error("Nhập ID phim"), { body: {} });
      const body = {};
      if ($("#movie-title").value.trim()) body.title = $("#movie-title").value.trim();
      if ($("#movie-desc").value.trim()) body.description = $("#movie-desc").value.trim();
      if ($("#movie-genre").value.trim()) body.genre = $("#movie-genre").value.trim();
      if ($("#movie-duration").value) body.durationMinutes = Number($("#movie-duration").value);
      if ($("#movie-rating").value) body.rating = Number($("#movie-rating").value);
      if ($("#movie-poster").value.trim()) body.posterUrl = $("#movie-poster").value.trim();
      if ($("#movie-release").value) body.releaseDate = $("#movie-release").value;
      if ($("#movie-status").value) body.status = $("#movie-status").value;
      const r = await apiJson(`/movies/${id}`, { method: "PUT", body: JSON.stringify(body) });
      showFlash("Cập nhật phim OK", "ok");
      console.log(r);
    });
  });

  $("#btn-movie-delete").addEventListener("click", async () => {
    await safeApi(async () => {
      const id = $("#movie-edit-id").value.trim();
      if (!id) throw new Error("Nhập ID phim");
      const { res } = await apiFetch(`/movies/${id}`, { method: "DELETE" });
      if (!res.ok && res.status !== 204) {
        const err = new Error("Xoá thất bại");
        err.status = res.status;
        throw err;
      }
      showFlash("Đã xoá phim", "ok");
    });
  });

  $("#btn-list-cinemas").addEventListener("click", async () => {
    await safeApi(async () => {
      const data = await apiJson("/cinemas");
      $("#cinema-list-out").innerHTML =
        "<table class='data'><thead><tr><th>ID</th><th>Tên</th><th>Thành phố</th></tr></thead><tbody>" +
        (data || []).map((c) => `<tr><td>${c.id}</td><td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.city || "")}</td></tr>`).join("") +
        "</tbody></table>";
    });
  });

  $("#btn-cinema-create").addEventListener("click", async () => {
    await safeApi(async () => {
      const body = {
        name: $("#cinema-name").value.trim(),
        address: $("#cinema-address").value.trim() || undefined,
        city: $("#cinema-city").value.trim() || undefined,
        phone: $("#cinema-phone").value.trim() || undefined,
      };
      await apiJson("/cinemas", { method: "POST", body: JSON.stringify(body) });
      showFlash("Tạo rạp OK", "ok");
    });
  });

  $("#btn-list-showtimes").addEventListener("click", async () => {
    await safeApi(async () => {
      const d = $("#st-filter-date").value;
      const c = $("#st-filter-cinema").value.trim();
      let path = "/showtimes";
      const qs = [];
      if (d) qs.push(`date=${encodeURIComponent(d)}`);
      if (c) qs.push(`cinemaId=${encodeURIComponent(c)}`);
      if (qs.length) path += "?" + qs.join("&");
      const data = await apiJson(path);
      $("#showtime-list-out").innerHTML =
        "<table class='data'><thead><tr><th>ID</th><th>Phim</th><th>Bắt đầu</th><th>Rạp</th><th>Auditorium</th><th>Giá</th></tr></thead><tbody>" +
        (data || [])
          .map(
            (s) =>
              `<tr><td>${s.id}</td><td>${escapeHtml(s.movieTitle || "")}</td><td>${fmtDt(s.startTime)}</td><td>${escapeHtml(s.cinemaName || "")}</td><td>${s.auditoriumId} ${escapeHtml(s.auditoriumName || "")}</td><td>${fmtMoney(s.basePrice)}</td></tr>`
          )
          .join("") +
        "</tbody></table>";
    });
  });

  $("#btn-showtime-create").addEventListener("click", async () => {
    await safeApi(async () => {
      const start = $("#st-start").value;
      const end = $("#st-end").value;
      const body = {
        movieId: Number($("#st-movie-id").value),
        auditoriumId: Number($("#st-aud-id").value),
        startTime: toIsoLocal(start),
        endTime: toIsoLocal(end),
        basePrice: Number($("#st-price").value),
      };
      const r = await apiJson("/showtimes", { method: "POST", body: JSON.stringify(body) });
      showFlash("Tạo suất OK id=" + r.id, "ok");
    });
  });

  $("#btn-showtime-get").addEventListener("click", async () => {
    const id = $("#st-edit-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/showtimes/${id}`);
      showJson("showtime-crud-result", d);
    });
  });

  $("#btn-showtime-update").addEventListener("click", async () => {
    const id = $("#st-edit-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const body = {};
      if ($("#st-movie-id").value) body.movieId = Number($("#st-movie-id").value);
      if ($("#st-aud-id").value) body.auditoriumId = Number($("#st-aud-id").value);
      if ($("#st-start").value) body.startTime = toIsoLocal($("#st-start").value);
      if ($("#st-end").value) body.endTime = toIsoLocal($("#st-end").value);
      if ($("#st-price").value) body.basePrice = Number($("#st-price").value);
      const d = await apiJson(`/showtimes/${id}`, { method: "PUT", body: JSON.stringify(body) });
      showJson("showtime-crud-result", d);
      showFlash("Cập nhật suất OK", "ok");
    });
  });

  $("#btn-showtime-delete").addEventListener("click", async () => {
    const id = $("#st-edit-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const { res } = await apiFetch(`/showtimes/${id}`, { method: "DELETE" });
      if (!res.ok && res.status !== 204) throw new Error("Xoá suất thất bại");
      showJson("showtime-crud-result", { deleted: id });
      showFlash("Đã xoá suất", "ok");
    });
  });
}

function numOrUndef(sel) {
  const v = document.querySelector(sel).value;
  return v === "" ? undefined : Number(v);
}

function toIsoLocal(dtLocal) {
  if (!dtLocal) return undefined;
  if (dtLocal.length === 16) return dtLocal + ":00";
  return dtLocal;
}

/* —— Customers —— */
function initCustomers() {
  $("#btn-customer-upsert").addEventListener("click", async () => {
    await safeApi(async () => {
      const body = {
        fullName: $("#cu-name").value.trim(),
        email: $("#cu-email").value.trim(),
        phone: $("#cu-phone").value.trim(),
      };
      const { res, body: resp } = await apiFetch("/customers", {
        method: "POST",
        body: JSON.stringify(body),
      });
      showJson("customer-result", resp);
      if (!res.ok) throw Object.assign(new Error(resp?.message || "upsert failed"), { body: resp, status: res.status });
      showFlash(res.status === 201 ? "Đã tạo khách mới" : "Đã cập nhật khách", "ok");
    });
  });
  $("#btn-customer-lookup").addEventListener("click", async () => {
    await safeApi(async () => {
      const email = $("#cu-lookup-email").value.trim();
      const phone = $("#cu-lookup-phone").value.trim();
      const params = new URLSearchParams();
      if (email) params.set("email", email);
      if (phone) params.set("phone", phone);
      if (!email && !phone) throw new Error("Nhập email hoặc SĐT");
      const { res, body } = await apiFetch(`/customers/lookup?${params.toString()}`);
      showJson("customer-result", body);
      if (!res.ok) throw Object.assign(new Error(body?.message || "Không tìm thấy"), { status: res.status, body });
      showFlash("Tìm thấy khách hàng", "ok");
    });
  });
  $("#btn-customer-validate").addEventListener("click", async () => {
    await safeApi(async () => {
      const body = {
        fullName: $("#cv-name").value.trim(),
        email: $("#cv-email").value.trim(),
        phone: $("#cv-phone").value.trim(),
      };
      const { res, body: resp } = await apiFetch("/customers/validate", {
        method: "POST",
        body: JSON.stringify(body),
      });
      showJson("customer-result", resp);
      if (!res.ok) showFlash("Không hợp lệ", "err");
      else showFlash("Hợp lệ", "ok");
    });
  });
}

/* —— Payments —— */
function initPayments() {
  $("#btn-pay-create").addEventListener("click", async () => {
    await safeApi(async () => {
      const body = {
        bookingReference: $("#pay-ref").value.trim(),
        amount: Number($("#pay-amount").value),
        currency: $("#pay-currency").value.trim() || "VND",
        paymentMethod: $("#pay-method").value,
        cardNumber: $("#pay-card").value.trim() || undefined,
        cardHolderName: $("#pay-holder").value.trim(),
      };
      const { res, body: resp } = await apiFetch("/payments", { method: "POST", body: JSON.stringify(body) });
      showJson("payment-result", resp);
      if (res.status === 402) showFlash("Thanh toán bị từ chối (402)", "err");
      else showFlash("Đã xử lý thanh toán", "ok");
      if (resp?.data?.paymentId) $("#pay-id-lookup").value = resp.data.paymentId;
    });
  });
  $("#btn-pay-get").addEventListener("click", async () => {
    const id = $("#pay-id-lookup").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/payments/${id}`);
      showJson("payment-result", d);
    });
  });
  $("#btn-pay-confirm").addEventListener("click", async () => {
    const id = $("#pay-id-lookup").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/payments/${id}/confirm`, { method: "POST", body: "{}" });
      showJson("payment-result", d);
      showFlash("Đã confirm payment", "ok");
    });
  });
}

/* —— Notifications —— */
function initNotifications() {
  $("#btn-notif-prefill").addEventListener("click", () => {
    const raw = sessionStorage.getItem("last_booking");
    if (!raw) {
      showFlash("Chưa có booking trong session — hãy checkout trước.", "err");
      return;
    }
    const b = JSON.parse(raw);
    $("#nf-booking-id").value = b.bookingId || "";
    $("#nf-booking-code").value = b.bookingCode || "";
    $("#nf-email").value = b.customer?.email || "";
    $("#nf-cust-name").value = b.customer?.fullName || "";
    $("#nf-movie").value = b.showtime?.movieTitle || "";
    $("#nf-cinema").value = b.showtime?.cinemaName || "";
    $("#nf-hall").value = b.showtime?.auditoriumName || "";
    $("#nf-start").value = b.showtime?.startTime || "";
    $("#nf-total").value = b.totalAmount ?? "";
    const tickets = (b.tickets || []).map((t) => ({
      ticketCode: t.ticketCode,
      seatCode: t.seatCode,
      seatType: t.seatType,
      price: t.price,
    }));
    $("#nf-tickets-json").value = JSON.stringify(tickets, null, 2);
    showFlash("Đã điền từ booking gần nhất.", "ok");
  });

  $("#btn-notif-send").addEventListener("click", async () => {
    await safeApi(async () => {
      let tickets;
      try {
        tickets = JSON.parse($("#nf-tickets-json").value || "[]");
      } catch {
        throw new Error("Tickets JSON không hợp lệ");
      }
      const body = {
        bookingId: $("#nf-booking-id").value.trim(),
        bookingCode: $("#nf-booking-code").value.trim(),
        recipientEmail: $("#nf-email").value.trim(),
        customerName: $("#nf-cust-name").value.trim(),
        movieTitle: $("#nf-movie").value.trim(),
        cinemaName: $("#nf-cinema").value.trim(),
        auditoriumName: $("#nf-hall").value.trim(),
        startTime: $("#nf-start").value.trim(),
        tickets,
        totalAmount: Number($("#nf-total").value),
      };
      const { res, body: resp } = await apiFetch("/notifications/booking-confirmations", {
        method: "POST",
        body: JSON.stringify(body),
      });
      showJson("notif-result", resp);
      if (resp?.data?.notificationId) $("#nf-id-get").value = resp.data.notificationId;
      showFlash("Đã gửi (202 Accepted)", "ok");
    });
  });

  $("#btn-notif-get").addEventListener("click", async () => {
    const id = $("#nf-id-get").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/notifications/${id}`);
      showJson("notif-result", d);
    });
  });
}

/* —— Holds —— */
function initHolds() {
  $("#btn-hold-get").addEventListener("click", async () => {
    const id = $("#hold-lookup-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/holds/${id}`);
      showJson("hold-manual-result", d);
    });
  });
  $("#btn-hold-release").addEventListener("click", async () => {
    const id = $("#hold-lookup-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/holds/${id}/release`, { method: "POST", body: "{}" });
      showJson("hold-manual-result", d);
    });
  });
  $("#btn-hold-confirm").addEventListener("click", async () => {
    const id = $("#hold-lookup-id").value.trim();
    if (!id) return;
    await safeApi(async () => {
      const d = await apiJson(`/holds/${id}/confirm`, { method: "POST", body: "{}" });
      showJson("hold-manual-result", d);
    });
  });
}

function init() {
  initNav();
  initBookingUi();
  initCatalog();
  initCustomers();
  initPayments();
  initNotifications();
  initHolds();
}

init();
