import { useEffect, useState, useCallback } from "react";
import { BorrowedMapItem } from "@/utils/map-service";
import { useAuth } from "@/context/AuthContext";
import { MapCard } from "@/components/map-card";
import { Library, Map } from "lucide-react";
import { useDebounce } from "@/lib/searchQuery/searchQuery";
import { PaginatedResponse } from "@/types";
import { fetchCurrentlyBorrowedMaps, returnMap } from "@/utils/borrow-service";
import { Button } from "@/components/ui/button";
import { toast } from "@/components/ui/use-toast";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { formatDateLocal } from "@/lib/date/date";
import { TransferMapModal } from "./modals/MapTransferModal";

const PAGE_SIZE = 3;

export default function MyMaps() {
  const { user } = useAuth();

  const [borrowedMaps, setBorrowedMaps] = useState<BorrowedMapItem[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const debouncedSearch = useDebounce(searchQuery, 500);

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [detailsOpen, setDetailsOpen] = useState(false);
  const [selectedMapDetails, setSelectedMapDetails] = useState<BorrowedMapItem | null>(null);

  const [returnConfirmOpen, setReturnConfirmOpen] = useState(false);
  const [mapToReturn, setMapToReturn] = useState<BorrowedMapItem | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const [transferOpen, setTransferOpen] = useState(false);
  const [mapToTransfer, setMapToTransfer] = useState<BorrowedMapItem | null>(null);


  /** -------------------------
   * Fetch Borrowed Maps
   * ------------------------ */
  const loadMaps = useCallback(async (page: number, query?: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const data: PaginatedResponse<BorrowedMapItem> = await fetchCurrentlyBorrowedMaps(page, PAGE_SIZE, query);
      console.log('data: ', data.content)
      setBorrowedMaps(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      console.error(err);
      setError("Failed to load maps");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    setCurrentPage(0); // reset page when search changes
  }, [debouncedSearch]);

  useEffect(() => {
    loadMaps(currentPage, debouncedSearch);
  }, [currentPage, debouncedSearch, loadMaps]);

  /** -------------------------
   * Helper Functions
   * ------------------------ */
  const openDetailsModal = (map: BorrowedMapItem) => {
    setSelectedMapDetails(map);
    setDetailsOpen(true);
  };

  const handleReturnMap = (map: BorrowedMapItem) => {
    setMapToReturn(map);
    setReturnConfirmOpen(true);
  };


  const handleTransferMapOpenModal = (map: BorrowedMapItem) => {
    setMapToTransfer(map);
    setTransferOpen(true);
  };

  const confirmReturn = async () => {
    if (!mapToReturn) return;

    try {
      setIsProcessing(true);
      await returnMap(mapToReturn);
      toast({
        title: "Map returned successfully",
        description: `${mapToReturn.name} has been returned.`,
      });

      setReturnConfirmOpen(false);
      setMapToReturn(null);
      loadMaps(currentPage, debouncedSearch);
    } catch (err) {
      console.error(err);
      toast({
        title: "Return failed",
        description: "Something went wrong. Please try again.",
        variant: "destructive",
      });
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h2 className="text-3xl font-bold font-display tracking-tight text-foreground flex items-center gap-3">
          <Library className="w-8 h-8 text-primary" />
          My Collection
        </h2>
        <p className="text-muted-foreground">Maps currently borrowed by your account.</p>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => <div key={i} className="h-64 rounded-xl bg-slate-100 animate-pulse" />)}
        </div>
      ) : error ? (
        // Error message
        <div className="col-span-full py-20 flex flex-col items-center justify-center text-center bg-red-50 rounded-2xl border border-red-200">
          <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mb-4">
            <Map className="w-8 h-8 text-red-400" />
          </div>
          <h3 className="text-lg font-bold text-red-700">{error}</h3>
          {/* <p className="text-red-600 max-w-sm mt-2">Please try again later!</p> */}
          {/* <Button
              variant="outline"
              className="mt-4 red"
              onClick={() => loadMaps(currentPage, debouncedSearch)}
            >
              Retry
            </Button> */}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {borrowedMaps?.map(map => (
              <MapCard
                key={map.borrowId}
                borrowedMap={map}
                onViewDetails={openDetailsModal}
                onReturn={() => handleReturnMap(map)}
                onTransfer={() => handleTransferMapOpenModal(map)}
                isProcessing={isProcessing}
              />

            ))}

            {borrowedMaps?.length === 0 && !isLoading && (
              <div className="col-span-full py-20 flex flex-col items-center justify-center text-center bg-white rounded-2xl border border-dashed">
                <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center mb-4">
                  <Library className="w-8 h-8 text-slate-300" />
                </div>
                <h3 className="text-lg font-bold text-foreground">No maps borrowed</h3>
                <p className="text-muted-foreground max-w-sm mt-2">
                  You haven't borrowed any maps yet. Visit the Browse page to find data for your research.
                </p>
              </div>
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-4 pt-6">
              <Button
                variant="outline"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => p - 1)}
              >
                Previous
              </Button>

              <span className="text-sm text-muted-foreground">
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
          )}
        </>
      )}

      {/* Details Modal */}
      {selectedMapDetails && (
        <Dialog open={detailsOpen} onOpenChange={setDetailsOpen}>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle className="text-2xl font-bold font-display mb-2">{selectedMapDetails.name}</DialogTitle>
              <DialogDescription className="text-sm text-muted-foreground">
                Detailed information about this map
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm">
                <p className="text-sm font-medium text-muted-foreground">Description</p>
                <p className="text-base text-foreground mt-1">{selectedMapDetails.description || "No description"}</p>
              </div>
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm flex justify-between items-center">
                <span className="text-sm font-medium text-muted-foreground">Status</span>
                <span className="px-2 py-1 rounded-full text-xs font-bold">
                  {selectedMapDetails.availabilityStatus}
                </span>
              </div>
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm flex justify-between items-center">
                <span className="text-sm font-medium text-muted-foreground">Created At</span>
                <span className="text-sm text-foreground">{formatDateLocal(selectedMapDetails.createdAt, "ro-RO")}</span>
              </div>
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 shadow-sm flex justify-between items-center">
                <span className="text-sm font-medium text-muted-foreground">Last Updated</span>
                <span className="text-sm text-foreground">{formatDateLocal(selectedMapDetails.updatedAt, "ro-RO")}</span>
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

      {/* Return Confirmation Modal */}
      <Dialog open={returnConfirmOpen} onOpenChange={setReturnConfirmOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Return Map</DialogTitle>
          </DialogHeader>
          <div className="py-4 text-sm text-foreground">
            Are you sure you want to return{" "}
            <span className="font-medium">{mapToReturn?.name}</span>?
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setReturnConfirmOpen(false)}>
              Cancel
            </Button>
            <Button
              className="bg-red-600 hover:bg-red-700 text-white"
              onClick={confirmReturn}
              disabled={isProcessing}
            >
              {isProcessing ? "Returning..." : "Confirm Return"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Transfer Modal */}
      <TransferMapModal
        isOpen={transferOpen}
        onClose={() => setTransferOpen(false)}
        mapId={mapToTransfer?.borrowId ?? null}
      />

    </div>
  );
}
