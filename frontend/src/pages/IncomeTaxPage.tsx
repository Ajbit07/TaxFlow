import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Calculator, Lightbulb } from "lucide-react";
import { taxApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Button, Card, CardTitle, Field, Input } from "@/components/ui/primitives";
import { formatMoney } from "@/lib/utils";
import type { IncomeTaxResponse } from "@/types/api";
import { useState } from "react";

const schema = z.object({
  businessIncome: z.coerce.number().min(0),
  salaryIncome: z.coerce.number().min(0),
  otherIncome: z.coerce.number().min(0),
  investments: z.coerce.number().min(0),
  deductions: z.coerce.number().min(0),
  businessExpenses: z.coerce.number().min(0),
});

type FormValues = z.infer<typeof schema>;

export default function IncomeTaxPage() {
  const businessId = useBusinessId();
  const { toast } = useToast();
  const [result, setResult] = useState<IncomeTaxResponse | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      businessIncome: 0,
      salaryIncome: 0,
      otherIncome: 0,
      investments: 0,
      deductions: 0,
      businessExpenses: 0,
    },
  });

  const estimate = useMutation({
    mutationFn: (values: FormValues) => taxApi.incomeTax(businessId, values),
    onSuccess: setResult,
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Income Tax Estimator</h1>
      <p className="text-sm text-slate-500">
        Estimate annual income tax using the old-regime slabs with 80C investment limits applied automatically.
      </p>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardTitle>Annual income & deductions</CardTitle>
          <form onSubmit={handleSubmit((values) => estimate.mutate(values))} className="grid gap-4 sm:grid-cols-2">
            <Field label="Business income (₹)" error={errors.businessIncome?.message}>
              <Input type="number" step="0.01" {...register("businessIncome")} />
            </Field>
            <Field label="Business expenses (₹)" error={errors.businessExpenses?.message}>
              <Input type="number" step="0.01" {...register("businessExpenses")} />
            </Field>
            <Field label="Salary income (₹)" error={errors.salaryIncome?.message}>
              <Input type="number" step="0.01" {...register("salaryIncome")} />
            </Field>
            <Field label="Other income (₹)" error={errors.otherIncome?.message}>
              <Input type="number" step="0.01" {...register("otherIncome")} />
            </Field>
            <Field label="80C investments (₹)" error={errors.investments?.message}>
              <Input type="number" step="0.01" {...register("investments")} />
            </Field>
            <Field label="Other deductions (₹)" error={errors.deductions?.message}>
              <Input type="number" step="0.01" {...register("deductions")} />
            </Field>
            <div className="sm:col-span-2">
              <Button type="submit" loading={estimate.isPending} className="w-full">
                <Calculator className="h-4 w-4" /> Estimate tax
              </Button>
            </div>
          </form>
        </Card>

        <div className="space-y-4">
          {result ? (
            <>
              <Card>
                <CardTitle>Estimate</CardTitle>
                <dl className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-slate-500">Taxable income</dt>
                    <dd className="font-semibold">{formatMoney(result.taxableIncome)}</dd>
                  </div>
                  <div className="flex justify-between border-t border-slate-200 pt-2 text-base">
                    <dt className="font-semibold">Estimated tax</dt>
                    <dd className="font-bold text-brand-600">{formatMoney(result.estimatedTax)}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-slate-500">Advance tax per quarter</dt>
                    <dd className="font-semibold">{formatMoney(result.advanceTaxDue)}</dd>
                  </div>
                </dl>
                <div className="mt-4 rounded-lg bg-slate-50 p-3 text-sm">
                  <p className="mb-1 text-xs font-semibold uppercase text-slate-400">Profit & loss</p>
                  {Object.entries(result.profitAndLoss).map(([key, value]) => (
                    <div key={key} className="flex justify-between">
                      <span className="text-slate-500">{key.replace(/([A-Z])/g, " $1").toLowerCase()}</span>
                      <span>{formatMoney(value)}</span>
                    </div>
                  ))}
                </div>
              </Card>
              <Card>
                <CardTitle>Tax-saving suggestions</CardTitle>
                <ul className="space-y-2">
                  {result.taxSavingSuggestions.map((suggestion) => (
                    <li key={suggestion} className="flex items-start gap-2 text-sm text-slate-600">
                      <Lightbulb className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
                      {suggestion}
                    </li>
                  ))}
                </ul>
              </Card>
            </>
          ) : (
            <Card className="flex h-full items-center justify-center">
              <p className="py-16 text-center text-sm text-slate-400">
                Enter your annual figures and press <strong>Estimate tax</strong> to see your liability, advance tax
                schedule and saving suggestions.
              </p>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
