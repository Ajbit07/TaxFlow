// Mirrors backend DTOs in com.taxflow.application.dto.TaxFlowDtos

export type UserRole = "BUSINESS_OWNER" | "EMPLOYEE" | "ADMIN";

export type BusinessType =
  | "KIRANA_STORE"
  | "SHOPKEEPER"
  | "FREELANCER"
  | "STARTUP"
  | "MSME"
  | "RESTAURANT"
  | "TRADER"
  | "PROPRIETORSHIP"
  | "PARTNERSHIP"
  | "LLP"
  | "PRIVATE_LIMITED"
  | "OTHER";

export type InvoiceType = "GST_INVOICE" | "PURCHASE_BILL" | "CREDIT_NOTE" | "DEBIT_NOTE";

export type InvoiceStatus =
  | "DRAFT"
  | "SENT"
  | "PAID"
  | "PARTIALLY_PAID"
  | "OVERDUE"
  | "CANCELLED"
  | "RECURRING";

export type ExpenseCategory =
  | "FUEL"
  | "RENT"
  | "ELECTRICITY"
  | "SALARY"
  | "MAINTENANCE"
  | "TRAVEL"
  | "FOOD"
  | "OFFICE"
  | "BILLS"
  | "OTHER";

export type DocumentType =
  | "PAN"
  | "AADHAAR"
  | "GST_CERTIFICATE"
  | "CANCELLED_CHEQUE"
  | "BILL"
  | "INVOICE"
  | "RECEIPT"
  | "PDF"
  | "IMAGE"
  | "OTHER";

export type FilingType = "GST_ANNUAL" | "ITR";
export type FilingStatus = "DRAFT" | "READY_FOR_REVIEW" | "BLOCKED" | "SUBMITTED" | "FILED";
export type RuleSeverity = "INFO" | "WARNING" | "ERROR";
export type NotificationType =
  | "LOW_STOCK"
  | "TAX_DUE"
  | "GST_DUE"
  | "INVOICE_PAID"
  | "INVOICE_OVERDUE"
  | "DOCUMENT_MISSING"
  | "MONTHLY_SUMMARY"
  | "SYSTEM";

export type TaskScope =
  | "DASHBOARD"
  | "INVOICE"
  | "CUSTOMER"
  | "PRODUCT"
  | "INVENTORY"
  | "EXPENSE"
  | "GST"
  | "INCOME_TAX"
  | "DOCUMENT"
  | "REPORT"
  | "CALENDAR"
  | "NOTIFICATION"
  | "SETTINGS"
  | "ADMIN";

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AuthResponse {
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  accessToken: string;
  refreshToken: string;
  emailVerified: boolean;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
}

export interface Business {
  id: string;
  ownerId: string;
  gstin: string | null;
  pan: string;
  businessName: string;
  ownerName: string;
  address: string;
  phone: string;
  email: string;
  state: string;
  businessType: BusinessType;
  financialYear: string;
  currency: string;
  language: string;
  darkMode: boolean;
}

export interface BusinessRequest {
  gstin?: string | null;
  pan: string;
  businessName: string;
  ownerName: string;
  address: string;
  phone: string;
  email: string;
  state: string;
  businessType: BusinessType;
  financialYear: string;
  currency?: string;
  language?: string;
  darkMode: boolean;
}

export interface Customer {
  id: string;
  name: string;
  gstin: string | null;
  pan: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  outstandingBalance: number;
  lifetimeSales: number;
  invoiceCount: number;
}

export interface CustomerRequest {
  name: string;
  gstin?: string | null;
  pan?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  openingBalance?: number | null;
}

export interface Product {
  id: string;
  name: string;
  category: string;
  sku: string;
  hsnCode: string;
  gstPercentage: number;
  stock: number;
  lowStockThreshold: number;
  purchasePrice: number;
  sellingPrice: number;
  barcode: string | null;
  expiryDate: string | null;
  lowStock: boolean;
  inventoryValue: number;
  grossMargin: number;
}

export interface ProductRequest {
  name: string;
  category: string;
  sku: string;
  hsnCode: string;
  gstPercentage: number;
  stock: number;
  lowStockThreshold: number;
  purchasePrice: number;
  sellingPrice: number;
  barcode?: string | null;
  expiryDate?: string | null;
}

export interface InvoiceLine {
  id: string;
  productId: string | null;
  description: string;
  hsnCode: string;
  quantity: number;
  unitPrice: number;
  gstRate: number;
  taxableAmount: number;
  gstAmount: number;
  totalAmount: number;
}

export interface InvoiceLineRequest {
  productId?: string | null;
  description: string;
  hsnCode: string;
  quantity: number;
  unitPrice: number;
  gstRate: number;
}

