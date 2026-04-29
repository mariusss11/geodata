import { cn } from "@/lib/utils";
import {
  Globe,
  CheckCircle2,
  XCircle,
  ArrowRight,
  UserIcon,
  Calendar
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { BorrowedMapItem } from "@/utils/map-service";
import { formatDateLocal } from "@/lib/date/date";
import { useAuth } from "@/context/AuthContext";

interface MapCardProps {
  borrowedMap: BorrowedMapItem;
  currentUser?: any | null;
  onBorrow?: (id: number) => void;
  onReturn?: (map: BorrowedMapItem) => void;
  onTransfer?: (map: BorrowedMapItem) => void;
  onViewDetails: (map: BorrowedMapItem) => void; // NEW
  isProcessing?: boolean;
}

export function MapCard({ borrowedMap, currentUser, onBorrow, onReturn, onTransfer, onViewDetails, isProcessing }: MapCardProps) {

  const { user } = useAuth();
  console.log('user: ', user)

  const isAvailable = borrowedMap.availabilityStatus === "AVAILABLE";
  const isBorrowed = borrowedMap.availabilityStatus === "BORROWED";
  const isBorrowedByMe = isBorrowed && borrowedMap.userId === user?.userId;
  const isUnavailable = borrowedMap.availabilityStatus === "UNAVAILABLE";

  // console.log('info: ', borrowedMap)
  // console.log('is borrowed: ', isBorrowed)
  // console.log('is borrowed by me: ', isBorrowedByMe)

  const statusColor = isAvailable
    ? { bg: "bg-emerald-100", text: "text-emerald-700", header: "bg-emerald-500" }
    : isBorrowed
      ? { bg: "bg-indigo-100", text: "text-indigo-700", header: "bg-indigo-500" }
      : { bg: "bg-slate-100", text: "text-slate-500", header: "bg-slate-300" };

  return (
    <div className="group relative bg-card hover:bg-slate-50 rounded-xl border shadow-sm hover:shadow-md transition-all duration-300 overflow-hidden flex flex-col h-full">
      {/* Visual Header */}
      <div className={`h-2 w-full ${statusColor.header}`} />

      <div className="p-5 flex-1 flex flex-col">
        {/* Header + Badge */}
        <div className="flex justify-between items-start mb-3">
          <div className="p-2 rounded-lg bg-slate-100 text-slate-600 group-hover:bg-white group-hover:text-primary transition-colors shadow-inner">
            <Globe className="w-6 h-6" />
          </div>
          <Badge
            variant="default"
            className={cn("capitalize px-3 py-1", statusColor.bg + " " + statusColor.text)}
          >
            {isAvailable ? (
              <span className="flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Available</span>
            ) : isBorrowed ? (
              <span className="flex items-center gap-1"><UserIcon className="w-3 h-3" /> Borrowed</span>
            ) : (
              <span className="flex items-center gap-1"><XCircle className="w-3 h-3" /> Unavailable</span>
            )}
          </Badge>
        </div>

        {/* Map info */}
        <h3 className="text-lg font-bold font-display text-foreground leading-tight mb-2 group-hover:text-primary transition-colors">
          {borrowedMap.name}
        </h3>
        <p className="text-sm text-muted-foreground line-clamp-2 mb-4 flex-1">
          {borrowedMap.description || "No description available"}
        </p>

        <div className="flex flex-col gap-1 text-xs font-mono text-slate-500 mb-6 bg-slate-50 p-2 rounded border">
          <span className="flex items-center gap-1"><Calendar className="w-3 h-3" /> Created on: {formatDateLocal(borrowedMap.createdAt, 'ro-RO')}</span>
          <span className="flex items-center gap-1"><Calendar className="w-3 h-3" /> Last updated: {formatDateLocal(borrowedMap.updatedAt, 'ro-RO')}</span>
        </div>

        {/* Buttons */}
        <div className="mt-auto pt-4 border-t border-slate-100">
          {/* AVAILABLE */}
          {isAvailable && (
            <div className="grid grid-cols-2 gap-2">
              <Button
                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-200 shadow-lg"
                onClick={() => onBorrow?.(borrowedMap.id)}
                disabled={isProcessing}
              >
                {isProcessing ? "Processing..." : "Borrow Map"}
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>

              <Button
                variant="outline"
                className="w-full border border-slate-300 hover:bg-slate-100 hover:text-foreground transition-colors"
                onClick={() => onViewDetails(borrowedMap)}
              >
                View Details
              </Button>
            </div>
          )}

          {/* BORROWED */}
          {isBorrowed && (
            <>
              {/* First row: View Details */}
              <div className="grid gap-2 grid-cols-1 mb-2">
                <Button
                  variant="outline"
                  className="w-full border border-slate-300 hover:bg-slate-100 hover:text-foreground transition-colors"
                  onClick={() => onViewDetails(borrowedMap)}
                >
                  View Details
                </Button>
              </div>

              {/* Second row: Return + Transfer (if both exist) */}
              {(onReturn && isBorrowedByMe) || (onTransfer && isBorrowedByMe) ? (
                <div className={`grid gap-2 ${onReturn && onTransfer ? "grid-cols-2" : "grid-cols-1"}`}>
                  {onReturn && isBorrowedByMe && (
                    <Button
                      className="w-full bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-200 shadow-lg"
                      onClick={() => onReturn(borrowedMap)}
                      disabled={isProcessing}
                    >
                      {isProcessing ? "Processing..." : "Return Map"}
                      <ArrowRight className="w-4 h-4 ml-2" />
                    </Button>
                  )}

                  {onTransfer && isBorrowedByMe && (
                    <Button
                      className="w-full bg-blue-600 hover:bg-blue-700 text-white shadow-blue-200 shadow-lg"
                      onClick={() => onTransfer(borrowedMap)}
                      disabled={isProcessing}
                    >
                      {isProcessing ? "Processing..." : "Transfer Map"}
                      <ArrowRight className="w-4 h-4 ml-2" />
                    </Button>
                  )}
                </div>
              ) : null}
            </>
          )}


          {/* UNAVAILABLE */}
          {isUnavailable && (
            <div className="grid grid-cols-1">
              <Button
                className="w-full bg-slate-400 text-white cursor-not-allowed"
                disabled
              >
                Unavailable
              </Button>
            </div>
          )}
        </div>


      </div>
    </div>
  );
}
