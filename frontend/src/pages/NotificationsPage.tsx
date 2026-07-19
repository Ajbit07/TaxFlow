import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import { notificationApi } from "@/api/endpoints";
import { errorMessage } from "@/api/client";
import { useBusinessId } from "@/context/BusinessContext";
import { useToast } from "@/components/ui/toast";
import { Button, Card, EmptyState, Pagination, Skeleton } from "@/components/ui/primitives";
import { cn, formatDateTime } from "@/lib/utils";

export default function NotificationsPage() {
  const businessId = useBusinessId();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["notifications", businessId, page],
    queryFn: () => notificationApi.list(businessId, { page }),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(businessId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications", businessId] });
      queryClient.invalidateQueries({ queryKey: ["unread", businessId] });
    },
    onError: (error) => toast(errorMessage(error), "error"),
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Notifications</h1>
      <Card>
        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-16" />
            ))}
          </div>
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            icon={<Bell className="h-10 w-10" />}
            title="You're all caught up"
            subtitle="GST due dates, low stock and overdue invoice alerts will appear here."
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {data.content.map((notification) => (
              <li key={notification.id} className={cn("flex items-start gap-3 py-4", !notification.readFlag && "bg-brand-50/40")}>
                <span className="mt-1 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-500">
                  {notification.type.replaceAll("_", " ")}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold">{notification.title}</p>
                  <p className="text-sm text-slate-500">{notification.message}</p>
                  <p className="mt-1 text-xs text-slate-400">{formatDateTime(notification.createdAt)}</p>
                </div>
                {!notification.readFlag && (
                  <Button size="sm" variant="outline" loading={markRead.isPending} onClick={() => markRead.mutate(notification.id)}>
                    Mark read
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
        {data && <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />}
      </Card>
    </div>
  );
}
