import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/components/ui/toast";
import { Button, Field, Input } from "@/components/ui/primitives";
import AuthShell from "./AuthShell";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const { applyAuth } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const login = useMutation({
    mutationFn: authApi.login,
    onSuccess: (auth) => {
      applyAuth(auth);
      navigate("/dashboard");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to manage GST, invoices and taxes.">
      <form onSubmit={handleSubmit((values) => login.mutate(values))} className="space-y-4">
        <Field label="Email" error={errors.email?.message}>
          <Input type="email" placeholder="you@business.in" {...register("email")} />
        </Field>
        <Field label="Password" error={errors.password?.message}>
          <Input type="password" placeholder="••••••••" {...register("password")} />
        </Field>
        <div className="flex justify-end">
          <Link to="/forgot-password" className="text-xs text-brand-600 hover:underline">
            Forgot password?
          </Link>
        </div>
        <Button type="submit" className="w-full" loading={login.isPending}>
          Sign in
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        New to TaxFlow?{" "}
        <Link to="/signup" className="font-medium text-brand-600 hover:underline">
          Create an account
        </Link>
      </p>
    </AuthShell>
  );
}
