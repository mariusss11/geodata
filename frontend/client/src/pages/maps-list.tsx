import { useEffect, useState } from "react";
import { fetchMapsPaginated, BorrowedMapItem } from "@/utils/map-service";
import { MapCard } from "@/components/map-card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { useDebounce } from "@/lib/searchQuery/searchQuery";
import { borrowMap } from "@/utils/borrow-service";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { toast } from "@/components/ui/use-toast"
import { formatDateLocal } from "@/lib/date/date";
import { cn } from "@/lib/utils";
import { PaginatedResponse } from "@/types";

const PAGE_SIZE = 3;

export default function MapsList() {
  const [maps, setMaps] = useState<BorrowedMapItem[]>([]);
  const [searchQuery, setSearch] = useState("");
  const debouncedSearch = useDebounce(searchQuery, 500);

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [borrowOpen, setBorrowOpen] = useState(false);
  const [selectedMapId, setSelectedMapId] = useState<number | null>(null);
  const [returnDate, setReturnDate] = useState("");
  const [isBorrowing, setIsBorrowing] = useState(false);

  // NEW: Details modal state
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [selectedMapDetails, setSelectedMapDetails] = useState<BorrowedMapItem | null>(null);


  // Reset pagination when search changes
  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearch]);

  // Fetch data when page or search changes
  useEffect(() => {
    loadMaps(currentPage, debouncedSearch);
  }, [currentPage, debouncedSearch]);

  async function loadMaps(page: number, searchQuery?: string) {
    setIsLoading(true);
    setError(null);

    try {
      const data: PaginatedResponse<BorrowedMapItem> = await fetchMapsPaginated(page, PAGE_SIZE, searchQuery);
      setMaps(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      console.error(err);
      setError("Failed to load maps");
    } finally {
      setIsLoading(false);
    }
  }

  function handleReturnMap(mapId: number) {
    setSelectedMapId(mapId);
    setBorrowOpen(true);
  }

  function handleViewDetails(map: BorrowedMapItem) {
    setSelectedMapDetails(map);
    setDetailsOpen(true);
  }

  async function handleConfirmBorrow() {
    if (!selectedMapId || !returnDate) return;

    try {
      setIsBorrowing(true);
      await borrowMap(selectedMapId, returnDate);
      toast({
        title: "Borrow started successfully",
        description: "You can find this map in your borrowed list.",
      });

      setBorrowOpen(false);
      loadMaps(currentPage, searchQuery); // refresh list
    } catch (e) {
      console.error(e);
      toast({
        title: "Borrow failed",
        description: "Something went wrong. Please try again.",
        variant: "destructive",
      });
    } finally {
      setIsBorrowing(false);
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold font-display tracking-tight">
            Browse Maps
          </h2>
          <p className="text-muted-foreground">
            Find the data you need for your next project.
          </p>
        </div>

        <div className="relative w-full md:w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            placeholder="Search maps..."
            className="pl-9 bg-white"
            value={searchQuery}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-64 rounded-xl bg-slate-100 animate-pulse" />
          ))}
        </div>
      ) : error ? (
        <div className="text-center text-red-500">{error}</div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {maps.map((map) => (
              <MapCard
                key={map.id}
                borrowedMap={map}
                onBorrow={(id) => handleReturnMap(id)}
                onViewDetails={handleViewDetails}
              />

            ))}
          </div>

          {maps.length === 0 && (
            <div className="py-12 text-center text-muted-foreground bg-white rounded-xl border border-dashed">
              No maps found.
            </div>
          )}

          {/* Pagination */}
          <div className="flex justify-center gap-4 pt-6">
            <Button
              variant="outline"
              disabled={currentPage === 0}
              onClick={() => setCurrentPage((p) => p - 1)}
            >
              Previous
            </Button>

            <span className="flex items-center text-sm text-muted-foreground">
              Page {currentPage + 1} of {totalPages}
            </span>

            <Button
              variant="outline"
              disabled={currentPage + 1 >= totalPages}
              onClick={() => setCurrentPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </>
      )}

      {/* Borrow Modal */}
      <Dialog open={borrowOpen} onOpenChange={setBorrowOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Select return date</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="returnDate">Return date</Label>
              <Input
                id="returnDate"
                type="date"
                value={returnDate}
                min={new Date().toISOString().split("T")[0]}
                onChange={(e) => setReturnDate(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setBorrowOpen(false)}>Cancel</Button>
            <Button
              disabled={!returnDate || isBorrowing}
              onClick={handleConfirmBorrow}
              className="bg-emerald-600 hover:bg-emerald-700"
            >
              {isBorrowing ? "Borrowing..." : "Confirm Borrow"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View Details Modal */}
      {selectedMapDetails && (
        <Dialog open={detailsOpen} onOpenChange={setDetailsOpen}>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle className="text-2xl font-bold font-display mb-2">
                {selectedMapDetails?.name}
              </DialogTitle>
              <p className="text-sm text-muted-foreground">
                Detailed information about this map
              </p>
            </DialogHeader>

            <div className="space-y-4 py-4">
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm">
                <p className="text-sm font-medium text-muted-foreground">Description</p>
                <p className="text-base text-foreground mt-1">
                  {selectedMapDetails?.description || "No description"}
                </p>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm flex justify-between items-center">
                <span className="text-sm font-medium text-muted-foreground">Status</span>
                <span className={cn(
                  "px-2 py-1 rounded-full text-xs font-bold",
                  selectedMapDetails.availabilityStatus === "AVAILABLE" ? "bg-emerald-100 text-emerald-700" :
                    selectedMapDetails.availabilityStatus === "BORROWED" ? "bg-indigo-100 text-indigo-700" :
                      "bg-slate-100 text-slate-500"
                )}>
                  {selectedMapDetails.availabilityStatus}
                </span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm flex justify-between items-center">
                <span className="text-sm font-medium text-muted-foreground">Created At</span>
                <span className="text-sm text-foreground">
                  {formatDateLocal(selectedMapDetails.createdAt, "ro-RO")}
                </span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm flex justify-between items-center">
                <span className="text-sm font-medium text-muted-foreground">Last Updated</span>
                <span className="text-sm text-foreground">
                  {formatDateLocal(selectedMapDetails.updatedAt, "ro-RO")}
                </span>
              </div>
            </div>

            <DialogFooter>
              <Button
                className="bg-slate-100 hover:bg-slate-200 text-slate-900 transition-colors"
                onClick={() => setDetailsOpen(false)}
              >
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

    </div>
  );
}