export interface Invoice {
  id: string;
  businessId: string;
  customerId: string | null;
  customerName: string;
  invoiceNumber: string;
  type: InvoiceType;
  status: InvoiceStatus;
  invoiceDate: string;
  dueDate: string | null;
  recurring: boolean;
  recurrencePattern: string | null;
  templateName: string;
  subtotal: number;
  totalGst: number;
  totalAmount: number;
  paidAmount: number;
  notes: string | null;
  qrPayload: string | null;
  lines: InvoiceLine[];
  createdAt: string;
}

export interface InvoiceRequest {
  customerId?: string | null;
  invoiceNumber?: string | null;
  type: InvoiceType;
  status: InvoiceStatus;
  invoiceDate: string;
  dueDate?: string | null;
  recurring: boolean;
  recurrencePattern?: string | null;
  templateName?: string | null;
  paidAmount?: number | null;
  notes?: string | null;
  lines: InvoiceLineRequest[];
}

export interface Expense {
  id: string;
  category: ExpenseCategory;
  vendor: string;
  amount: number;
  gstAmount: number;
  expenseDate: string;
  receiptUrl: string | null;
  description: string | null;
}

export interface ExpenseRequest {
  category: ExpenseCategory;
  vendor: string;
  amount: number;
  gstAmount: number;
  expenseDate: string;
  receiptUrl?: string | null;
  description?: string | null;
}

export interface DocumentItem {
  id: string;
  documentType: DocumentType;
  fileName: string;
  contentType: string;
  storagePath: string;
  fileSize: number;
  version: number;
  status: string;
  extractedFieldsJson: string | null;
  createdAt: string;
}

export interface GstSummary {
  outputGst: number;
  inputGst: number;
  itcAvailable: number;
  netGstPayable: number;
  lateFee: number;
  interest: number;
  reminders: string[];
}

export interface IncomeTaxRequest {
  businessIncome: number;
  salaryIncome: number;
  otherIncome: number;
  investments: number;
  deductions: number;
  businessExpenses: number;
}

export interface IncomeTaxResponse {
  taxableIncome: number;
  estimatedTax: number;
  advanceTaxDue: number;
  taxSavingSuggestions: string[];
  profitAndLoss: Record<string, number>;
}

export interface Filing {
  id: string;
  filingType: FilingType;
  status: FilingStatus;
  periodStart: string;
  periodEnd: string;
  progressPercent: number;
  taxDue: number;
  lateFee: number;
  summaryJson: string | null;
  submittedAt: string | null;
}

export interface FilingWizardRequest {
  filingType: FilingType;
  periodStart: string;
  periodEnd: string;
  step: number;
  answers: Record<string, unknown>;
}

export interface ComplianceIssue {
  code: string;
  severity: RuleSeverity;
  entity: string;
  message: string;
}

export interface Recommendation {
  code: string;
  severity: RuleSeverity;
  title: string;
  message: string;
  estimatedImpact: number;
}

export interface FinancialHealth {
  score: number;
  grade: string;
  strengths: string[];
  recommendations: string[];
}

export interface Insight {
  title: string;
  value: string;
  explanation: string;
  severity: RuleSeverity;
}

export interface KpiDashboard {
  grossMargin: number;
  netProfitMargin: number;
  inventoryTurnover: number;
  receivableDays: number;
  averageOrderValue: number;
  repeatCustomerRate: number;
  monthlyGrowth: number;
}

export interface NotificationItem {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  readFlag: boolean;
  actionUrl: string | null;
  dueDate: string | null;
  createdAt: string;
}

export interface Dashboard {
  metrics: Record<string, number>;
  pendingFilings: number;
  pendingInvoices: number;
  stockValue: number;
  cashFlow: number;
  healthScore: number;
  insights: Insight[];
  notifications: NotificationItem[];
  charts: Record<string, number[]>;
  kpis: KpiDashboard;
}

export interface SearchResult {
  type: "CUSTOMER" | "PRODUCT" | "INVOICE" | "DOCUMENT";
  id: string;
  title: string;
  subtitle: string;
  url: string;
}

export interface AuditEntry {
  id: number;
  userId: string | null;
  businessId: string | null;
  actionTime: string;
  ipAddress: string;
  action: string;
  entityType: string;
  entityId: string | null;
  oldValue: string | null;
  newValue: string | null;
  success: boolean;
  errorMessage: string | null;
}

export interface InventoryMovementRequest {
  productId: string;
  type: "STOCK_IN" | "STOCK_OUT" | "SALE" | "PURCHASE" | "ADJUSTMENT" | "EXPIRED";
  quantity: number;
  unitCost: number;
  reason: string;
  referenceNumber?: string | null;
}
