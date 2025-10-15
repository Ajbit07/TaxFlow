import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import type { Business, BusinessRequest, BusinessType } from "@/types/api";
import { Button, Field, Input, Select } from "@/components/ui/primitives";

const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;
const PAN_REGEX = /^[A-Z]{5}[0-9]{4}[A-Z]$/;

export const INDIAN_STATES = [
  "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Delhi", "Goa", "Gujarat",
  "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra",
  "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
  "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
];

const BUSINESS_TYPES: BusinessType[] = [
  "KIRANA_STORE", "SHOPKEEPER", "FREELANCER", "STARTUP", "MSME", "RESTAURANT", "TRADER",
  "PROPRIETORSHIP", "PARTNERSHIP", "LLP", "PRIVATE_LIMITED", "OTHER",
];

const schema = z.object({
  businessName: z.string().min(2, "Business name required"),
  ownerName: z.string().min(2, "Owner name required"),
  gstin: z
    .string()
    .transform((value) => value.trim().toUpperCase())
    .refine((value) => value === "" || GSTIN_REGEX.test(value), "Invalid GSTIN format")
    .optional()
    .or(z.literal("")),
  pan: z
    .string()
    .transform((value) => value.trim().toUpperCase())
    .refine((value) => PAN_REGEX.test(value), "Invalid PAN format (e.g. ABCDE1234F)"),
  email: z.string().email("Enter a valid email"),
  phone: z.string().min(8, "Enter a valid phone"),
  address: z.string().min(5, "Address required"),
  state: z.string().min(2, "Select a state"),
  businessType: z.enum(BUSINESS_TYPES as [BusinessType, ...BusinessType[]]),
  financialYear: z.string().regex(/^\d{4}-\d{4}$/, "Format: 2025-2026"),
});

type FormValues = z.infer<typeof schema>;

function currentFinancialYear(): string {
  const now = new Date();
  const start = now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
  return `${start}-${start + 1}`;
}

export default function BusinessForm({
  initial,
  onSubmit,
  submitting,
  submitLabel = "Save business",
}: {
  initial?: Business | null;
  onSubmit: (body: BusinessRequest) => void;
  submitting: boolean;
  submitLabel?: string;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: initial
      ? {
          businessName: initial.businessName,
          ownerName: initial.ownerName,
          gstin: initial.gstin ?? "",
          pan: initial.pan,
          email: initial.email,
          phone: initial.phone,
          address: initial.address,
          state: initial.state,
          businessType: initial.businessType,
          financialYear: initial.financialYear,
        }
      : { financialYear: currentFinancialYear(), businessType: "PROPRIETORSHIP", state: "Maharashtra" },
  });

  const submit = (values: FormValues) => {
    onSubmit({
      ...values,
      gstin: values.gstin === "" ? null : values.gstin,
      currency: initial?.currency ?? "INR",
      language: initial?.language ?? "en",
      darkMode: initial?.darkMode ?? false,
    });
  };

  return (
    <form onSubmit={handleSubmit(submit)} className="grid gap-4 sm:grid-cols-2">
      <Field label="Business name" error={errors.businessName?.message}>
        <Input placeholder="Sharma Traders" {...register("businessName")} />
      </Field>
      <Field label="Owner name" error={errors.ownerName?.message}>
        <Input placeholder="Priya Sharma" {...register("ownerName")} />
      </Field>
      <Field label="GSTIN (optional)" error={errors.gstin?.message}>
        <Input placeholder="27ABCDE1234F1Z5" {...register("gstin")} />
      </Field>
      <Field label="PAN" error={errors.pan?.message}>
        <Input placeholder="ABCDE1234F" {...register("pan")} />
      </Field>
      <Field label="Business email" error={errors.email?.message}>
        <Input type="email" placeholder="billing@business.in" {...register("email")} />
      </Field>
      <Field label="Phone" error={errors.phone?.message}>
        <Input placeholder="+91 98765 43210" {...register("phone")} />
      </Field>
      <div className="sm:col-span-2">
        <Field label="Address" error={errors.address?.message}>
          <Input placeholder="Shop 4, MG Road, Pune" {...register("address")} />
        </Field>
      </div>
      <Field label="State" error={errors.state?.message}>
        <Select {...register("state")}>
          {INDIAN_STATES.map((state) => (
            <option key={state}>{state}</option>
          ))}
        </Select>
      </Field>
      <Field label="Business type" error={errors.businessType?.message}>
        <Select {...register("businessType")}>
          {BUSINESS_TYPES.map((type) => (
            <option key={type} value={type}>
              {type.replaceAll("_", " ")}
            </option>
          ))}
        </Select>
      </Field>
      <Field label="Financial year" error={errors.financialYear?.message}>
        <Input placeholder="2025-2026" {...register("financialYear")} />
      </Field>
      <div className="flex items-end">
        <Button type="submit" loading={submitting} className="w-full">
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
