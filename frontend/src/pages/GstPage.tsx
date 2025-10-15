import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Banknote, CheckCircle2, ShieldAlert } from "lucide-react";
import { taxApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Badge, Button, Card, CardTitle, EmptyState, Input, Pagination, Select, Skeleton } from "@/components/ui/primitives";
import { formatDate, formatMoney, monthEndIso, monthStartIso } from "@/lib/utils";
import type { Filing, FilingType } from "@/types/api";

const WIZARD_STEPS = ["Period", "Sales review", "Purchases & ITC", "Compliance check", "Review & save"];

export default function GstPage() {
  const businessId = useBusinessId();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [from, setFrom] = useState(monthStartIso());
  const [to, setTo] = useState(monthEndIso());
  const [page, setPage] = useState(0);
  const [wizardStep, setWizardStep] = useState(0);
  const [filingType, setFilingType] = useState<FilingType>("GST_ANNUAL");

  const { data: summary, isLoading: loadingSummary } = useQuery({
    queryKey: ["gst-summary", businessId, from, to],
    queryFn: () => taxApi.gstSummary(businessId, { from, to }),
  });

  const { data: compliance = [] } = useQuery({
    queryKey: ["compliance", businessId],
    queryFn: () => taxApi.compliance(businessId),
  });

  const { data: filings } = useQuery({
    queryKey: ["filings", businessId, page],
    queryFn: () => taxApi.filings(businessId, { page }),
  });

  const saveWizard = useMutation({
    mutationFn: (step: number) =>
      taxApi.saveWizard(businessId, {
        filingType,
        periodStart: from,
        periodEnd: to,
        step,
        answers: { reviewedSales: step >= 2, reviewedPurchases: step >= 3, complianceChecked: step >= 4 },
      }),
    onSuccess: (filing) => {
      queryClient.invalidateQueries({ queryKey: ["filings", businessId] });
      if (filing.status === "BLOCKED") {
        toast("Filing saved but blocked by compliance errors.", "error");
      } else {
        toast(`Filing progress saved (${filing.progressPercent}%)`, "success");
      }
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const submit = useMutation({
    mutationFn: (filing: Filing) => taxApi.submitFiling(businessId, filing.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["filings", businessId] });
      toast("Filing submitted", "success");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const errors = compliance.filter((issue) => issue.severity === "ERROR");

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">GST Filing</h1>

      <Card>
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <p className="mb-1 text-xs font-medium text-slate-500">From</p>
            <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </div>
          <div>
            <p className="mb-1 text-xs font-medium text-slate-500">To</p>
            <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </div>
          <div>
            <p className="mb-1 text-xs font-medium text-slate-500">Filing type</p>
            <Select value={filingType} onChange={(e) => setFilingType(e.target.value as FilingType)}>
              <option value="GST_ANNUAL">GST</option>
              <option value="ITR">ITR</option>
            </Select>
          </div>
        </div>
      </Card>

      {loadingSummary || !summary ? (
        <div className="grid gap-4 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <p className="text-xs text-slate-500">Output GST (sales)</p>
            <p className="text-xl font-bold">{formatMoney(summary.outputGst)}</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Input GST (ITC)</p>
            <p className="text-xl font-bold text-emerald-600">{formatMoney(summary.inputGst)}</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Net GST payable</p>
            <p className="text-xl font-bold text-brand-600">{formatMoney(summary.netGstPayable)}</p>
          </Card>
          <Card>
            <p className="text-xs text-slate-500">Late fee + interest</p>
            <p className="text-xl font-bold text-rose-500">{formatMoney(summary.lateFee + summary.interest)}</p>
          </Card>
        </div>
      )}

      {summary && summary.reminders.length > 0 && (
        <Card className="border-brand-200 bg-brand-50 dark:border-brand-800 dark:bg-brand-900/20">
          <ul className="space-y-1 text-sm text-brand-800 dark:text-brand-200">
            {summary.reminders.map((reminder) => (
              <li key={reminder} className="flex items-center gap-2">
                <Banknote className="h-4 w-4 shrink-0" /> {reminder}
              </li>
            ))}
          </ul>
        </Card>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        {/* Filing wizard */}
        <Card>
          <CardTitle>Filing wizard</CardTitle>
          <ol className="mb-4 space-y-2">
            {WIZARD_STEPS.map((label, index) => (
              <li key={label} className="flex items-center gap-3 text-sm">
                <span
                  className={
                    index < wizardStep
                      ? "flex h-6 w-6 items-center justify-center rounded-full bg-emerald-500 text-xs font-bold text-white"
                      : index === wizardStep
                        ? "flex h-6 w-6 items-center justify-center rounded-full bg-brand-600 text-xs font-bold text-white"
                        : "flex h-6 w-6 items-center justify-center rounded-full bg-slate-200 text-xs font-bold text-slate-500 dark:bg-slate-700"
                  }
                >
                  {index < wizardStep ? "✓" : index + 1}
                </span>
                <span className={index === wizardStep ? "font-semibold" : "text-slate-500"}>{label}</span>
              </li>
            ))}
          </ol>
          <div className="flex gap-2">
            <Button
              variant="outline"
              disabled={wizardStep === 0}
              onClick={() => setWizardStep((s) => Math.max(0, s - 1))}
            >
              Back
            </Button>
            <Button
              loading={saveWizard.isPending}
              onClick={() => {
                const next = Math.min(WIZARD_STEPS.length, wizardStep + 1);
                setWizardStep(next);
                saveWizard.mutate(next);
              }}
            >
              {wizardStep >= WIZARD_STEPS.length - 1 ? "Save & mark ready" : "Save & continue"}
            </Button>
          </div>
        </Card>

        {/* Compliance */}
        <Card>
          <CardTitle>Compliance checks</CardTitle>
          {compliance.length === 0 ? (
            <div className="flex items-center gap-2 rounded-lg bg-emerald-50 p-4 text-sm text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-300">
              <CheckCircle2 className="h-5 w-5" /> All checks passed. You are ready to file.
            </div>
          ) : (
            <ul className="max-h-64 space-y-2 overflow-y-auto">
              {compliance.map((issue, index) => (
                <li key={index} className="flex items-start gap-2 rounded-lg border border-slate-100 p-3 text-sm dark:border-slate-800">
                  <ShieldAlert
                    className={issue.severity === "ERROR" ? "h-4 w-4 shrink-0 text-rose-500" : "h-4 w-4 shrink-0 text-amber-500"}
                  />
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-semibold text-slate-500">{issue.entity}</span>
                      <Badge value={issue.severity} />
                    </div>
                    <p className="text-slate-600 dark:text-slate-300">{issue.message}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
          {errors.length > 0 && (
            <p className="mt-3 text-xs text-rose-500">
              {errors.length} blocking error{errors.length > 1 ? "s" : ""} must be fixed before submission.
            </p>
          )}
        </Card>
      </div>

      {/* Filing history */}
      <Card>
        <CardTitle>Filing history</CardTitle>
        {!filings || filings.content.length === 0 ? (
          <EmptyState title="No filings yet" subtitle="Save progress in the wizard to start a filing record." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-700">
                  <th className="py-2 pr-4">Type</th>
                  <th className="py-2 pr-4">Period</th>
                  <th className="py-2 pr-4">Status</th>
                  <th className="py-2 pr-4">Progress</th>
                  <th className="py-2 pr-4 text-right">Tax due</th>
                  <th className="py-2 pr-4">Submitted</th>
                  <th className="py-2" />
                </tr>
              </thead>
              <tbody>
                {filings.content.map((filing) => (
                  <tr key={filing.id} className="border-b border-slate-100 last:border-0 dark:border-slate-800">
                    <td className="py-3 pr-4 font-medium">{filing.filingType.replaceAll("_", " ")}</td>
                    <td className="py-3 pr-4 text-slate-500">
                      {formatDate(filing.periodStart)} → {formatDate(filing.periodEnd)}
                    </td>
                    <td className="py-3 pr-4">
                      <Badge value={filing.status} />
                    </td>
                    <td className="py-3 pr-4">
                      <div className="h-2 w-24 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
                        <div className="h-full rounded-full bg-brand-500" style={{ width: `${filing.progressPercent}%` }} />
                      </div>
                    </td>
                    <td className="py-3 pr-4 text-right">{formatMoney(filing.taxDue)}</td>
                    <td className="py-3 pr-4 text-slate-500">{formatDate(filing.submittedAt)}</td>
                    <td className="py-3 text-right">
                      {(filing.status === "READY_FOR_REVIEW" || filing.status === "DRAFT") && (
                        <Button size="sm" variant="outline" loading={submit.isPending} onClick={() => submit.mutate(filing)}>
                          Submit
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {filings && <Pagination page={page} totalPages={filings.totalPages} onChange={setPage} />}
      </Card>
    </div>
  );
}
