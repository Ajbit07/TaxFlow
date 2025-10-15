import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Users } from "lucide-react";
import { customerApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useDebounce } from "@/hooks/useDebounce";
import { useToast } from "@/components/ui/toast";
import {
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  Pagination,
  Skeleton,
} from "@/components/ui/primitives";
import { formatMoney } from "@/lib/utils";
import type { Customer } from "@/types/api";

const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;
const PAN_REGEX = /^[A-Z]{5}[0-9]{4}[A-Z]$/;

const schema = z.object({
  name: z.string().min(2, "Name required"),
  gstin: z
    .string()
    .transform((v) => v.trim().toUpperCase())
    .refine((v) => v === "" || GSTIN_REGEX.test(v), "Invalid GSTIN")
    .optional()
    .or(z.literal("")),
  pan: z
    .string()
    .transform((v) => v.trim().toUpperCase())
    .refine((v) => v === "" || PAN_REGEX.test(v), "Invalid PAN")
    .optional()
    .or(z.literal("")),
  phone: z.string().optional(),
  email: z.string().email("Invalid email").optional().or(z.literal("")),
  address: z.string().optional(),
  openingBalance: z.coerce.number().min(0).optional(),
});

type FormValues = z.infer<typeof schema>;

export default function CustomersPage() {
  const businessId = useBusinessId();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Customer | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [deleting, setDeleting] = useState<Customer | null>(null);
  const debouncedQuery = useDebounce(query);

  const { data, isLoading } = useQuery({
    queryKey: ["customers", businessId, page, debouncedQuery],
    queryFn: () => customerApi.list(businessId, { page, query: debouncedQuery || undefined }),
  });

  const form = useForm<FormValues>({ resolver: zodResolver(schema) });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["customers", businessId] });

  const save = useMutation({
    mutationFn: (values: FormValues) => {
      const body = {
        ...values,
        gstin: values.gstin || null,
        pan: values.pan || null,
        email: values.email || null,
      };
      return editing ? customerApi.update(businessId, editing.id, body) : customerApi.create(businessId, body);
    },
    onSuccess: () => {
      invalidate();
      toast(editing ? "Customer updated" : "Customer created", "success");
      closeModal();
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const remove = useMutation({
    mutationFn: (id: string) => customerApi.remove(businessId, id),
    onSuccess: () => {
      invalidate();
      toast("Customer deleted", "success");
      setDeleting(null);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  function openModal(customer?: Customer) {
    setEditing(customer ?? null);
    form.reset(
      customer
        ? {
            name: customer.name,
            gstin: customer.gstin ?? "",
            pan: customer.pan ?? "",
            phone: customer.phone ?? "",
            email: customer.email ?? "",
            address: customer.address ?? "",
            openingBalance: customer.outstandingBalance,
          }
        : { name: "", gstin: "", pan: "", phone: "", email: "", address: "", openingBalance: 0 },
    );
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setEditing(null);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Customers</h1>
        <Button onClick={() => openModal()}>
          <Plus className="h-4 w-4" /> Add customer
        </Button>
      </div>

      <Card>
        <Input
          placeholder="Search customers by name…"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setPage(0);
          }}
          className="mb-4 max-w-sm"
        />

        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<Users className="h-10 w-10" />}
            title="No customers yet"
            subtitle="Add your first customer to start creating GST invoices."
            action={<Button onClick={() => openModal()}>Add customer</Button>}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-700">
                  <th className="py-2 pr-4">Name</th>
                  <th className="py-2 pr-4">GSTIN</th>
                  <th className="py-2 pr-4">Contact</th>
                  <th className="py-2 pr-4 text-right">Invoices</th>
                  <th className="py-2 pr-4 text-right">Lifetime sales</th>
                  <th className="py-2 pr-4 text-right">Outstanding</th>
                  <th className="py-2" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((customer) => (
                  <tr key={customer.id} className="border-b border-slate-100 last:border-0 dark:border-slate-800">
                    <td className="py-3 pr-4 font-medium">{customer.name}</td>
                    <td className="py-3 pr-4 text-slate-500">{customer.gstin ?? "—"}</td>
                    <td className="py-3 pr-4 text-slate-500">{customer.phone ?? customer.email ?? "—"}</td>
                    <td className="py-3 pr-4 text-right">{customer.invoiceCount}</td>
                    <td className="py-3 pr-4 text-right">{formatMoney(customer.lifetimeSales)}</td>
                    <td className="py-3 pr-4 text-right font-medium">
                      <span className={customer.outstandingBalance > 0 ? "text-amber-600" : ""}>
                        {formatMoney(customer.outstandingBalance)}
                      </span>
                    </td>
                    <td className="py-3 text-right">
                      <Button variant="ghost" size="sm" onClick={() => openModal(customer)}>
                        Edit
                      </Button>
                      <Button variant="ghost" size="sm" className="text-rose-500" onClick={() => setDeleting(customer)}>
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

      <Modal open={modalOpen} onClose={closeModal} title={editing ? "Edit customer" : "Add customer"}>
        <form onSubmit={form.handleSubmit((values) => save.mutate(values))} className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field label="Name" error={form.formState.errors.name?.message}>
              <Input {...form.register("name")} />
            </Field>
          </div>
          <Field label="GSTIN" error={form.formState.errors.gstin?.message}>
            <Input placeholder="27ABCDE1234F1Z5" {...form.register("gstin")} />
          </Field>
          <Field label="PAN" error={form.formState.errors.pan?.message}>
            <Input placeholder="ABCDE1234F" {...form.register("pan")} />
          </Field>
          <Field label="Phone" error={form.formState.errors.phone?.message}>
            <Input {...form.register("phone")} />
          </Field>
          <Field label="Email" error={form.formState.errors.email?.message}>
            <Input type="email" {...form.register("email")} />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Address" error={form.formState.errors.address?.message}>
              <Input {...form.register("address")} />
            </Field>
          </div>
          <Field label="Opening balance (₹)" error={form.formState.errors.openingBalance?.message}>
            <Input type="number" step="0.01" {...form.register("openingBalance")} />
          </Field>
          <div className="flex items-end justify-end gap-2 sm:col-span-2">
            <Button type="button" variant="outline" onClick={closeModal}>
              Cancel
            </Button>
            <Button type="submit" loading={save.isPending}>
              {editing ? "Save changes" : "Add customer"}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleting} onClose={() => setDeleting(null)} title="Delete customer">
        <p className="text-sm text-slate-600 dark:text-slate-300">
          Delete <strong>{deleting?.name}</strong>? This cannot be undone.
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
