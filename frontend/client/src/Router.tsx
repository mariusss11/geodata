import { Route, Redirect, Switch } from "wouter";
import AuthPage from "@/pages/auth-page";
import Dashboard from "@/pages/dashboard";
import MapsList from "@/pages/maps-list";
import MyMaps from "@/pages/my-maps";
import ManageMaps from "@/pages/manage-maps";
import NotFound from "@/pages/not-found";
import ProtectedRoute from "@/components/ProtectedRoute"; // move this to a separate file

export default function Router() {
    return (
        <Switch>
            <Route path="/login">
                <AuthPage />
            </Route>

            <Route path="/dashboard">
                <ProtectedRoute component={Dashboard} />
            </Route>

            <Route path="/maps">
                <ProtectedRoute component={MapsList} />
            </Route>

            <Route path="/my-maps">
                <ProtectedRoute component={MyMaps} />
            </Route>

            <Route path="/manage">
                <ProtectedRoute component={ManageMaps} roles={['admin', 'manager']} />
            </Route>

            <Route path="/">
                <Redirect to="/dashboard" />
            </Route>

            <Route component={NotFound} />
        </Switch>
    );
}
