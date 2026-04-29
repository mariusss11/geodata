import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import axios from 'axios';
import { User, AuthState } from '@/types/index';
import { fetchClient } from '@/utils/user-service';

const VITE_IDENTITY_SERVICE_HOSTNAME = import.meta.env.VITE_AUTH_SERVICE_HOSTNAME || 'localhost:8010';
console.log('VITE_IDENTITY_SERVICE_HOSTNAME:', VITE_IDENTITY_SERVICE_HOSTNAME);
const VITE_IDENTITY_SERVICE_URL = `http://${VITE_IDENTITY_SERVICE_HOSTNAME}`;

interface AuthContextType extends AuthState {
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, name: string, password: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};


interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [authState, setAuthState] = useState<AuthState>({
        user: null,
        isAuthenticated: false,
        isLoading: true,
    });

    useEffect(() => {
        // Check for existing session on app load
        checkAuthStatus();
    }, []);

    const checkAuthStatus = async () => {
        try {
            // Simulate checking authentication status
            const token = localStorage.getItem('token');
            if (token) {
                // In a real app, you'd validate the token with your backend
                const userData = localStorage.getItem('user');
                if (userData) {
                    const user = JSON.parse(userData);
                    setAuthState({
                        user,
                        isAuthenticated: true,
                        isLoading: false,
                    });
                    return;
                }
            }
        } catch (error) {
            console.error('Auth check failed:', error);
        }

        setAuthState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
        });
    };

    const login = async (username: string, password: string) => {
        console.log('trying to login')
        localStorage.getItem('token') && localStorage.removeItem('token');
        localStorage.removeItem('client');
        try {
            const response = await axios.post(VITE_IDENTITY_SERVICE_URL + '/api/auth/login', { username, password });
            console.log("Response: ", response)
            const responseData = response.data;
            localStorage.setItem('token', responseData.message);

            // Always set user with role
            let userWithRole = responseData.data;

            console.log('use is: ', userWithRole)

            // Set the client just when the user has the USER role 
            // if (userWithRole.role === 'USER') {
            //     const client = await fetchClient();
            //     localStorage.setItem('client', JSON.stringify(client));
            // }

            // Set user in localStorage
            localStorage.setItem('user', JSON.stringify(userWithRole));

            setAuthState({
                user: userWithRole,
                isAuthenticated: true,
                isLoading: false,
            });
        } catch (error) {
            console.error('error that occurred: ', error)
            if (error instanceof Error) {
                console.warn('should be modifed for prod')
                throw new Error(error.message)
            } else {
                throw new Error('An error occurred when trying to login');
            }
        }
    };

    const register = async (username: string, name: string, password: string) => {
        try {
            // Simulate API call to your backend
            const response = await axios.post(VITE_IDENTITY_SERVICE_URL + '/api/auth/register', {
                username, name, password, role: 'USER'
            });
            console.log('Registration response:', response);
            if (response.data === '') {
                throw new Error('Registration failed, please try again');
            }
        } catch (error) {
            console.error('Registration failed:', error);
            throw error;
        };
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('client');
        setAuthState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
        });
    };

    return (
        <AuthContext.Provider
            value={{
                ...authState,
                login,
                register,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};