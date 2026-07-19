import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle, Package, Plus } from "lucide-react";
import { productApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useDebounce } from "@/hooks/useDebounce";
import { useToast } from "@/components/ui/toast";
import { Button, Card, EmptyState, Field, Input, Modal, Pagination, Skeleton } from "@/components/ui/primitives";
import { formatMoney } from "@/lib/utils";
import type { Product } from "@/types/api";

const schema = z.object({
  name: z.string().min(2, "Name required"),
  category: z.string().min(1, "Category required"),
  sku: z.string().min(1, "SKU required"),
  hsnCode: z.string().min(2, "HSN required").max(8, "Max 8 characters"),
  gstPercentage: z.coerce.number().min(0).max(28),
  stock: z.coerce.number().min(0),
  lowStockThreshold: z.coerce.number().min(0),
  purchasePrice: z.coerce.number().min(0),
  sellingPrice: z.coerce.number().min(0),
  barcode: z.string().optional(),
  expiryDate: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export default function ProductsPage() {
  const businessId = useBusinessId();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [deleting, setDeleting] = useState<Product | null>(null);
  const debouncedQuery = useDebounce(query);

  const { data, isLoading } = useQuery({
    queryKey: ["products", businessId, page, debouncedQuery],
    queryFn: () => productApi.list(businessId, { page, query: debouncedQuery || undefined }),
  });

  const { data: lowStock = [] } = useQuery({
    queryKey: ["low-stock", businessId],
    queryFn: () => productApi.lowStock(businessId),
  });

  const form = useForm<FormValues>({ resolver: zodResolver(schema) });
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["products", businessId] });
    queryClient.invalidateQueries({ queryKey: ["low-stock", businessId] });
  };

  const save = useMutation({
    mutationFn: (values: FormValues) => {
      const body = { ...values, barcode: values.barcode || null, expiryDate: values.expiryDate || null };
      return editing ? productApi.update(businessId, editing.id, body) : productApi.create(businessId, body);
    },
    onSuccess: () => {
      invalidate();
      toast(editing ? "Product updated" : "Product created", "success");
      setModalOpen(false);
      setEditing(null);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const remove = useMutation({
    mutationFn: (id: string) => productApi.remove(businessId, id),
    onSuccess: () => {
      invalidate();
      toast("Product deleted", "success");
      setDeleting(null);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  function openModal(product?: Product) {
    setEditing(product ?? null);
    form.reset(
      product
        ? {
            name: product.name,
            category: product.category,
            sku: product.sku,
            hsnCode: product.hsnCode,
            gstPercentage: product.gstPercentage,
            stock: product.stock,
            lowStockThreshold: product.lowStockThreshold,
            purchasePrice: product.purchasePrice,
            sellingPrice: product.sellingPrice,
            barcode: product.barcode ?? "",
            expiryDate: product.expiryDate ?? "",
          }
        : {
            name: "",
            category: "",
            sku: "",
            hsnCode: "",
            gstPercentage: 18,
            stock: 0,
            lowStockThreshold: 5,
            purchasePrice: 0,
            sellingPrice: 0,
            barcode: "",
            expiryDate: "",
          },
    );
    setModalOpen(true);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Products</h1>
        <Button onClick={() => openModal()}>
          <Plus className="h-4 w-4" /> Add product
        </Button>
      </div>

      {lowStock.length > 0 && (
        <Card className="border-amber-300 bg-amber-50">
          <div className="flex items-center gap-2 text-sm text-amber-700">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            <span>
              <strong>{lowStock.length}</strong> product{lowStock.length > 1 ? "s are" : " is"} low on stock:{" "}
              {lowStock
                .slice(0, 5)
                .map((p) => p.name)
                .join(", ")}
              {lowStock.length > 5 ? "…" : ""}
            </span>
          </div>
        </Card>
      )}

      <Card>
        <Input
          placeholder="Search products by name…"
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
            icon={<Package className="h-10 w-10" />}
            title="No products yet"
            subtitle="Add products with HSN codes and GST rates to speed up invoicing."
            action={<Button onClick={() => openModal()}>Add product</Button>}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400">
                  <th className="py-2 pr-4">Product</th>
                  <th className="py-2 pr-4">SKU / HSN</th>
                  <th className="py-2 pr-4 text-right">GST %</th>
                  <th className="py-2 pr-4 text-right">Stock</th>
                  <th className="py-2 pr-4 text-right">Buy</th>
                  <th className="py-2 pr-4 text-right">Sell</th>
                  <th className="py-2 pr-4 text-right">Margin</th>
                  <th className="py-2" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((product) => (
                  <tr key={product.id} className="border-b border-slate-100 last:border-0">
                    <td className="py-3 pr-4">
                      <p className="font-medium">{product.name}</p>
                      <p className="text-xs text-slate-400">{product.category}</p>
                    </td>
                    <td className="py-3 pr-4 text-slate-500">
                      {product.sku} / {product.hsnCode}
                    </td>
                    <td className="py-3 pr-4 text-right">{product.gstPercentage}%</td>
                    <td className="py-3 pr-4 text-right">
                      <span className={product.lowStock ? "font-semibold text-amber-600" : ""}>{product.stock}</span>
                    </td>
                    <td className="py-3 pr-4 text-right">{formatMoney(product.purchasePrice)}</td>
                    <td className="py-3 pr-4 text-right">{formatMoney(product.sellingPrice)}</td>
                    <td className="py-3 pr-4 text-right">{product.grossMargin}%</td>
                    <td className="py-3 text-right">
                      <Button variant="ghost" size="sm" onClick={() => openModal(product)}>
                        Edit
                      </Button>
                      <Button variant="ghost" size="sm" className="text-rose-500" onClick={() => setDeleting(product)}>
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

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? "Edit product" : "Add product"} wide>
        <form onSubmit={form.handleSubmit((values) => save.mutate(values))} className="grid gap-4 sm:grid-cols-3">
          <Field label="Name" error={form.formState.errors.name?.message}>
            <Input {...form.register("name")} />
          </Field>
          <Field label="Category" error={form.formState.errors.category?.message}>
            <Input placeholder="Grocery" {...form.register("category")} />
          </Field>
          <Field label="SKU" error={form.formState.errors.sku?.message}>
            <Input placeholder="SKU-001" {...form.register("sku")} />
          </Field>
          <Field label="HSN/SAC code" error={form.formState.errors.hsnCode?.message}>
            <Input placeholder="0910" {...form.register("hsnCode")} />
          </Field>
          <Field label="GST %" error={form.formState.errors.gstPercentage?.message}>
            <Input type="number" step="0.01" {...form.register("gstPercentage")} />
          </Field>
          <Field label="Stock" error={form.formState.errors.stock?.message}>
            <Input type="number" step="0.001" {...form.register("stock")} />
          </Field>
          <Field label="Low stock alert at" error={form.formState.errors.lowStockThreshold?.message}>
            <Input type="number" step="0.001" {...form.register("lowStockThreshold")} />
          </Field>
          <Field label="Purchase price (₹)" error={form.formState.errors.purchasePrice?.message}>
            <Input type="number" step="0.01" {...form.register("purchasePrice")} />
          </Field>
          <Field label="Selling price (₹)" error={form.formState.errors.sellingPrice?.message}>
            <Input type="number" step="0.01" {...form.register("sellingPrice")} />
          </Field>
          <Field label="Barcode (optional)" error={form.formState.errors.barcode?.message}>
            <Input {...form.register("barcode")} />
          </Field>
          <Field label="Expiry date (optional)" error={form.formState.errors.expiryDate?.message}>
            <Input type="date" {...form.register("expiryDate")} />
          </Field>
          <div className="flex items-end justify-end gap-2 sm:col-span-3">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={save.isPending}>
              {editing ? "Save changes" : "Add product"}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleting} onClose={() => setDeleting(null)} title="Delete product">
        <p className="text-sm text-slate-600">
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
