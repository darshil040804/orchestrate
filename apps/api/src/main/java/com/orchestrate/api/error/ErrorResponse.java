package com.orchestrate.api.error;

/** Uniform JSON error body: {@code {"error": "CODE", "message": "..."}}. */
public record ErrorResponse(String error, String message) {}
