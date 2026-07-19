import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { ApiResponse, AuthResponse } from "@/types/api";

const TOKEN_KEY = "taxflow.accessToken";
const REFRESH_KEY = "taxflow.refreshToken";

export const tokenStore = {
  get access() {
    return localStorage.getItem(TOKEN_KEY);
  },
  get refresh() {
    return localStorage.getItem(REFRESH_KEY);
  },
  set(auth: Pick<AuthResponse, "accessToken" | "refreshToken">) {
    localStorage.setItem(TOKEN_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = tokenStore.access;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.refresh;
  if (!refreshToken) return null;
  try {
    const { data } = await api.post<ApiResponse<AuthResponse>>("/auth/refresh", { refreshToken });
    tokenStore.set(data.data);
    return data.data.accessToken;
  } catch {
    tokenStore.clear();
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._retried && !original.url?.includes("/auth/")) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccessToken();
      const token = await refreshing;
      refreshing = null;
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      }
      window.dispatchEvent(new CustomEvent("taxflow:logout"));
    }
    return Promise.reject(error);
  },
);

/** Extracts the API error message from an unknown error. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as Partial<ApiResponse<unknown>> | undefined;
    if (body?.message) return body.message;
    if (error.response?.status === 429) return "Too many requests, slow down.";
  }
  return "Something went wrong. Please try again.";
}

/** Downloads a binary endpoint as a file. */
export async function downloadFile(url: string, fallbackName: string): Promise<void> {
  const response = await api.get(url, { responseType: "blob" });
  const disposition = response.headers["content-disposition"] as string | undefined;
  const match = disposition?.match(/filename="?([^";]+)"?/);
  const name = match?.[1] ?? fallbackName;
  const blobUrl = URL.createObjectURL(response.data as Blob);
  const anchor = document.createElement("a");
  anchor.href = blobUrl;
  anchor.download = name;
  anchor.click();
  URL.revokeObjectURL(blobUrl);
}
