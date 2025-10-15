import { useEffect, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useFieldArray, useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2 } from "lucide-react";
import { customerApi, invoiceApi, productApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Button, Card, CardTitle, Field, Input, Select, Skeleton, Textarea } from "@/components/ui/primitives";
import { formatMoney, todayIso } from "@/lib/utils";
import type { InvoiceRequest, InvoiceStatus, InvoiceType } from "@/types/api";

const lineSchema = z.object({
  productId: z.string().optional(),
  description: z.string().min(1, "Required"),
  hsnCode: z.string().min(1, "Required").max(8),
  quantity: z.coerce.number().gt(0, "> 0"),
  unitPrice: z.coerce.number().min(0),
  gstRate: z.coerce.number().min(0).max(28),
});

const schema = z.object({
  customerId: z.string().optional(),
  invoiceNumber: z.string().optional(),
  type: z.enum(["GST_INVOICE", "PURCHASE_BILL", "CREDIT_NOTE", "DEBIT_NOTE"]),
  status: z.enum(["DRAFT", "SENT", "PAID", "PARTIALLY_PAID", "OVERDUE", "CANCELLED", "RECURRING"]),
  invoiceDate: z.string().min(1, "Required"),
  dueDate: z.string().optional(),
  paidAmount: z.coerce.number().min(0).optional(),
  notes: z.string().optional(),
  lines: z.array(lineSchema).min(1, "Add at least one line"),
});

type FormValues = z.infer<typeof schema>;

