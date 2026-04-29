import { AuthProvider } from "@/context/AuthContext";
import Router from "@/Router";
import { Toaster } from "./components/ui/toaster";

export default function App() {
  return (
    <AuthProvider>
      <Router />
      <Toaster />
    </AuthProvider>
  );
}
