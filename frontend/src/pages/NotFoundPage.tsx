import { Link } from "react-router-dom";
import { Compass } from "lucide-react";
import { Button } from "@/components/ui/primitives";

export default function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center">
      <Compass className="h-16 w-16 text-slate-300" />
      <h1 className="text-4xl font-bold">404</h1>
      <p className="text-sm text-slate-500">The page you are looking for doesn't exist or has moved.</p>
      <Link to="/dashboard">
        <Button>Back to dashboard</Button>
      </Link>
    </div>
  );
}
