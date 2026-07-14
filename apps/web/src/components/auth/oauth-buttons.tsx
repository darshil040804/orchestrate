import { buttonVariants } from "@/components/ui/button";
import { API_ORIGIN } from "@/lib/api/client";

// Real full-page navigations, not fetch calls — OAuth is a server-side redirect
// round trip to Google/GitHub and back, not something JS can drive with a click
// handler. Base UI's Button has no link/href semantics, so these are plain
// anchors styled with the button's own exported variant classes.
export function OAuthButtons() {
  return (
    <div className="flex flex-col gap-2">
      <a
        href={`${API_ORIGIN}/oauth2/authorization/google`}
        className={buttonVariants({ variant: "outline" })}
      >
        Continue with Google
      </a>
      <a
        href={`${API_ORIGIN}/oauth2/authorization/github`}
        className={buttonVariants({ variant: "outline" })}
      >
        Continue with GitHub
      </a>
    </div>
  );
}
