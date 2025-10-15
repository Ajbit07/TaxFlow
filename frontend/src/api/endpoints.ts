import { api, downloadFile } from "./client";
import type {
  ApiResponse,
  AuditEntry,
  AuthResponse,
  Business,
  BusinessRequest,
  ComplianceIssue,
  Customer,
  CustomerRequest,
  Dashboard,
  DocumentItem,
  DocumentType,
  Expense,
  ExpenseCategory,
  ExpenseRequest,
  Filing,
  FilingWizardRequest,
  FinancialHealth,
  GstSummary,
  IncomeTaxRequest,
  IncomeTaxResponse,
  InventoryMovementRequest,
  Invoice,
  InvoiceRequest,
  InvoiceStatus,
  InvoiceType,
  NotificationItem,
  PageResponse,
  Product,
  ProductRequest,
  Recommendation,
  SearchResult,
  TaskScope,
  UserProfile,
} from "@/types/api";

type Page = { page?: number; size?: number; sort?: string };

function pageParams(page?: Page): Record<string, unknown> {
  return { page: page?.page ?? 0, size: page?.size ?? 20, ...(page?.sort ? { sort: page.sort } : {}) };
}

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data } = await promise;
  return data.data;
}

// ---- Auth ----
export const authApi = {
  register: (body: { email: string; password: string; fullName: string; phone?: string }) =>
    unwrap<AuthResponse>(api.post("/auth/register", body)),
  login: (body: { email: string; password: string }) => unwrap<AuthResponse>(api.post("/auth/login", body)),
  logout: (refreshToken: string) => unwrap<void>(api.post("/auth/logout", { refreshToken })),
  forgotPassword: (email: string) => unwrap<{ resetToken: string }>(api.post("/auth/forgot-password", { email })),
  resetPassword: (body: { token: string; newPassword: string }) => unwrap<void>(api.post("/auth/reset-password", body)),
  verifyEmail: (token: string) => unwrap<void>(api.post("/auth/verify-email", null, { params: { token } })),
  me: () => unwrap<UserProfile>(api.get("/account/me")),
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    unwrap<void>(api.put("/account/password", body)),
};

// ---- Businesses ----
export const businessApi = {
  mine: () => unwrap<Business[]>(api.get("/businesses")),
  create: (body: BusinessRequest) => unwrap<Business>(api.post("/businesses", body)),
  update: (id: string, body: BusinessRequest) => unwrap<Business>(api.put(`/businesses/${id}`, body)),
  assignEmployee: (id: string, body: { email: string; scopes: TaskScope[] }) =>
    unwrap<void>(api.post(`/businesses/${id}/members`, body)),
};

// ---- Customers ----
export const customerApi = {
  list: (businessId: string, params: { query?: string } & Page) =>
    unwrap<PageResponse<Customer>>(
      api.get(`/businesses/${businessId}/customers`, { params: { ...pageParams(params), query: params.query } }),
    ),
  search: (businessId: string, query: string) =>
    unwrap<Customer[]>(api.get(`/businesses/${businessId}/customers/search`, { params: { query } })),
  create: (businessId: string, body: CustomerRequest) =>
    unwrap<Customer>(api.post(`/businesses/${businessId}/customers`, body)),
  update: (businessId: string, id: string, body: CustomerRequest) =>
    unwrap<Customer>(api.put(`/businesses/${businessId}/customers/${id}`, body)),
  remove: (businessId: string, id: string) => unwrap<void>(api.delete(`/businesses/${businessId}/customers/${id}`)),
};

// ---- Products ----
export const productApi = {
  list: (businessId: string, params: { query?: string; category?: string } & Page) =>
    unwrap<PageResponse<Product>>(
      api.get(`/businesses/${businessId}/products`, {
        params: { ...pageParams(params), query: params.query, category: params.category },
      }),
    ),
  lowStock: (businessId: string) => unwrap<Product[]>(api.get(`/businesses/${businessId}/products/low-stock`)),
  create: (businessId: string, body: ProductRequest) =>
    unwrap<Product>(api.post(`/businesses/${businessId}/products`, body)),
  update: (businessId: string, id: string, body: ProductRequest) =>
    unwrap<Product>(api.put(`/businesses/${businessId}/products/${id}`, body)),
  remove: (businessId: string, id: string) => unwrap<void>(api.delete(`/businesses/${businessId}/products/${id}`)),
  move: (businessId: string, body: InventoryMovementRequest) =>
    unwrap<unknown>(api.post(`/businesses/${businessId}/products/movements`, body)),
};

// ---- Invoices ----
export const invoiceApi = {
  list: (
    businessId: string,
    params: { query?: string; status?: InvoiceStatus; type?: InvoiceType; from?: string; to?: string } & Page,
  ) =>
    unwrap<PageResponse<Invoice>>(
      api.get(`/businesses/${businessId}/invoices`, { params: { ...pageParams(params), ...params } }),
    ),
  get: (businessId: string, id: string) => unwrap<Invoice>(api.get(`/businesses/${businessId}/invoices/${id}`)),
  nextNumber: (businessId: string) =>
    unwrap<{ invoiceNumber: string }>(api.get(`/businesses/${businessId}/invoices/next-number`)),
  create: (businessId: string, body: InvoiceRequest) =>
    unwrap<Invoice>(api.post(`/businesses/${businessId}/invoices`, body)),
  update: (businessId: string, id: string, body: InvoiceRequest) =>
    unwrap<Invoice>(api.put(`/businesses/${businessId}/invoices/${id}`, body)),
  duplicate: (businessId: string, id: string) =>
    unwrap<Invoice>(api.post(`/businesses/${businessId}/invoices/${id}/duplicate`)),
  remove: (businessId: string, id: string) => unwrap<void>(api.delete(`/businesses/${businessId}/invoices/${id}`)),
  downloadPdf: (businessId: string, id: string) =>
    downloadFile(`/businesses/${businessId}/invoices/${id}/pdf`, `invoice-${id}.pdf`),
};

