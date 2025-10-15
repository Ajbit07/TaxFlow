import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Copy, Download, Pencil, Trash2 } from "lucide-react";
import { invoiceApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Badge, Button, Card, CardTitle, Modal, Skeleton } from "@/components/ui/primitives";
import { formatDate, formatMoney } from "@/lib/utils";

export default function InvoiceDetailPage() {
  const businessId = useBusinessId();
  const { invoiceId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [confirmDelete, setConfirmDelete] = useState(false);

  const { data: invoice, isLoading } = useQuery({
    queryKey: ["invoice", businessId, invoiceId],
    queryFn: () => invoiceApi.get(businessId, invoiceId!),
  });

  const duplicate = useMutation({
    mutationFn: () => invoiceApi.duplicate(businessId, invoiceId!),
    onSuccess: (copy) => {
      queryClient.invalidateQueries({ queryKey: ["invoices", businessId] });
      toast(`Duplicated as ${copy.invoiceNumber}`, "success");
      navigate(`/invoices/${copy.id}`);
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  const remove = useMutation({
    mutationFn: () => invoiceApi.remove(businessId, invoiceId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invoices", businessId] });
      toast("Invoice deleted", "success");
      navigate("/invoices");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  if (isLoading || !invoice) {
    return <Skeleton className="h-96" />;
  }

  const balance = invoice.totalAmount - invoice.paidAmount;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold">{invoice.invoiceNumber}</h1>
            <Badge value={invoice.status} />
          </div>
          <p className="text-sm text-slate-500">
            {invoice.type.replaceAll("_", " ")} • {formatDate(invoice.invoiceDate)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => invoiceApi.downloadPdf(businessId, invoice.id).catch((e) => toast(errorMessage(e), "error"))}>
            <Download className="h-4 w-4" /> PDF
          </Button>
          <Button variant="outline" onClick={() => duplicate.mutate()} loading={duplicate.isPending}>
            <Copy className="h-4 w-4" /> Duplicate
          </Button>
          <Link to={`/invoices/${invoice.id}/edit`}>
            <Button variant="outline">
              <Pencil className="h-4 w-4" /> Edit
            </Button>
          </Link>
          <Button variant="danger" onClick={() => setConfirmDelete(true)}>
            <Trash2 className="h-4 w-4" /> Delete
          </Button>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardTitle>Line items</CardTitle>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-700">
                  <th className="py-2 pr-4">Description</th>
                  <th className="py-2 pr-4">HSN</th>
                  <th className="py-2 pr-4 text-right">Qty</th>
                  <th className="py-2 pr-4 text-right">Rate</th>
                  <th className="py-2 pr-4 text-right">GST %</th>
                  <th className="py-2 pr-4 text-right">GST</th>
                  <th className="py-2 text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {invoice.lines.map((line) => (
                  <tr key={line.id} className="border-b border-slate-100 last:border-0 dark:border-slate-800">
                    <td className="py-3 pr-4 font-medium">{line.description}</td>
                    <td className="py-3 pr-4 text-slate-500">{line.hsnCode}</td>
                    <td className="py-3 pr-4 text-right">{line.quantity}</td>
                    <td className="py-3 pr-4 text-right">{formatMoney(line.unitPrice)}</td>
                    <td className="py-3 pr-4 text-right">{line.gstRate}%</td>
                    <td className="py-3 pr-4 text-right">{formatMoney(line.gstAmount)}</td>
                    <td className="py-3 text-right font-medium">{formatMoney(line.totalAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {invoice.notes && (
            <p className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500 dark:bg-slate-800/50">{invoice.notes}</p>
          )}
        </Card>

        <div className="space-y-4">
          <Card>
            <CardTitle>Summary</CardTitle>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-slate-500">Customer</dt>
                <dd className="font-medium">{invoice.customerName}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-500">Due date</dt>
                <dd>{formatDate(invoice.dueDate)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-500">Subtotal</dt>
                <dd>{formatMoney(invoice.subtotal)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-500">Total GST</dt>
                <dd>{formatMoney(invoice.totalGst)}</dd>
              </div>
              <div className="flex justify-between border-t border-slate-200 pt-2 text-base font-bold dark:border-slate-700">
                <dt>Grand total</dt>
                <dd>{formatMoney(invoice.totalAmount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-500">Paid</dt>
                <dd className="text-emerald-600">{formatMoney(invoice.paidAmount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-slate-500">Balance due</dt>
                <dd className={balance > 0 ? "font-semibold text-amber-600" : "font-semibold text-emerald-600"}>
                  {formatMoney(balance)}
                </dd>
              </div>
            </dl>
          </Card>
          {invoice.qrPayload && (
            <Card>
              <CardTitle>UPI payment link</CardTitle>
              <code className="block break-all rounded-lg bg-slate-50 p-3 text-xs dark:bg-slate-800/50">{invoice.qrPayload}</code>
            </Card>
          )}
        </div>
      </div>

      <Modal open={confirmDelete} onClose={() => setConfirmDelete(false)} title="Delete invoice">
        <p className="text-sm text-slate-600 dark:text-slate-300">
          Delete <strong>{invoice.invoiceNumber}</strong>? Stock changes from this invoice will be reversed.
        </p>
        <div className="mt-6 flex justify-end gap-2">
          <Button variant="outline" onClick={() => setConfirmDelete(false)}>
            Cancel
          </Button>
          <Button variant="danger" loading={remove.isPending} onClick={() => remove.mutate()}>
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}
