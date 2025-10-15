import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ArrowDownRight, ArrowUpRight, IndianRupee, PiggyBank, ReceiptText, Wallet } from "lucide-react";
import { dashboardApi } from "@/api/endpoints";
import { useBusinessId } from "@/context/BusinessContext";
import { Badge, Card, CardTitle, Skeleton } from "@/components/ui/primitives";
import { formatMoney } from "@/lib/utils";

const PIE_COLORS = ["#28706b", "#4fa9a0", "#b45309", "#7c6f64", "#3b5b92", "#9a3f3f", "#64748b"];

function StatCard({ title, value, icon, positive }: { title: string; value: string; icon: React.ReactNode; positive?: boolean }) {
  return (
    <Card className="flex items-center gap-4">
      <div className="rounded-xl bg-brand-50 p-3 text-brand-600 dark:bg-brand-900/30">{icon}</div>
      <div className="min-w-0">
        <p className="truncate text-xs text-slate-500">{title}</p>
        <p className="truncate text-lg font-bold">{value}</p>
      </div>
      {positive !== undefined && (
        <div className={positive ? "ml-auto text-emerald-500" : "ml-auto text-rose-500"}>
          {positive ? <ArrowUpRight className="h-4 w-4" /> : <ArrowDownRight className="h-4 w-4" />}
        </div>
      )}
    </Card>
  );
}

export default function DashboardPage() {
  const businessId = useBusinessId();
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", businessId],
    queryFn: () => dashboardApi.get(businessId),
  });

  if (isLoading || !data) {
    return (
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-24" />
        ))}
      </div>
    );
  }

  const revenueSeries = (data.charts.revenue ?? []).map((value, index) => ({
    name: `M${index + 1}`,
    revenue: value,
  }));
  const gstSeries = [
    { name: "Input GST", value: data.charts.gst?.[0] ?? 0 },
    { name: "Output GST", value: data.charts.gst?.[1] ?? 0 },
    { name: "Net payable", value: data.charts.gst?.[2] ?? 0 },
  ];
  const expenseSeries = (data.charts.expenses ?? []).map((value, index) => ({ name: `Cat ${index + 1}`, value }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <div className="flex items-center gap-2 text-sm">
          <span className="text-slate-500">Financial health</span>
          <span
            className={
              data.healthScore >= 80
                ? "rounded-full bg-emerald-100 px-3 py-1 font-bold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300"
                : data.healthScore >= 50
                  ? "rounded-full bg-amber-100 px-3 py-1 font-bold text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"
                  : "rounded-full bg-rose-100 px-3 py-1 font-bold text-rose-700 dark:bg-rose-900/40 dark:text-rose-300"
            }
          >
            {data.healthScore}/100
          </span>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard title="Monthly sales" value={formatMoney(data.metrics.monthlySales)} icon={<IndianRupee className="h-5 w-5" />} positive />
        <StatCard title="Monthly expenses" value={formatMoney(data.metrics.expenses)} icon={<ReceiptText className="h-5 w-5" />} positive={false} />
        <StatCard title="Profit" value={formatMoney(data.metrics.profit)} icon={<PiggyBank className="h-5 w-5" />} positive={data.metrics.profit >= 0} />
        <StatCard title="GST payable" value={formatMoney(data.metrics.monthlyGst)} icon={<Wallet className="h-5 w-5" />} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardTitle>Revenue trend</CardTitle>
          <div className="h-64">
            <ResponsiveContainer>
              <AreaChart data={revenueSeries}>
                <defs>
                  <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#28706b" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#28706b" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#33415522" />
                <XAxis dataKey="name" fontSize={11} />
                <YAxis fontSize={11} tickFormatter={(v: number) => `${Math.round(v / 1000)}k`} />
                <Tooltip formatter={(value) => formatMoney(Number(value))} />
                <Area type="monotone" dataKey="revenue" stroke="#28706b" fill="url(#rev)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card>
          <CardTitle>GST position</CardTitle>
          <div className="h-64">
            <ResponsiveContainer>
              <BarChart data={gstSeries}>
                <CartesianGrid strokeDasharray="3 3" stroke="#33415522" />
                <XAxis dataKey="name" fontSize={10} />
                <YAxis fontSize={11} tickFormatter={(v: number) => `${Math.round(v / 1000)}k`} />
                <Tooltip formatter={(value) => formatMoney(Number(value))} />
                <Bar dataKey="value" fill="#28706b" radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <CardTitle>Expense breakdown</CardTitle>
          {expenseSeries.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-400">No expenses recorded this month.</p>
          ) : (
            <div className="h-56">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={expenseSeries} dataKey="value" nameKey="name" innerRadius={45} outerRadius={80} paddingAngle={3}>
                    {expenseSeries.map((_, index) => (
                      <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatMoney(Number(value))} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card>
          <CardTitle>Insights</CardTitle>
          <ul className="space-y-3">
            {data.insights.map((insight) => (
              <li key={insight.title} className="rounded-lg border border-slate-100 p-3 dark:border-slate-800">
                <div className="flex items-center justify-between">
                  <p className="text-xs font-semibold text-slate-500">{insight.title}</p>
                  <Badge value={insight.severity} />
                </div>
                <p className="mt-1 text-sm font-bold">{insight.value}</p>
                <p className="text-xs text-slate-400">{insight.explanation}</p>
              </li>
            ))}
          </ul>
        </Card>

        <Card>
          <CardTitle>Key numbers</CardTitle>
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Today's sales</dt>
              <dd className="font-semibold">{formatMoney(data.metrics.todaysSales)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Stock value</dt>
              <dd className="font-semibold">{formatMoney(data.stockValue)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Cash flow</dt>
              <dd className="font-semibold">{formatMoney(data.cashFlow)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Pending invoices</dt>
              <dd className="font-semibold">{data.pendingInvoices}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Pending filings</dt>
              <dd className="font-semibold">{data.pendingFilings}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Avg. order value</dt>
              <dd className="font-semibold">{formatMoney(data.kpis.averageOrderValue)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Gross margin</dt>
              <dd className="font-semibold">{data.kpis.grossMargin}%</dd>
            </div>
          </dl>
          <Link to="/reports" className="mt-4 block text-center text-xs font-medium text-brand-600 hover:underline">
            View full reports →
          </Link>
        </Card>
      </div>
    </div>
  );
}