export default function InvoiceFormPage() {
  const businessId = useBusinessId();
  const { invoiceId } = useParams();
  const isEdit = !!invoiceId;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const { data: existing, isLoading: loadingExisting } = useQuery({
    queryKey: ["invoice", businessId, invoiceId],
    queryFn: () => invoiceApi.get(businessId, invoiceId!),
    enabled: isEdit,
  });

  const { data: customers } = useQuery({
    queryKey: ["customers-all", businessId],
    queryFn: () => customerApi.list(businessId, { page: 0, size: 200 }),
  });

  const { data: products } = useQuery({
    queryKey: ["products-all", businessId],
    queryFn: () => productApi.list(businessId, { page: 0, size: 200 }),
  });

  const { data: nextNumber } = useQuery({
    queryKey: ["next-number", businessId],
    queryFn: () => invoiceApi.nextNumber(businessId),
    enabled: !isEdit,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      type: "GST_INVOICE",
      status: "DRAFT",
      invoiceDate: todayIso(),
      paidAmount: 0,
      lines: [{ description: "", hsnCode: "", quantity: 1, unitPrice: 0, gstRate: 18 }],
    },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "lines" });

  useEffect(() => {
    if (existing) {
      form.reset({
        customerId: existing.customerId ?? "",
        invoiceNumber: existing.invoiceNumber,
        type: existing.type,
        status: existing.status,
        invoiceDate: existing.invoiceDate,
        dueDate: existing.dueDate ?? "",
        paidAmount: existing.paidAmount,
        notes: existing.notes ?? "",
        lines: existing.lines.map((line) => ({
          productId: line.productId ?? "",
          description: line.description,
          hsnCode: line.hsnCode,
          quantity: line.quantity,
          unitPrice: line.unitPrice,
          gstRate: line.gstRate,
        })),
      });
    }
  }, [existing, form]);

  useEffect(() => {
    if (!isEdit && nextNumber) {
      form.setValue("invoiceNumber", nextNumber.invoiceNumber);
    }
  }, [nextNumber, isEdit, form]);

  const watchedLines = form.watch("lines");
  const totals = useMemo(() => {
    let subtotal = 0;
    let gst = 0;
    for (const line of watchedLines ?? []) {
      const taxable = (Number(line.quantity) || 0) * (Number(line.unitPrice) || 0);
      subtotal += taxable;
      gst += (taxable * (Number(line.gstRate) || 0)) / 100;
    }
    return { subtotal, gst, total: subtotal + gst };
  }, [watchedLines]);

  const save = useMutation({
    mutationFn: (values: FormValues) => {
      const body: InvoiceRequest = {
        customerId: values.customerId || null,
        invoiceNumber: values.invoiceNumber || null,
        type: values.type as InvoiceType,
        status: values.status as InvoiceStatus,
        invoiceDate: values.invoiceDate,
        dueDate: values.dueDate || null,
        recurring: values.status === "RECURRING",
        paidAmount: values.paidAmount ?? 0,
        notes: values.notes || null,
        lines: values.lines.map((line) => ({ ...line, productId: line.productId || null })),
      };
      return isEdit ? invoiceApi.update(businessId, invoiceId!, body) : invoiceApi.create(businessId, body);
    },
    onSuccess: (invoice) => {
      queryClient.invalidateQueries({ queryKey: ["invoices", businessId] });
      toast(isEdit ? "Invoice updated" : `Invoice ${invoice.invoiceNumber} created`, "success");
      navigate(`/invoices/${invoice.id}`);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  function applyProduct(index: number, productId: string) {
    form.setValue(`lines.${index}.productId`, productId);
    const product = products?.content.find((p) => p.id === productId);
    if (product) {
      form.setValue(`lines.${index}.description`, product.name);
      form.setValue(`lines.${index}.hsnCode`, product.hsnCode);
      form.setValue(`lines.${index}.unitPrice`, product.sellingPrice);
      form.setValue(`lines.${index}.gstRate`, product.gstPercentage);
    }
  }

  if (isEdit && loadingExisting) {
    return <Skeleton className="h-96" />;
  }

  return (
    <form onSubmit={form.handleSubmit((values) => save.mutate(values))} className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{isEdit ? `Edit ${existing?.invoiceNumber}` : "New invoice"}</h1>
        <div className="flex gap-2">
          <Button type="button" variant="outline" onClick={() => navigate(-1)}>
            Cancel
          </Button>
          <Button type="submit" loading={save.isPending}>
            {isEdit ? "Save changes" : "Create invoice"}
          </Button>
        </div>
      </div>

      <Card>
        <CardTitle>Details</CardTitle>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Customer">
            <Select {...form.register("customerId")}>
              <option value="">Walk-in customer</option>
              {customers?.content.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Invoice number" error={form.formState.errors.invoiceNumber?.message}>
            <Input {...form.register("invoiceNumber")} />
          </Field>
          <Field label="Type">
            <Select {...form.register("type")}>
              <option value="GST_INVOICE">GST Invoice (sale)</option>
              <option value="PURCHASE_BILL">Purchase bill</option>
              <option value="CREDIT_NOTE">Credit note</option>
              <option value="DEBIT_NOTE">Debit note</option>
            </Select>
          </Field>
          <Field label="Status">
            <Select {...form.register("status")}>
              {["DRAFT", "SENT", "PAID", "PARTIALLY_PAID", "OVERDUE", "CANCELLED", "RECURRING"].map((s) => (
                <option key={s} value={s}>
                  {s.replaceAll("_", " ")}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Invoice date" error={form.formState.errors.invoiceDate?.message}>
            <Input type="date" {...form.register("invoiceDate")} />
          </Field>
          <Field label="Due date">
            <Input type="date" {...form.register("dueDate")} />
          </Field>
          <Field label="Paid amount (₹)">
            <Input type="number" step="0.01" {...form.register("paidAmount")} />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Notes / terms">
              <Textarea rows={2} placeholder="Payment terms, thank-you note…" {...form.register("notes")} />
            </Field>
          </div>
        </div>
      </Card>

      <Card>
        <div className="mb-3 flex items-center justify-between">
          <CardTitle className="mb-0">Line items</CardTitle>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => append({ description: "", hsnCode: "", quantity: 1, unitPrice: 0, gstRate: 18 })}
          >
            <Plus className="h-4 w-4" /> Add line
          </Button>
        </div>
        {form.formState.errors.lines?.message && (
          <p className="mb-2 text-xs text-rose-500">{form.formState.errors.lines.message}</p>
        )}
        <div className="space-y-3">
          {fields.map((field, index) => {
            const lineErrors = form.formState.errors.lines?.[index];
            const line = watchedLines?.[index];
            const taxable = (Number(line?.quantity) || 0) * (Number(line?.unitPrice) || 0);
            const lineGst = (taxable * (Number(line?.gstRate) || 0)) / 100;
            return (
              <div key={field.id} className="grid items-end gap-2 rounded-lg border border-slate-100 p-3 dark:border-slate-800 md:grid-cols-[1.5fr_2fr_1fr_0.8fr_1fr_0.8fr_1.2fr_auto]">
                <Field label="Product">
                  <Select value={line?.productId ?? ""} onChange={(e) => applyProduct(index, e.target.value)}>
                    <option value="">Custom item</option>
                    {products?.content.map((product) => (
                      <option key={product.id} value={product.id}>
                        {product.name}
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Description" error={lineErrors?.description?.message}>
                  <Input {...form.register(`lines.${index}.description`)} />
                </Field>
                <Field label="HSN" error={lineErrors?.hsnCode?.message}>
                  <Input {...form.register(`lines.${index}.hsnCode`)} />
                </Field>
                <Field label="Qty" error={lineErrors?.quantity?.message}>
                  <Input type="number" step="0.001" {...form.register(`lines.${index}.quantity`)} />
                </Field>
                <Field label="Rate (₹)" error={lineErrors?.unitPrice?.message}>
                  <Input type="number" step="0.01" {...form.register(`lines.${index}.unitPrice`)} />
                </Field>
                <Field label="GST %" error={lineErrors?.gstRate?.message}>
                  <Input type="number" step="0.01" {...form.register(`lines.${index}.gstRate`)} />
                </Field>
                <div className="pb-1 text-right text-sm">
                  <p className="text-xs text-slate-400">Total</p>
                  <p className="font-semibold">{formatMoney(taxable + lineGst)}</p>
                </div>
                <Button type="button" variant="ghost" size="sm" className="text-rose-500" onClick={() => remove(index)} disabled={fields.length === 1}>
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            );
          })}
        </div>

        <div className="mt-4 ml-auto w-full max-w-xs space-y-1 rounded-lg bg-slate-50 p-4 text-sm dark:bg-slate-800/50">
          <div className="flex justify-between">
            <span className="text-slate-500">Subtotal</span>
            <span>{formatMoney(totals.subtotal)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">GST</span>
            <span>{formatMoney(totals.gst)}</span>
          </div>
          <div className="flex justify-between border-t border-slate-200 pt-1 font-bold dark:border-slate-700">
            <span>Grand total</span>
            <span>{formatMoney(totals.total)}</span>
          </div>
        </div>
      </Card>
    </form>
  );
}
