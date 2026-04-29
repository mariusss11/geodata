import axios from "axios";

const HOSTNAME = import.meta.env.VITE_API_IDENTITY_SERVICE || "localhost:8010";
const VITE_API_IDENTITY_SERVICE = `http://${HOSTNAME}/api/users`;
const ITEMS_PER_PAGE = 5;

/**
 * Fetch client profile information using token stored in localStorage.
 * @returns {Promise<Client>} - The authenticated client's data.
 * @throws {Error} - On request failure.
 */
export const fetchClient = async () => {
    const token = localStorage.getItem('token');
    console.log("Fetching client with token:", token);

    try {
        const response = await axios.get(VITE_API_IDENTITY_SERVICE, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        console.log("Client was successfully fetched:", response.data);
        return response.data;
    } catch (error) {
        console.error("Error fetching client:", error);
        throw error;
    }
};

/**
 * Fetch the total number of items currently borrowed by the client.
 * @returns {Promise<number>} - Number of borrowed items.
 * @throws {Error} - On request failure.
 */
export const fetchNumberOfCurrentlyBorrowedItems = async () => {
    try {
        const response = await axios.get(`${VITE_API_IDENTITY_SERVICE}/currently/borrowed-items/number`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });
        return response;
    } catch (error) {
        console.error("Error fetching the number of currently borrowed items:", error);
        throw error;
    }
};

/**
 * Fetch all items currently borrowed by the client.
 * @returns {Promise<Item[]>} - Array of currently borrowed items.
 * @throws {Error} - On request failure.
 */
export const fetchCurrentlyBorrowedItemsPaginated = async (
    page: number,
    query: string,
) => {
    console.log('Fetching reviewable items with params: ', page, query)
    const params = new URLSearchParams();
    params.append('page', String(page));
    params.append('size', String(ITEMS_PER_PAGE));
    params.append('searchQuery', query);
    try {
        const response = await axios.get(`${VITE_API_IDENTITY_SERVICE}/currently/borrowed-items/paginated?${params.toString()}`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });
        console.log('Successfully fetched client\'s borrowed items')
        return response.data;
    } catch (error) {
        console.error("Error fetching currently borrowed items:", error);
        throw error;
    }
};

export const fetchUsersPaginated = async (
    pageNumber: number,
    pageSize: number,
    search: string,
) => {
    console.log('Fetching users with params: ', pageNumber, search)

    const params: Record<string, any> = {
        pageNumber,
        pageSize
    };

    if (search?.trim()) {
        params.search = search;
    }

    try {
        const response = await axios.get(`${VITE_API_IDENTITY_SERVICE}/paginated/exclude-me`, {
            params,
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });
        console.log('Users fetched successfully:', response)
        return response.data;
    } catch (error) {
        console.error("Error fetching users:", error);
        throw error;
    }
};

