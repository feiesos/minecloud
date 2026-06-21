import { tokenStore } from './tokenStore';

const BASE_URL = '/api/v1';

let isRefreshing = false;
let refreshPromise: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.getRefreshToken();
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    if (data.code === 200 && data.data) {
      tokenStore.setTokens(data.data.token, data.data.refreshToken);
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

async function refreshAccessToken(): Promise<boolean> {
  if (isRefreshing && refreshPromise) {
    return refreshPromise;
  }
  isRefreshing = true;
  refreshPromise = tryRefresh().finally(() => {
    isRefreshing = false;
    refreshPromise = null;
  });
  return refreshPromise;
}

export async function apiClient<T = unknown>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const accessToken = tokenStore.getAccessToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }
  if (options.body instanceof FormData) {
    delete headers['Content-Type'];
  }

  let res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (res.status === 401 && accessToken) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      const newToken = tokenStore.getAccessToken();
      headers['Authorization'] = `Bearer ${newToken}`;
      res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
    } else {
      tokenStore.clear();
      window.dispatchEvent(new Event('auth:logout'));
    }
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({ msg: res.statusText }));
    throw new Error(body.msg || `Request failed (${res.status})`);
  }

  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.msg || 'Unknown error');
  }
  return json.data as T;
}

export async function apiUpload<T = unknown>(
  path: string,
  formData: FormData,
): Promise<T> {
  return apiClient<T>(path, {
    method: 'POST',
    body: formData,
  });
}
