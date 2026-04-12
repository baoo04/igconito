/**
 * Gọi API Gateway — mọi service đều bọc { data, message, status }.
 *
 * Mặc định Docker: cùng origin + /api (nginx proxy → gateway), tránh CORS và "Failed to fetch".
 * Gọi trực tiếp :8080 từ trình duyệt thường bị chặn cross-origin dù gateway có CORS.
 */
function migrateLegacyApiBase() {
  try {
    const v = localStorage.getItem("cinema_api_base");
    if (!v) return;
    if (/^https?:\/\/(localhost|127\.0\.0\.1):8080\/?$/i.test(v)) {
      localStorage.removeItem("cinema_api_base");
    }
  } catch (_) {
    /* ignore */
  }
}

function getApiBase() {
  migrateLegacyApiBase();
  const saved = localStorage.getItem("cinema_api_base");
  if (saved) return saved.replace(/\/$/, "");
  if (typeof window !== "undefined" && window.location?.protocol && window.location.protocol !== "file:") {
    return `${window.location.origin}/api`;
  }
  const host = window.location.hostname || "localhost";
  return `http://${host}:8080`;
}

function setApiBase(url) {
  const u = url.trim().replace(/\/$/, "");
  localStorage.setItem("cinema_api_base", u);
  return u;
}

async function apiFetch(path, options = {}) {
  const base = getApiBase();
  const url = path.startsWith("http") ? path : `${base}${path.startsWith("/") ? "" : "/"}${path}`;
  // Chỉ gửi Content-Type khi có body JSON — GET + Content-Type gây CORS preflight không cần thiết.
  const hasBody =
    options.body != null &&
    (typeof options.body === "string" ? options.body.length > 0 : true) &&
    !(typeof FormData !== "undefined" && options.body instanceof FormData);
  const headers = {
    Accept: "application/json",
    ...options.headers,
  };
  if (hasBody && !headers["Content-Type"] && !headers["content-type"]) {
    headers["Content-Type"] = "application/json";
  }
  const res = await fetch(url, { ...options, headers });
  const text = await res.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = { raw: text };
    }
  }
  return { res, body };
}

/** Trích data từ ApiResponse hoặc trả nguyên body */
function unwrapData(body) {
  if (body && typeof body === "object" && "data" in body) return body.data;
  return body;
}

async function apiJson(path, options = {}) {
  const { res, body } = await apiFetch(path, options);
  if (!res.ok) {
    const err = new Error(body?.message || res.statusText || "Request failed");
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return unwrapData(body);
}

export {
  getApiBase,
  setApiBase,
  apiFetch,
  apiJson,
  unwrapData,
};
