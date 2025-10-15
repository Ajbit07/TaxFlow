import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Banknote,
  Bell,
  Calculator,
  FileText,
  FolderOpen,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Package,
  Receipt,
  Search,
  Settings,
  Sun,
  Users,
  X,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useBusiness } from "@/context/BusinessContext";
import { useTheme } from "@/context/ThemeContext";
import { notificationApi, searchApi } from "@/api/endpoints";
import { useDebounce } from "@/hooks/useDebounce";
import { cn } from "@/lib/utils";
import { Select } from "@/components/ui/primitives";

const navItems = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/invoices", label: "Invoices", icon: FileText },
  { to: "/customers", label: "Customers", icon: Users },
  { to: "/products", label: "Products", icon: Package },
  { to: "/expenses", label: "Expenses", icon: Receipt },
  { to: "/gst", label: "GST Filing", icon: Banknote },
  { to: "/income-tax", label: "Income Tax", icon: Calculator },
  { to: "/reports", label: "Reports", icon: FolderOpen },
  { to: "/settings", label: "Settings", icon: Settings },
];

const breadcrumbNames: Record<string, string> = {
  dashboard: "Dashboard",
  invoices: "Invoices",
  customers: "Customers",
  products: "Products",
  expenses: "Expenses",
  gst: "GST Filing",
  "income-tax": "Income Tax",
  reports: "Reports",
  settings: "Settings",
  notifications: "Notifications",
  new: "New",
  edit: "Edit",
};

export default function AppLayout() {
  const { user, logout } = useAuth();
  const { businesses, business, selectBusiness } = useBusiness();
  const { dark, toggle } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const debouncedSearch = useDebounce(searchTerm);
  const searchRef = useRef<HTMLDivElement>(null);

  const { data: unread } = useQuery({
    queryKey: ["unread", business?.id],
    queryFn: () => notificationApi.unreadCount(business!.id),
    enabled: !!business,
    refetchInterval: 60_000,
  });

  const { data: results = [] } = useQuery({
    queryKey: ["search", business?.id, debouncedSearch],
    queryFn: () => searchApi.global(business!.id, debouncedSearch),
    enabled: !!business && debouncedSearch.length >= 2,
  });

  useEffect(() => {
    const onClick = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) setSearchOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  useEffect(() => setSidebarOpen(false), [location.pathname]);

  const crumbs = location.pathname.split("/").filter(Boolean);

  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 w-64 transform border-r border-slate-200 bg-white transition-transform",
          "dark:border-slate-800 dark:bg-slate-900 lg:static lg:translate-x-0",
          sidebarOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex h-16 items-center border-b border-slate-200 px-5 dark:border-slate-800">
          <span className="font-display text-xl font-bold tracking-tight">TaxFlow</span>
          <button className="ml-auto lg:hidden" onClick={() => setSidebarOpen(false)}>
            <X className="h-5 w-5" />
          </button>
        </div>
        <nav className="space-y-1 p-3">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 border-l-2 px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "border-brand-600 bg-brand-50/60 text-brand-800 dark:border-brand-400 dark:bg-brand-900/20 dark:text-brand-200"
                    : "border-transparent text-slate-600 hover:border-slate-300 hover:bg-slate-100/70 dark:text-slate-300 dark:hover:bg-slate-800/60",
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
        {businesses.length > 0 && (
          <div className="border-t border-slate-200 p-3 dark:border-slate-800">
            <p className="mb-1 px-1 text-[10px] font-semibold uppercase tracking-wide text-slate-400">Business</p>
            <Select value={business?.id ?? ""} onChange={(e) => selectBusiness(e.target.value)}>
              {businesses.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.businessName}
                </option>
              ))}
            </Select>
          </div>
        )}
      </aside>

      {/* Main */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/80 px-4 backdrop-blur dark:border-slate-800 dark:bg-slate-900/80">
          <button className="lg:hidden" onClick={() => setSidebarOpen(true)}>
            <Menu className="h-5 w-5" />
          </button>

          {/* Global search */}
          <div ref={searchRef} className="relative w-full max-w-md">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setSearchOpen(true);
              }}
              onFocus={() => setSearchOpen(true)}
              placeholder="Search invoices, customers, products…"
              className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 pl-9 pr-3 text-sm focus:border-brand-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800"
            />
            {searchOpen && results.length > 0 && (
              <div className="absolute mt-1 w-full overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg dark:border-slate-700 dark:bg-slate-900">
                {results.map((r) => (
                  <button
                    key={`${r.type}-${r.id}`}
                    className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-slate-50 dark:hover:bg-slate-800"
                    onClick={() => {
                      setSearchOpen(false);
                      setSearchTerm("");
                      navigate(r.url);
                    }}
                  >
                    <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-500 dark:bg-slate-800">
                      {r.type}
                    </span>
                    <span className="truncate font-medium">{r.title}</span>
                    <span className="ml-auto truncate text-xs text-slate-400">{r.subtitle}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="ml-auto flex items-center gap-1">
            <button
              onClick={toggle}
              className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800"
              title="Toggle theme"
            >
              {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>
            <Link
              to="/notifications"
              className="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800"
            >
              <Bell className="h-4 w-4" />
              {(unread?.unread ?? 0) > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[9px] font-bold text-white">
                  {unread!.unread}
                </span>
              )}
            </Link>
            <div className="mx-2 hidden text-right sm:block">
              <p className="text-xs font-semibold">{user?.fullName}</p>
              <p className="text-[10px] text-slate-400">{user?.role.replaceAll("_", " ")}</p>
            </div>
            <button
              onClick={() => logout().then(() => navigate("/login"))}
              className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-600 dark:text-slate-400 dark:hover:bg-rose-900/20"
              title="Log out"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </header>

        {/* Breadcrumbs */}
        <div className="flex items-center gap-1 px-6 pt-4 text-xs text-slate-400">
          <Link to="/dashboard" className="hover:text-brand-600">
            Home
          </Link>
          {crumbs.map((crumb, index) => (
            <span key={index} className="flex items-center gap-1">
              <span>/</span>
              <span className={index === crumbs.length - 1 ? "font-medium text-slate-600 dark:text-slate-300" : ""}>
                {breadcrumbNames[crumb] ?? crumb}
              </span>
            </span>
          ))}
        </div>

        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
