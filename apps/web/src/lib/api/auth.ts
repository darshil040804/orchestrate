import { apiFetch } from "@/lib/api/client";

export type UserResponse = {
  id: string;
  email: string;
  emailVerified: boolean;
};

export type MessageResponse = {
  message: string;
};

export function signup(input: { email: string; password: string }) {
  return apiFetch<MessageResponse>("/api/auth/signup", {
    method: "POST",
    body: input,
  });
}

export function verifyEmail(token: string) {
  return apiFetch<MessageResponse>(
    `/api/auth/verify-email?token=${encodeURIComponent(token)}`
  );
}

export function login(input: { email: string; password: string }) {
  return apiFetch<UserResponse>("/api/auth/login", {
    method: "POST",
    body: input,
  });
}

export function logout() {
  return apiFetch<void>("/api/auth/logout", { method: "POST" });
}

export function requestPasswordReset(input: { email: string }) {
  return apiFetch<MessageResponse>("/api/auth/password-reset/request", {
    method: "POST",
    body: input,
  });
}

export function confirmPasswordReset(input: {
  token: string;
  newPassword: string;
}) {
  return apiFetch<MessageResponse>("/api/auth/password-reset/confirm", {
    method: "POST",
    body: input,
  });
}

export function getMe() {
  return apiFetch<UserResponse>("/api/auth/me", { authenticated: true });
}
