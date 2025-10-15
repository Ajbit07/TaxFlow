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
  fullName: z.string().min(2, "Enter your full name"),
  email: z.string().email("Enter a valid email"),
  phone: z.string().optional(),
  password: z.string().min(8, "Minimum 8 characters"),
});

type FormValues = z.infer<typeof schema>;

export default function SignupPage() {
  const { applyAuth } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const signup = useMutation({
    mutationFn: authApi.register,
    onSuccess: (auth) => {
      applyAuth(auth);
      toast("Account created. Set up your business next.", "success");
      navigate("/onboarding");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <AuthShell title="Create your account" subtitle="Start invoicing and filing GST in minutes.">
      <form onSubmit={handleSubmit((values) => signup.mutate(values))} className="space-y-4">
        <Field label="Full name" error={errors.fullName?.message}>
          <Input placeholder="Priya Sharma" {...register("fullName")} />
        </Field>
        <Field label="Email" error={errors.email?.message}>
          <Input type="email" placeholder="you@business.in" {...register("email")} />
        </Field>
        <Field label="Phone (optional)" error={errors.phone?.message}>
          <Input placeholder="+91 98765 43210" {...register("phone")} />
        </Field>
        <Field label="Password" error={errors.password?.message}>
          <Input type="password" placeholder="At least 8 characters" {...register("password")} />
        </Field>
        <Button type="submit" className="w-full" loading={signup.isPending}>
          Create account
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        Already have an account?{" "}
        <Link to="/login" className="font-medium text-brand-600 hover:underline">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
