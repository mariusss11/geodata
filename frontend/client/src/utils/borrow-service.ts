import axios from "axios";
import { InsertMap } from "@shared/schema";
import { BorrowedMapItem } from "./map-service";

const VITE_API_BORROWS_SERVICE = import.meta.env.VITE_API_BORROWS_SERVICE || "http://localhost:8030/api/borrows";

const JWT_TOKEN = localStorage.getItem('token');


export const borrowMap = async (
    mapId: number,
    returnDate: string,
) => {
    try {
        const response = await axios.post(`${VITE_API_BORROWS_SERVICE}/create`,
            {
                "mapId": mapId,
                "returnDate": returnDate
            },
            {
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

export const returnMap = async (
    borrowedMapItem: BorrowedMapItem,
) => {

    try {
        const response = await axios.post(`${VITE_API_BORROWS_SERVICE}/return`,
            {
                "mapId": borrowedMapItem.mapId,
            },
            {
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

export const fetchCurrentlyBorrowedMaps = async (
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

        const response = await axios.get(`${VITE_API_BORROWS_SERVICE}/current`, {
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