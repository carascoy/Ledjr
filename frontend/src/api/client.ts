// every call to the backend goes through api(). it handles the login stuff in one spot:
//  - credentials: "include" so the browser sends the session cookie
//  - the CSRF token header on POST/PUT/DELETE (spring blocks writes without it)
//  - a 401 anywhere = session expired, tells the app you're signed out

// where the backend lives. set VITE_API_URL on the server (see frontend/.env.example),
// falls back to localhost for dev. vite bakes this in at build time, so rebuild after changing it
export const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

type Csrf = { headerName: string; token: string };
let csrf: Csrf | null = null;

async function loadCsrf(): Promise<Csrf> {
  const response = await fetch(`${API_URL}/csrf`, { credentials: "include" });
  if (!response.ok) throw new Error(`Couldn't get a CSRF token (${response.status})`);
  const data: Csrf = await response.json();
  csrf = { headerName: data.headerName, token: data.token };
  return csrf;
}

// signing in/out swaps the session, and the CSRF token lives in the session, so drop the old one
export function resetCsrf() {
  csrf = null;
}

// same as fetch() but takes a path like "/debts" instead of the full url
export async function api(path: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? "GET").toUpperCase();
  const isWrite = method !== "GET" && method !== "HEAD";

  const send = async () => {
    const headers = new Headers(init.headers);
    if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (isWrite) {
      const token = csrf ?? (await loadCsrf());
      headers.set(token.headerName, token.token);
    }
    return fetch(`${API_URL}${path}`, { ...init, headers, credentials: "include" });
  };

  let response = await send();
  // 403 on a write is usually a stale CSRF token (the session changed). get a fresh one, try once more
  if (response.status === 403 && isWrite) {
    resetCsrf();
    response = await send();
  }
  if (response.status === 401) {
    window.dispatchEvent(new Event(SIGNED_OUT_EVENT));
  }
  return response;
}

// UserProvider listens for this and clears the user
export const SIGNED_OUT_EVENT = "ledjr:signed-out";
