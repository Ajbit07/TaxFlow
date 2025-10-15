import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { FileText, Plus } from "lucide-react";
import { invoiceApi } from "@/api/endpoints";
import { useBusinessId } from "@/context/BusinessContext";
import { useDebounce } from "@/hooks/useDebounce";
import { Badge, Button, Card, EmptyState, Input, Pagination, Select, Skeleton } from "@/components/ui/primitives";
import { formatDate, formatMoney } from "@/lib/utils";
import type { InvoiceStatus, InvoiceType } from "@/types/api";

const STATUSES: InvoiceStatus[] = ["DRAFT", "SENT", "PAID", "PARTIALLY_PAID", "OVERDUE", "CANCELLED", "RECURRING"];
const TYPES: InvoiceType[] = ["GST_INVOICE", "PURCHASE_BILL", "CREDIT_NOTE", "DEBIT_NOTE"];

export default function InvoicesPage() {
  const businessId = useBusinessId();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<InvoiceStatus | "">("");
  const [type, setType] = useState<InvoiceType | "">("");
  const debouncedQuery = useDebounce(query);

  const { data, isLoading } = useQuery({
    queryKey: ["invoices", businessId, page, debouncedQuery, status, type],
    queryFn: () =>
      invoiceApi.list(businessId, {
        page,
        query: debouncedQuery || undefined,
        status: status || undefined,
        type: type || undefined,
      }),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Invoices</h1>
        <Link to="/invoices/new">
          <Button>
            <Plus className="h-4 w-4" /> New invoice
          </Button>
        </Link>
      </div>

      <Card>
        <div className="mb-4 flex flex-wrap gap-3">
          <Input
            placeholder="Search invoice number…"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setPage(0);
            }}
            className="max-w-xs"
          />
          <Select value={status} onChange={(e) => setStatus(e.target.value as InvoiceStatus | "")} className="max-w-44">
            <option value="">All statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replaceAll("_", " ")}
              </option>
            ))}
          </Select>
          <Select value={type} onChange={(e) => setType(e.target.value as InvoiceType | "")} className="max-w-44">
            <option value="">All types</option>
            {TYPES.map((t) => (
              <option key={t} value={t}>
                {t.replaceAll("_", " ")}
              </option>
            ))}
          </Select>
        </div>

        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<FileText className="h-10 w-10" />}
            title="No invoices found"
            subtitle="Create your first GST invoice — totals, CGST/SGST and stock update automatically."
            action={
              <Link to="/invoices/new">
                <Button>New invoice</Button>
              </Link>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-400 dark:border-slate-700">
                  <th className="py-2 pr-4">Invoice</th>
                  <th className="py-2 pr-4">Customer</th>
                  <th className="py-2 pr-4">Date</th>
                  <th className="py-2 pr-4">Due</th>
                  <th className="py-2 pr-4">Status</th>
                  <th className="py-2 pr-4 text-right">GST</th>
                  <th className="py-2 pr-4 text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((invoice) => (
                  <tr
                    key={invoice.id}
                    onClick={() => navigate(`/invoices/${invoice.id}`)}
                    className="cursor-pointer border-b border-slate-100 last:border-0 hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/50"
                  >
                    <td className="py-3 pr-4">
                      <p className="font-medium">{invoice.invoiceNumber}</p>
                      <p className="text-xs text-slate-400">{invoice.type.replaceAll("_", " ")}</p>
                    </td>
                    <td className="py-3 pr-4">{invoice.customerName}</td>
                    <td className="py-3 pr-4 text-slate-500">{formatDate(invoice.invoiceDate)}</td>
                    <td className="py-3 pr-4 text-slate-500">{formatDate(invoice.dueDate)}</td>
                    <td className="py-3 pr-4">
                      <Badge value={invoice.status} />
                    </td>
                    <td className="py-3 pr-4 text-right">{formatMoney(invoice.totalGst)}</td>
                    <td className="py-3 pr-4 text-right font-semibold">{formatMoney(invoice.totalAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </Card>
    </div>
  );
}
