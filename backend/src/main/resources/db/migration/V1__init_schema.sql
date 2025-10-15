-- TaxFlow initial schema (PostgreSQL 15+)

CREATE TABLE users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    email_verified BOOLEAN NOT NULL,
    verification_token VARCHAR(255),
    reset_token VARCHAR(255),
    reset_token_expires_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ
);

CREATE TABLE businesses (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    owner_id UUID NOT NULL REFERENCES users (id),
    gstin VARCHAR(15),
    pan VARCHAR(10) NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    state VARCHAR(80) NOT NULL,
    business_type VARCHAR(40) NOT NULL,
    financial_year VARCHAR(9) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    language VARCHAR(12) NOT NULL,
    dark_mode BOOLEAN NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE business_members (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_business_member UNIQUE (business_id, user_id)
);

CREATE TABLE business_member_scopes (
    member_id UUID NOT NULL REFERENCES business_members (id) ON DELETE CASCADE,
    scope VARCHAR(40) NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL
);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    gstin VARCHAR(15),
    pan VARCHAR(10),
    phone VARCHAR(255),
    email VARCHAR(255),
    address VARCHAR(255),
    outstanding_balance NUMERIC(15, 2) NOT NULL
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    sku VARCHAR(255) NOT NULL,
    hsn_code VARCHAR(8) NOT NULL,
    gst_percentage NUMERIC(5, 2) NOT NULL,
    stock NUMERIC(15, 3) NOT NULL,
    low_stock_threshold NUMERIC(15, 3) NOT NULL,
    purchase_price NUMERIC(15, 2) NOT NULL,
    selling_price NUMERIC(15, 2) NOT NULL,
    barcode VARCHAR(255),
    expiry_date DATE
);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    quantity NUMERIC(15, 3) NOT NULL,
    unit_cost NUMERIC(15, 2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reference_number VARCHAR(255)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers (id),
    invoice_number VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    recurring BOOLEAN NOT NULL,
    recurrence_pattern VARCHAR(255),
    template_name VARCHAR(255) NOT NULL,
    subtotal NUMERIC(15, 2) NOT NULL,
    total_gst NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    paid_amount NUMERIC(15, 2) NOT NULL,
    notes VARCHAR(255),
    qr_payload VARCHAR(1024)
);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    invoice_id UUID NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    product_id UUID REFERENCES products (id),
    description VARCHAR(255) NOT NULL,
    hsn_code VARCHAR(8) NOT NULL,
    quantity NUMERIC(15, 3) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,
    gst_rate NUMERIC(5, 2) NOT NULL,
    taxable_amount NUMERIC(15, 2) NOT NULL,
    gst_amount NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    vendor VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    gst_amount NUMERIC(15, 2) NOT NULL,
    expense_date DATE NOT NULL,
    receipt_url VARCHAR(255),
    description VARCHAR(255)
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    uploaded_by_id UUID NOT NULL REFERENCES users (id),
    document_type VARCHAR(40) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    version INTEGER NOT NULL,
    extracted_fields_json TEXT,
    status VARCHAR(255) NOT NULL
);

CREATE TABLE tax_filings (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    filing_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    progress_percent INTEGER NOT NULL,
    tax_due NUMERIC(15, 2) NOT NULL,
    late_fee NUMERIC(15, 2) NOT NULL,
    summary_json TEXT,
    submitted_at TIMESTAMPTZ
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read_flag BOOLEAN NOT NULL,
    action_url VARCHAR(255),
    due_date DATE
);

CREATE TABLE business_rules (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    business_id UUID REFERENCES businesses (id) ON DELETE CASCADE,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    expression_key VARCHAR(255) NOT NULL,
    message TEXT NOT NULL
);

CREATE TABLE audit_logs (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id UUID,
    business_id UUID,
    action_time TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    success BOOLEAN NOT NULL,
    error_message TEXT
);

-- Indexes for frequent lookups
CREATE INDEX idx_businesses_owner ON businesses (owner_id);
CREATE INDEX idx_customers_business ON customers (business_id);
CREATE INDEX idx_products_business ON products (business_id);
CREATE INDEX idx_products_low_stock ON products (business_id, stock);
CREATE INDEX idx_invoices_business ON invoices (business_id);
CREATE INDEX idx_invoices_business_date ON invoices (business_id, invoice_date);
CREATE INDEX idx_invoices_business_status ON invoices (business_id, status);
CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
CREATE INDEX idx_expenses_business_date ON expenses (business_id, expense_date);
CREATE INDEX idx_documents_business ON documents (business_id);
CREATE INDEX idx_tax_filings_business ON tax_filings (business_id);
CREATE INDEX idx_notifications_user ON notifications (business_id, user_id);
CREATE INDEX idx_audit_logs_business ON audit_logs (business_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Default global business rule used by the tax engine
INSERT INTO business_rules (id, created_at, updated_at, business_id, code, name, rule_type, severity, enabled, expression_key, message)
VALUES (gen_random_uuid(), now(), now(), NULL, 'GLOBAL_LOW_ITC', 'Low ITC capture', 'COMPLIANCE', 'WARNING', TRUE, 'LOW_ITC_CAPTURE',
        'Sales GST exists but no input tax credit has been recorded for this period. Upload purchase bills before filing.');
