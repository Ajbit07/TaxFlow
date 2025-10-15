import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Receipt } from "lucide-react";
import { expenseApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Button, Card, EmptyState, Field, Input, Modal, Pagination, Select, Skeleton } from "@/components/ui/primitives";
import { formatDate, formatMoney, todayIso } from "@/lib/utils";
import type { Expense, ExpenseCategory } from "@/types/api";

const CATEGORIES: ExpenseCategory[] = [
  "FUEL", "RENT", "ELECTRICITY", "SALARY", "MAINTENANCE", "TRAVEL", "FOOD", "OFFICE", "BILLS", "OTHER",
];

const schema = z.object({
  category: z.enum(CATEGORIES as [ExpenseCategory, ...ExpenseCategory[]]),
  vendor: z.string().min(1, "Vendor required"),
  amount: z.coerce.number().gt(0, "Amount must be positive"),
  gstAmount: z.coerce.number().min(0),
  expenseDate: z.string().min(1, "Date required"),
  description: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export default function ExpensesPage() {
  const businessId = useBusinessId();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [page, setPage] = useState(0);
  const [category, setCategory] = useState<ExpenseCategory | "">("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Expense | null>(null);
  const [deleting, setDeleting] = useState<Expense | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["expenses", businessId, page, category],
    queryFn: () => expenseApi.list(businessId, { page, category: category || undefined }),
  });

  const form = useForm<FormValues>({ resolver: zodResolver(schema) });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["expenses", businessId] });

  const save = useMutation({
    mutationFn: (values: FormValues) =>
      editing
        ? expenseApi.update(businessId, editing.id, { ...values, description: values.description || null })
        : expenseApi.create(businessId, { ...values, description: values.description || null }),
    onSuccess: () => {
      invalidate();
      toast(editing ? "Expense updated" : "Expense recorded", "success");
      setModalOpen(false);
      setEditing(null);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const remove = useMutation({
    mutationFn: (id: string) => expenseApi.remove(businessId, id),
    onSuccess: () => {
      invalidate();
      toast("Expense deleted", "success");
      setDeleting(null);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  function openModal(expense?: Expense) {
    setEditing(expense ?? null);
    form.reset(
      expense
        ? {
            category: expense.category,
            vendor: expense.vendor,
            amount: expense.amount,
            gstAmount: expense.gstAmount,
            expenseDate: expense.expenseDate,
            description: expense.description ?? "",
          }
        : { category: "OTHER", vendor: "", amount: 0, gstAmount: 0, expenseDate: todayIso(), description: "" },
    );
    setModalOpen(true);
  }

  const monthTotal = data?.content.reduce((sum, e) => sum + e.amount, 0) ?? 0;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Expenses</h1>
        <Button onClick={() => openModal()}>
          <Plus className="h-4 w-4" /> Record expense
        </Button>
      </div>

      <Card>
        <div className="mb-4 flex flex-wrap items-center gap-3">
          <Select
            value={category}
            onChange={(e) => {
              setCategory(e.target.value as ExpenseCategory | "");
              setPage(0);
            }}
            className="max-w-48"
          >
            <option value="">All categories</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <p className="ml-auto text-sm text-slate-500">
            Page total: <strong>{formatMoney(monthTotal)}</strong>
          </p>
        </div>

        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<Receipt className="h-10 w-10" />}
            title="No expenses recorded"
            subtitle="Recording expenses with GST paid maximises your input tax credit."
            action={<Button onClick={() => openModal()}>Record expense</Button>}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-700">
                  <th className="py-2 pr-4">Date</th>
                  <th className="py-2 pr-4">Vendor</th>
                  <th className="py-2 pr-4">Category</th>
                  <th className="py-2 pr-4 text-right">GST paid</th>
                  <th className="py-2 pr-4 text-right">Amount</th>
                  <th className="py-2" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((expense) => (
                  <tr key={expense.id} className="border-b border-slate-100 last:border-0 dark:border-slate-800">
                    <td className="py-3 pr-4 text-slate-500">{formatDate(expense.expenseDate)}</td>
                    <td className="py-3 pr-4">
                      <p className="font-medium">{expense.vendor}</p>
                      {expense.description && <p className="text-xs text-slate-400">{expense.description}</p>}
                    </td>
                    <td className="py-3 pr-4">
                      <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs dark:bg-slate-800">{expense.category}</span>
                    </td>
                    <td className="py-3 pr-4 text-right text-slate-500">{formatMoney(expense.gstAmount)}</td>
                    <td className="py-3 pr-4 text-right font-semibold">{formatMoney(expense.amount)}</td>
                    <td className="py-3 text-right">
                      <Button variant="ghost" size="sm" onClick={() => openModal(expense)}>
                        Edit
                      </Button>
                      <Button variant="ghost" size="sm" className="text-rose-500" onClick={() => setDeleting(expense)}>
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </Card>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? "Edit expense" : "Record expense"}>
        <form onSubmit={form.handleSubmit((values) => save.mutate(values))} className="grid gap-4 sm:grid-cols-2">
          <Field label="Category" error={form.formState.errors.category?.message}>
            <Select {...form.register("category")}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Vendor" error={form.formState.errors.vendor?.message}>
            <Input placeholder="Airtel, landlord…" {...form.register("vendor")} />
          </Field>
          <Field label="Amount (₹)" error={form.formState.errors.amount?.message}>
            <Input type="number" step="0.01" {...form.register("amount")} />
          </Field>
          <Field label="GST paid (₹)" error={form.formState.errors.gstAmount?.message}>
            <Input type="number" step="0.01" {...form.register("gstAmount")} />
          </Field>
          <Field label="Date" error={form.formState.errors.expenseDate?.message}>
            <Input type="date" {...form.register("expenseDate")} />
          </Field>
          <Field label="Description (optional)" error={form.formState.errors.description?.message}>
            <Input {...form.register("description")} />
          </Field>
          <div className="flex items-end justify-end gap-2 sm:col-span-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={save.isPending}>
              {editing ? "Save changes" : "Record"}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleting} onClose={() => setDeleting(null)} title="Delete expense">
        <p className="text-sm text-slate-600 dark:text-slate-300">
          Delete the {deleting?.category} expense of <strong>{formatMoney(deleting?.amount ?? 0)}</strong> from{" "}
          <strong>{deleting?.vendor}</strong>?
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="outline" onClick={() => setDeleting(null)}>
            Cancel
          </Button>
          <Button variant="danger" loading={remove.isPending} onClick={() => deleting && remove.mutate(deleting.id)}>
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}