// ---- Expenses ----
export const expenseApi = {
  list: (businessId: string, params: { category?: ExpenseCategory; from?: string; to?: string } & Page) =>
    unwrap<PageResponse<Expense>>(
      api.get(`/businesses/${businessId}/expenses`, { params: { ...pageParams(params), ...params } }),
    ),
  create: (businessId: string, body: ExpenseRequest) =>
    unwrap<Expense>(api.post(`/businesses/${businessId}/expenses`, body)),
  update: (businessId: string, id: string, body: ExpenseRequest) =>
    unwrap<Expense>(api.put(`/businesses/${businessId}/expenses/${id}`, body)),
  remove: (businessId: string, id: string) => unwrap<void>(api.delete(`/businesses/${businessId}/expenses/${id}`)),
};

// ---- Documents ----
export const documentApi = {
  list: (businessId: string, params: Page) =>
    unwrap<PageResponse<DocumentItem>>(api.get(`/businesses/${businessId}/documents`, { params: pageParams(params) })),
  upload: (businessId: string, documentType: DocumentType, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return unwrap<DocumentItem>(
      api.post(`/businesses/${businessId}/documents`, form, {
        params: { documentType },
        headers: { "Content-Type": "multipart/form-data" },
      }),
    );
  },
  download: (businessId: string, id: string, fileName: string) =>
    downloadFile(`/businesses/${businessId}/documents/${id}/download`, fileName),
  remove: (businessId: string, id: string) => unwrap<void>(api.delete(`/businesses/${businessId}/documents/${id}`)),
};

// ---- Tax ----
export const taxApi = {
  gstSummary: (businessId: string, params?: { from?: string; to?: string }) =>
    unwrap<GstSummary>(api.get(`/businesses/${businessId}/tax/gst-summary`, { params })),
  incomeTax: (businessId: string, body: IncomeTaxRequest) =>
    unwrap<IncomeTaxResponse>(api.post(`/businesses/${businessId}/tax/income-tax`, body)),
  recommendations: (businessId: string, from: string, to: string) =>
    unwrap<Recommendation[]>(api.get(`/businesses/${businessId}/tax/recommendations`, { params: { from, to } })),
  compliance: (businessId: string) => unwrap<ComplianceIssue[]>(api.get(`/businesses/${businessId}/tax/compliance`)),
  health: (businessId: string) => unwrap<FinancialHealth>(api.get(`/businesses/${businessId}/tax/health`)),
  filings: (businessId: string, params: Page) =>
    unwrap<PageResponse<Filing>>(api.get(`/businesses/${businessId}/tax/filings`, { params: pageParams(params) })),
  saveWizard: (businessId: string, body: FilingWizardRequest) =>
    unwrap<Filing>(api.post(`/businesses/${businessId}/tax/filings/wizard`, body)),
  submitFiling: (businessId: string, filingId: string) =>
    unwrap<Filing>(api.post(`/businesses/${businessId}/tax/filings/${filingId}/submit`)),
};

// ---- Dashboard / Reports / Notifications / Search / Audit ----
export const dashboardApi = {
  get: (businessId: string) => unwrap<Dashboard>(api.get(`/businesses/${businessId}/dashboard`)),
};

export const reportApi = {
  downloadPdf: (businessId: string, reportType: string, from?: string, to?: string) =>
    downloadFile(
      `/businesses/${businessId}/reports/${reportType}/pdf${from && to ? `?from=${from}&to=${to}` : ""}`,
      `${reportType}-report.pdf`,
    ),
  downloadExcel: (businessId: string, reportType: string, from?: string, to?: string) =>
    downloadFile(
      `/businesses/${businessId}/reports/${reportType}/excel${from && to ? `?from=${from}&to=${to}` : ""}`,
      `${reportType}-report.xlsx`,
    ),
};

export const notificationApi = {
  list: (businessId: string, params: Page) =>
    unwrap<PageResponse<NotificationItem>>(
      api.get(`/businesses/${businessId}/notifications`, { params: pageParams(params) }),
    ),
  unreadCount: (businessId: string) =>
    unwrap<{ unread: number }>(api.get(`/businesses/${businessId}/notifications/unread-count`)),
  markRead: (businessId: string, id: string) =>
    unwrap<NotificationItem>(api.put(`/businesses/${businessId}/notifications/${id}/read`)),
};

export const searchApi = {
  global: (businessId: string, q: string) =>
    unwrap<SearchResult[]>(api.get(`/businesses/${businessId}/search`, { params: { q } })),
};

export const auditApi = {
  list: (businessId: string, params: Page) =>
    unwrap<PageResponse<AuditEntry>>(api.get(`/businesses/${businessId}/audit`, { params: pageParams(params) })),
};
