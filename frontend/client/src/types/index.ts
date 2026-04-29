export interface User {
    userId: number;
    name: string;
    username: string;
    password: string;
    role: "user" | "manager" | "admin" | string;
};

export interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
}

export interface PaginatedResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
};