import { useState, useEffect, useCallback } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { toast } from "@/components/ui/use-toast";
import { fetchUsersPaginated as fetchUsersPaginatedExcludeMe } from "@/utils/user-service";
import { useDebounce } from "@/lib/searchQuery/searchQuery";
import { User } from "@/types";
import { BorrowedMapItem } from "@/utils/map-service";

interface TransferMapModalProps {
    isOpen: boolean;
    onClose: () => void;
    mapId: number | null;
}

const PAGE_SIZE = 5;

export function TransferMapModal({
    isOpen,
    onClose,
    mapId,
}: TransferMapModalProps) {
    const [users, setUsers] = useState<User[]>([]);
    const [selectedUser, setSelectedUser] = useState<number | null>(null);
    const [isProcessing, setIsProcessing] = useState(false);

    const [searchQuery, setSearchQuery] = useState("");
    const debouncedSearch = useDebounce(searchQuery, 500);

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [isLoading, setIsLoading] = useState(false);

    const loadUsers = useCallback(
        async (pageNumber: number, query: string) => {
            setIsLoading(true);
            try {
                const data = await fetchUsersPaginatedExcludeMe(pageNumber, PAGE_SIZE, query);

                console.log('the data is: ', data)

                setUsers(data?.content ?? []);       // always array
                setTotalPages(data?.totalPages ?? 0);
            } catch (err) {
                console.error(err);
                toast({ title: "Failed to load users", variant: "destructive" });
                setUsers([]);                        // fallback if error
                setTotalPages(0);
            } finally {
                setIsLoading(false);
            }
        },
        []
    );



    // Reset page when search changes
    useEffect(() => {
        if (isOpen) setCurrentPage(0);
    }, [debouncedSearch, isOpen]);

    // Load users
    useEffect(() => {
        if (isOpen) {
            loadUsers(currentPage, debouncedSearch);
        }
    }, [currentPage, debouncedSearch, isOpen, loadUsers]);

    // Reset modal state
    useEffect(() => {
        if (isOpen) {
            setSelectedUser(null);
            setSearchQuery("");
            setCurrentPage(0);
        } else {
            setUsers([]);
            setTotalPages(0);
        }
    }, [isOpen]);

    // Reset selection when page/search changes
    useEffect(() => {
        setSelectedUser(null);
    }, [currentPage, debouncedSearch]);

    const handleTransfer = async () => {
        if (!mapId || !selectedUser) return;

        const user = users.find((u) => u.userId === selectedUser);
        if (!user) return;

        if (!confirm(`Transfer map to ${user.name}?`)) return;

        console.log('should transfer map to: ', selectedUser)

        // setIsProcessing(true);
        // try {
        //   await transferMap(mapId, selectedUser);
        //   toast({ title: "Map transferred successfully" });
        //   onClose();
        // } catch (err) {
        //   console.error(err);
        //   toast({ title: "Transfer failed", variant: "destructive" });
        // } finally {
        //   setIsProcessing(false);
        // }
    };

    const selectedUserObj = users?.find((u) => u.userId === selectedUser);

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Transfer Map</DialogTitle>
                </DialogHeader>

                <div className="py-4 space-y-3">
                    {/* Search */}
                    <div>
                        <label className="block text-sm font-medium mb-1">
                            Search user
                        </label>
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search by name or username..."
                            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                        />
                    </div>

                    {/* User List */}
                    <div className="border rounded h-[100px] sm:h-[270px] overflow-y-auto">
                        {isLoading ? (
                            <div className="flex items-center justify-center h-full">
                                <span className="text-sm text-muted-foreground">
                                    Loading users...
                                </span>
                            </div>
                        ) : users.length === 0 ? (
                            <div className="flex items-center justify-center h-full">
                                <div className="text-sm text-muted-foreground text-center space-y-1">
                                    <p>No users found</p>
                                    <p className="text-xs">Try a different search</p>
                                </div>
                            </div>
                        ) : (
                            users.map((user) => (
                                <div
                                    key={user.userId}
                                    onClick={() =>
                                        !isLoading && setSelectedUser(user.userId)
                                    }
                                    className={`
                        flex items-center justify-between
                        p-2 cursor-pointer border-b last:border-b-0
                        transition-all duration-150
                        hover:bg-muted hover:pl-4
                        ${selectedUser === user.userId
                                            ? "bg-primary/10 ring-2 ring-primary"
                                            : ""
                                        }
                    `}
                                >
                                    <div>
                                        <p className="text-sm font-medium">{user.name}</p>
                                        <p className="text-xs text-muted-foreground">
                                            {user.username}
                                        </p>
                                    </div>

                                    {selectedUser === user.userId && (
                                        <span className="text-xs font-semibold text-primary">
                                            Selected
                                        </span>
                                    )}
                                </div>
                            ))
                        )}
                    </div>

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <div className="flex justify-between items-center px-2">
                            <Button
                                variant="outline"
                                size="sm"
                                disabled={currentPage === 0 || isLoading}
                                onClick={() => setCurrentPage((p) => p - 1)}
                            >
                                Previous
                            </Button>

                            <span className="text-sm text-muted-foreground">
                                Page {currentPage + 1} of {totalPages}
                            </span>

                            <Button
                                variant="outline"
                                size="sm"
                                disabled={
                                    currentPage + 1 >= totalPages || isLoading
                                }
                                onClick={() => setCurrentPage((p) => p + 1)}
                            >
                                Next
                            </Button>
                        </div>
                    )}

                    {/* Selected User Info */}
                    {selectedUserObj && (
                        <div className="rounded-md border bg-muted/50 p-3">
                            <p className="text-xs text-muted-foreground">
                                Selected user
                            </p>
                            <p className="text-sm font-semibold">
                                {selectedUserObj.name}
                            </p>
                            <p className="text-xs text-muted-foreground">
                                {selectedUserObj.username}
                            </p>
                        </div>
                    )}
                </div>

                <DialogFooter className="flex justify-between">
                    <Button variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button
                        onClick={handleTransfer}
                        disabled={isProcessing || !selectedUser}
                    >
                        {isProcessing ? "Transferring..." : "Transfer"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
