export const API_BASE_URL = "http://54.86.192.52:8080";
const TOKEN_KEY = "accessToken";

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  constructor(message, code, status) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

function buildQueryString(query) {
  if (!query) return "";
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") {
      params.append(key, String(value));
    }
  }
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export async function apiRequest(path, options = {}) {
  const { method = "GET", body, query, signal } = options;

  const headers = {};
  const token = getAccessToken();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const res = await fetch(`${API_BASE_URL}${path}${buildQueryString(query)}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal,
  });

  if (res.status === 204) {
    return undefined;
  }

  const text = await res.text();
  let json;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch {
      json = undefined;
    }
  }

  if (!res.ok) {
    throw new ApiError(
      json?.message ?? `요청에 실패했습니다. (${res.status})`,
      json?.code ?? "UNKNOWN_ERROR",
      res.status,
    );
  }

  if (json && typeof json === "object" && "data" in json) {
    if (json.success === false) {
      throw new ApiError(json.message ?? "요청에 실패했습니다.", json.code ?? "UNKNOWN_ERROR", res.status);
    }
    return json.data;
  }

  return json;
}
