import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useToast } from "@/components/ui/toast";
import { Button, Field, Input } from "@/components/ui/primitives";
import AuthShell from "./AuthShell";

const schema = z.object({
  token: z.string().min(10, "Paste the reset token"),
  newPassword: z.string().min(8, "Minimum 8 characters"),
});
type FormValues = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const { toast } = useToast();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { token: params.get("token") ?? "" },
  });

  const reset = useMutation({
    mutationFn: authApi.resetPassword,
    onSuccess: () => {
      toast("Password reset. Sign in with your new password.", "success");
      navigate("/login");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <AuthShell title="Choose a new password" subtitle="Paste your reset token and set a new password.">
      <form onSubmit={handleSubmit((values) => reset.mutate(values))} className="space-y-4">
        <Field label="Reset token" error={errors.token?.message}>
          <Input placeholder="Reset token" {...register("token")} />
        </Field>
        <Field label="New password" error={errors.newPassword?.message}>
          <Input type="password" placeholder="At least 8 characters" {...register("newPassword")} />
        </Field>
        <Button type="submit" className="w-full" loading={reset.isPending}>
          Reset password
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        <Link to="/login" className="font-medium text-brand-600 hover:underline">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
