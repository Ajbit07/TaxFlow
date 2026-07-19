import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { businessApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useToast } from "@/components/ui/toast";
import { useBusiness } from "@/context/BusinessContext";
import BusinessForm from "@/components/BusinessForm";

export default function OnboardingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const { selectBusiness } = useBusiness();

  const create = useMutation({
    mutationFn: businessApi.create,
    onSuccess: async (business) => {
      await queryClient.invalidateQueries({ queryKey: ["businesses"] });
      selectBusiness(business.id);
      toast("Business created. Welcome to TaxFlow!", "success");
      navigate("/dashboard");
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <div className="mx-auto flex min-h-screen max-w-2xl animate-page-in flex-col justify-center p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Set up your business</h1>
        <p className="text-sm text-slate-500">This powers your invoices, GST reports and tax estimates.</p>
      </div>
      <div className="rounded-lg border border-slate-200 border-t-4 border-t-brand-600 bg-white p-6 shadow-sm">
        <BusinessForm onSubmit={(body) => create.mutate(body)} submitting={create.isPending} submitLabel="Create business" />
      </div>
    </div>
  );
}
