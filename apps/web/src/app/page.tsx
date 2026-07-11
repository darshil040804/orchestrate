"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

type HealthResponse = {
  status: string;
};

export default function Home() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    fetch(`${apiUrl}/api/health`)
      .then((res) => {
        if (!res.ok) throw new Error(`Request failed: ${res.status}`);
        return res.json() as Promise<HealthResponse>;
      })
      .then(setHealth)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-24">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Orchestrate Backend Health</CardTitle>
        </CardHeader>
        <CardContent>
          {error && <p className="text-red-500">Error: {error}</p>}
          {!error && !health && <p>Loading…</p>}
          {health && <p>status: {health.status}</p>}
        </CardContent>
      </Card>
    </main>
  );
}
