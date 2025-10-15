import type { ReactNode } from "react";
import { motion } from "framer-motion";

export default function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f7f6f2] p-4 dark:bg-[#101413]">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md rounded-lg border border-slate-200 border-t-4 border-t-brand-600 bg-white p-8 shadow-sm dark:border-slate-800 dark:border-t-brand-500 dark:bg-slate-900"
      >
        <p className="mb-8 font-display text-2xl font-bold tracking-tight">TaxFlow</p>
        <h1 className="text-xl font-semibold">{title}</h1>
        <p className="mb-6 mt-1 text-sm text-slate-500">{subtitle}</p>
        {children}
      </motion.div>
    </div>
  );
}
