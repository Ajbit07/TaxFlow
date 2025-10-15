import { createContext, useContext, useMemo, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { businessApi } from "@/api/endpoints";
import { useAuth } from "./AuthContext";
import type { Business } from "@/types/api";
import { useLocalStorage } from "@/hooks/useLocalStorage";

interface BusinessContextValue {
  businesses: Business[];
  business: Business | null;
  businessId: string | null;
  selectBusiness: (id: string) => void;
  loading: boolean;
}

const BusinessContext = createContext<BusinessContextValue | null>(null);

export function useBusiness(): BusinessContextValue {
  const ctx = useContext(BusinessContext);
  if (!ctx) throw new Error("useBusiness must be used inside BusinessProvider");
  return ctx;
}

/** Returns the selected business id, throwing if none is selected (routes guard this). */
export function useBusinessId(): string {
  const { businessId } = useBusiness();
  if (!businessId) throw new Error("No business selected");
  return businessId;
}

export function BusinessProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [storedId, setStoredId] = useLocalStorage<string | null>("taxflow.businessId", null);

  const { data: businesses = [], isLoading } = useQuery({
    queryKey: ["businesses", user?.id],
    queryFn: businessApi.mine,
    enabled: !!user,
  });

  const business = useMemo(() => {
    if (businesses.length === 0) return null;
    return businesses.find((b) => b.id === storedId) ?? businesses[0];
  }, [businesses, storedId]);

  const value = useMemo(
    () => ({
      businesses,
      business,
      businessId: business?.id ?? null,
      selectBusiness: setStoredId,
      loading: isLoading,
    }),
    [businesses, business, setStoredId, isLoading],
  );

  return <BusinessContext.Provider value={value}>{children}</BusinessContext.Provider>;
}
