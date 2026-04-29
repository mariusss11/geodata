import { useAuth } from "@/context/AuthContext";
import { AppLayout } from "@/components/layout";
import { Redirect } from "wouter";

interface Props {
    component: React.ComponentType;
    roles?: string[];
}

export default function ProtectedRoute({ component: Component, roles = [] }: Props) {
    const { user, isLoading } = useAuth();

    if (isLoading) return <div className="min-h-screen flex items-center justify-center">Loading...</div>;
    if (!user) return <Redirect to="/login" />;
    if (roles.length > 0 && !roles.includes(user.role)) return <Redirect to="/dashboard" />;

    return (
        <AppLayout>
            <Component />
        </AppLayout>
    );
}
