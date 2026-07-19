import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { authApi, auditApi, businessApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusiness, useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import BusinessForm from "@/components/BusinessForm";
import { Button, Card, CardTitle, Field, Input, Pagination, Skeleton } from "@/components/ui/primitives";
import { cn, formatDateTime } from "@/lib/utils";
import type { TaskScope } from "@/types/api";

const SCOPES: TaskScope[] = [
  "DASHBOARD", "INVOICE", "CUSTOMER", "PRODUCT", "INVENTORY", "EXPENSE", "GST", "INCOME_TAX",
  "DOCUMENT", "REPORT", "NOTIFICATION", "SETTINGS",
];

const passwordSchema = z.object({
  currentPassword: z.string().min(1, "Required"),
  newPassword: z.string().min(8, "Minimum 8 characters"),
});
type PasswordValues = z.infer<typeof passwordSchema>;

const memberSchema = z.object({
  email: z.string().email("Valid email required"),
});
type MemberValues = z.infer<typeof memberSchema>;

type Tab = "business" | "team" | "security" | "audit";

export default function SettingsPage() {
  const businessId = useBusinessId();
  const { business } = useBusiness();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [tab, setTab] = useState<Tab>("business");
  const [auditPage, setAuditPage] = useState(0);
  const [selectedScopes, setSelectedScopes] = useState<TaskScope[]>(["DASHBOARD", "INVOICE"]);

  const updateBusiness = useMutation({
    mutationFn: (body: Parameters<typeof businessApi.update>[1]) => businessApi.update(businessId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["businesses"] });
      toast("Business profile updated", "success");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const passwordForm = useForm<PasswordValues>({ resolver: zodResolver(passwordSchema) });
  const changePassword = useMutation({
    mutationFn: authApi.changePassword,
    onSuccess: () => {
      toast("Password changed", "success");
      passwordForm.reset();
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const memberForm = useForm<MemberValues>({ resolver: zodResolver(memberSchema) });
  const assign = useMutation({
    mutationFn: (values: MemberValues) => businessApi.assignEmployee(businessId, { email: values.email, scopes: selectedScopes }),
    onSuccess: () => {
      toast("Employee assigned. Default password for new users: TaxFlow@123", "success");
      memberForm.reset();
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const { data: audit, isLoading: loadingAudit } = useQuery({
    queryKey: ["audit", businessId, auditPage],
    queryFn: () => auditApi.list(businessId, { page: auditPage }),
    enabled: tab === "audit",
  });

  function toggleScope(scope: TaskScope) {
    setSelectedScopes((current) =>
      current.includes(scope) ? current.filter((s) => s !== scope) : [...current, scope],
    );
  }

  const tabs: { id: Tab; label: string }[] = [
    { id: "business", label: "Business profile" },
    { id: "team", label: "Team access" },
    { id: "security", label: "Security" },
    { id: "audit", label: "Audit log" },
  ];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Settings</h1>

      <div className="flex gap-1 border-b border-slate-200">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={cn(
              "border-b-2 px-4 py-2 text-sm font-medium transition-colors",
              tab === t.id
                ? "border-brand-600 text-brand-600"
                : "border-transparent text-slate-500 hover:text-slate-700",
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "business" && (
        <Card>
          <CardTitle>Business profile</CardTitle>
          <BusinessForm
            initial={business}
            onSubmit={(body) => updateBusiness.mutate(body)}
            submitting={updateBusiness.isPending}
            submitLabel="Update business"
          />
        </Card>
      )}

      {tab === "team" && (
        <Card>
          <CardTitle>Assign an employee or accountant</CardTitle>
          <p className="mb-4 text-sm text-slate-500">
            Grant scoped access to this business. If the email is new, an employee account is created with the default
            password <code className="rounded bg-slate-100 px-1">TaxFlow@123</code> (they should change it after first login).
          </p>
          <form onSubmit={memberForm.handleSubmit((values) => assign.mutate(values))} className="space-y-4">
            <Field label="Employee email" error={memberForm.formState.errors.email?.message}>
              <Input type="email" placeholder="accountant@firm.in" {...memberForm.register("email")} />
            </Field>
            <div>
              <p className="mb-2 text-xs font-medium text-slate-600">Allowed areas</p>
              <div className="flex flex-wrap gap-2">
                {SCOPES.map((scope) => (
                  <button
                    key={scope}
                    type="button"
                    onClick={() => toggleScope(scope)}
                    className={cn(
                      "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                      selectedScopes.includes(scope)
                        ? "border-brand-600 bg-brand-50 text-brand-700"
                        : "border-slate-300 text-slate-500 hover:border-slate-400",
                    )}
                  >
                    {scope.replaceAll("_", " ")}
                  </button>
                ))}
              </div>
            </div>
            <Button type="submit" loading={assign.isPending} disabled={selectedScopes.length === 0}>
              Assign access
            </Button>
          </form>
        </Card>
      )}

      {tab === "security" && (
        <Card className="max-w-lg">
          <CardTitle>Change password</CardTitle>
          <form onSubmit={passwordForm.handleSubmit((values) => changePassword.mutate(values))} className="space-y-4">
            <Field label="Current password" error={passwordForm.formState.errors.currentPassword?.message}>
              <Input type="password" {...passwordForm.register("currentPassword")} />
            </Field>
            <Field label="New password" error={passwordForm.formState.errors.newPassword?.message}>
              <Input type="password" {...passwordForm.register("newPassword")} />
            </Field>
            <Button type="submit" loading={changePassword.isPending}>
              Change password
            </Button>
          </form>
        </Card>
      )}

      {tab === "audit" && (
        <Card>
          <CardTitle>Audit log</CardTitle>
          {loadingAudit ? (
            <div className="space-y-2">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-10" />
              ))}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400">
                    <th className="py-2 pr-4">Time</th>
                    <th className="py-2 pr-4">Action</th>
                    <th className="py-2 pr-4">Entity</th>
                    <th className="py-2 pr-4">Change</th>
                    <th className="py-2">Result</th>
                  </tr>
                </thead>
                <tbody>
                  {audit?.content.map((entry) => (
                    <tr key={entry.id} className="border-b border-slate-100 last:border-0">
                      <td className="py-2 pr-4 text-xs text-slate-500">{formatDateTime(entry.actionTime)}</td>
                      <td className="py-2 pr-4 font-medium">{entry.action}</td>
                      <td className="py-2 pr-4 text-slate-500">{entry.entityType}</td>
                      <td className="max-w-xs truncate py-2 pr-4 text-xs text-slate-400">
                        {entry.oldValue ? `${entry.oldValue} → ` : ""}
                        {entry.newValue ?? "—"}
                      </td>
                      <td className="py-2">
                        <span className={entry.success ? "text-emerald-600" : "text-rose-500"}>
                          {entry.success ? "OK" : "Failed"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {audit && <Pagination page={auditPage} totalPages={audit.totalPages} onChange={setAuditPage} />}
        </Card>
      )}
    </div>
  );
}
