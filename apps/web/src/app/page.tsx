import { LandingView } from "@/components/auth/landing-view";

export default async function Home({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;

  return <LandingView oauthError={error} />;
}
