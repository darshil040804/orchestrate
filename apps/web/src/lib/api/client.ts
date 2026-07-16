export const API_ORIGIN =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  code: string | undefined;

  constructor(status: number, code: string | undefined, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

type ApiFetchOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  /**
   * Opt-in only. On a 401, attempts exactly one de-duplicated (single-flight)
   * POST /api/auth/refresh, then retries the original request once. The backend
   * rotates the refresh token on every use with reuse-detection — replaying an
   * already-rotated token revokes ALL of that user's refresh tokens — so this
   * must never fire more than one concurrent refresh call, and must never be
   * used for the auth-flow endpoints themselves (login/signup/refresh/logout/
   * password-reset), only for calls to endpoints that require a valid session.
   */
  authenticated?: boolean;
};

let refreshPromise: Promise<boolean> | null = null;

function refreshAccessToken(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = fetch(`${API_ORIGIN}/api/auth/refresh`, {
      method: "POST",
      credentials: "include",
    })
      .then((res) => res.ok)
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

async function toApiError(res: Response): Promise<ApiError> {
  let code: string | undefined;
  let message = `Request failed with status ${res.status}`;
  try {
    const data: unknown = await res.json();
    if (data && typeof data === "object") {
      if ("error" in data && typeof data.error === "string") code = data.error;
      if ("message" in data && typeof data.message === "string") message = data.message;
    }
    // GET /api/auth/me returns a plain 401 with no JSON body when unauthenticated
    // (it's not behind the app's uniform {error,message} handler) — no code/message
    // to extract there, callers must not assume every ApiError has a `code`.
  } catch {
    // non-JSON body
  }
  return new ApiError(res.status, code, message);
}

/**
 * Shared fetch wrapper for the backend API. Always sends credentials (auth lives
 * in httpOnly cookies, never in JS-readable state) and always JSON-encodes a
 * provided body. Throws ApiError on any non-2xx response.
 */
export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const { authenticated, body, headers, ...rest } = options;

  const doFetch = () =>
    fetch(`${API_ORIGIN}${path}`, {
      ...rest,
      credentials: "include",
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

  let res = await doFetch();

  if (res.status === 401 && authenticated) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      res = await doFetch();
    }
  }

  if (!res.ok) {
    throw await toApiError(res);
  }

  // Any 2xx can come back with no body (e.g. 201 Created from a ResponseEntity<Void>,
  // not just 204 No Content) — parse only if there's actually content to parse.
  const text = await res.text();
  if (text.length === 0) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}
