import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useToast } from "@/components/ui/toast";
import { Button, Field, Input } from "@/components/ui/primitives";
import AuthShell from "./AuthShell";

const schema = z.object({ email: z.string().email("Enter a valid email") });
type FormValues = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const { toast } = useToast();
  const [resetToken, setResetToken] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const request = useMutation({
    mutationFn: (values: FormValues) => authApi.forgotPassword(values.email),
    onSuccess: (data) => setResetToken(data.resetToken),
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <AuthShell title="Reset your password" subtitle="We will issue a reset token for your account.">
      {resetToken ? (
        <div className="space-y-4">
          <p className="text-sm text-slate-600 dark:text-slate-300">
            Your reset token has been issued. Use it on the reset page:
          </p>
          <code className="block break-all rounded-lg bg-slate-100 p-3 text-xs dark:bg-slate-800">{resetToken}</code>
          <Link to={`/reset-password?token=${resetToken}`}>
            <Button className="w-full">Continue to reset</Button>
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit((values) => request.mutate(values))} className="space-y-4">
          <Field label="Email" error={errors.email?.message}>
            <Input type="email" placeholder="you@business.in" {...register("email")} />
          </Field>
          <Button type="submit" className="w-full" loading={request.isPending}>
            Request reset token
          </Button>
        </form>
      )}
      <p className="mt-6 text-center text-sm text-slate-500">
        <Link to="/login" className="font-medium text-brand-600 hover:underline">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
