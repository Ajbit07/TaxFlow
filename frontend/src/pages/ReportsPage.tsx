import { useState } from "react";
import { FileSpreadsheet, FileText } from "lucide-react";
import { reportApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Button, Card, Input } from "@/components/ui/primitives";
import { monthEndIso, monthStartIso } from "@/lib/utils";

const REPORTS = [
  { type: "sales", title: "Sales report", description: "GST invoices, purchases and transaction counts." },
  { type: "gst", title: "GST report", description: "Output GST, input tax credit and net payable (GSTR summary)." },
  { type: "expense", title: "Expense report", description: "Total spend with recent expense records." },
  { type: "inventory", title: "Inventory report", description: "Stock valuation and low-stock alerts." },
];

export default function ReportsPage() {
  const businessId = useBusinessId();
  const { toast } = useToast();
  const [from, setFrom] = useState(monthStartIso());
  const [to, setTo] = useState(monthEndIso());
  const [busy, setBusy] = useState<string | null>(null);

  async function download(kind: "pdf" | "excel", type: string) {
    setBusy(`${type}-${kind}`);
    try {
      if (kind === "pdf") {
        await reportApi.downloadPdf(businessId, type, from, to);
      } else {
        await reportApi.downloadExcel(businessId, type, from, to);
      }
    } catch (error) {
      toast(errorMessage(error), "error");
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Reports</h1>

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
        </div>
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        {REPORTS.map((report) => (
          <Card key={report.type}>
            <h3 className="font-semibold">{report.title}</h3>
            <p className="mb-4 mt-1 text-sm text-slate-500">{report.description}</p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                loading={busy === `${report.type}-pdf`}
                onClick={() => download("pdf", report.type)}
              >
                <FileText className="h-4 w-4" /> PDF
              </Button>
              <Button
                variant="outline"
                size="sm"
                loading={busy === `${report.type}-excel`}
                onClick={() => download("excel", report.type)}
              >
                <FileSpreadsheet className="h-4 w-4" /> Excel
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
