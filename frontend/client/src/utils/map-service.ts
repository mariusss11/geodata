import axios from "axios";
import { InsertMap } from "@shared/schema";

const VITE_API_MAPS_SERVICE = import.meta.env.VITE_API_BACKEND || "http://localhost:8020/api";

const JWT_TOKEN = localStorage.getItem('token');

export interface BorrowedMapItem {
    id: number;
    name: string;
    description: string;
    availabilityStatus: "AVAILABLE" | "BORROWED" | "UNAVAILABLE";
    currentUserId?: string | null;
    locationData?: { lat: number; lng: number };
    createdAt: string;
    updatedAt: string;
}

export interface BorrowedMapItem {
    borrowId: number;
    mapId: number;
    userId: number;
    name: string;
    description: string;
    availabilityStatus: "AVAILABLE" | "BORROWED" | "UNAVAILABLE";
    currentUserId?: string | null;
    locationData?: { lat: number; lng: number };
    createdAt: string;
    updatedAt: string;
}

// Fetch all maps
export const fetchRecentMaps = async (): Promise<BorrowedMapItem[]> => {
    try {
        const response = await axios.get(
            `${VITE_API_MAPS_SERVICE}/maps/recent`, {
            headers: {
                Authorization: `Bearer ${JWT_TOKEN}`
            }
        }
        )
        return response.data;
    } catch (error) {
        console.error("Error fetching recent maps :", error);
        throw error;
    }

};


// Create map
export function useCreateMap() {
    const createMap = (map: Omit<BorrowedMapItem, "id">): Promise<BorrowedMapItem> => {
        return axios.post(`${VITE_API_MAPS_SERVICE}/maps`, map).then((res) => res.data);
    };

    return { createMap };
}

// Delete map
export function useDeleteMap() {
    const deleteMap = (id: string): Promise<void> => {
        return axios.delete(`${VITE_API_MAPS_SERVICE}/maps/${id}`).then(() => undefined);
    };

    return { deleteMap };
}

export interface MapStats {
    totalMaps: number;
    availableMaps: number;
    borrowedMaps: number;
};

// Map stats
export const fetchMapsStats = async (): Promise<MapStats> => {
    try {
        const response = await axios.get(
            `${VITE_API_MAPS_SERVICE}/maps/stats`, {
            headers: {
                Authorization: `Bearer ${JWT_TOKEN}`
            }
        })
        return response.data;
    } catch (error) {
        console.error("Error fetching the maps stats:", error);
        throw error;
    }
}

export const fetchMapsPaginated = async (
    pageNumber: number,
    pageSize: number,
    searchQuery?: string
) => {
    try {
        const params: Record<string, any> = {
            pageNumber,
            pageSize
        };

        // console.log('the params are: ', params)

        if (searchQuery?.trim()) {
            params.searchQuery = searchQuery;
        }

        const response = await axios.get(`${VITE_API_MAPS_SERVICE}/maps/search/pagination`, {
            params,
            headers: {
                Authorization: `Bearer ${JWT_TOKEN}`
            }
        })
        return response.data;
    } catch (error) {
        console.error("Error fetching the maps stats:", error);
        throw error;
    }
}



export function useBorrowMap() {
    const fetchStats = (): Promise<{ total: number; available: number; borrowed: number }> => {
        return axios.get(`${VITE_API_MAPS_SERVICE}/maps/stats`).then((res) => res.data);
    };

    return { fetchStats };
}