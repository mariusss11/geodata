"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { FormProvider, useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Map } from "lucide-react";
import { useLocation } from "wouter";

// ---------------- Zod Schemas ----------------
const loginSchema = z.object({
  username: z.string().min(3, "Username must be at least 3 characters"),
  password: z.string().min(6, "Password must be at least 6 characters"),
});

const registerSchema = z.object({
  username: z.string().min(3, "Username must be at least 3 characters"),
  password: z.string().min(6, "Password must be at least 6 characters"),
  name: z.string().min(3, "Name must be at least 3 characters"),
});

type AuthFormValues = z.infer<typeof registerSchema | typeof loginSchema>;

// ---------------- Auth Page ----------------
export default function AuthPage() {
  const [location, setLocation] = useLocation();
  const { login, register, isLoading } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"login" | "register">("login");

  const form = useForm<AuthFormValues>({
    defaultValues: { username: "", password: "", name: "" },
  });

  // Reset form + error when switching tabs
  useEffect(() => {
    form.reset();
    setError(null);
  }, [activeTab]);

  async function onSubmit(values: AuthFormValues) {
    console.log("submit", values);
    setError(null);
    try {
      // Validate depending on active tab and set form errors when invalid
      if (activeTab === "login") {
        const result = loginSchema.safeParse(values);
        if (!result.success) {
          const flat = result.error.flatten().fieldErrors;
          Object.entries(flat).forEach(([k, v]) => {
            form.setError(k as any, { message: (v && v[0]) || "Invalid" });
          });
          return;
        }
        await login(values.username, values.password);
      } else {
        const result = registerSchema.safeParse(values);
        if (!result.success) {
          const flat = result.error.flatten().fieldErrors;
          Object.entries(flat).forEach(([k, v]) => {
            form.setError(k as any, { message: (v && v[0]) || "Invalid" });
          });
          return;
        }
        const registeredValues = result.data;
        await register(registeredValues.username, registeredValues.name, registeredValues.password);
      }
      setLocation("/dashboard");
    } catch (err: any) {
      // console.log('the error is: ', err.response.data)
      // console.log('the error is: ', err.resonse.data)
      const msg = err?.response?.data || "Authentication failed";
      setError(msg);
      console.warn(err);
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-slate-50">
      {/* ---------------- Left Side ---------------- */}
      <div className="hidden lg:flex flex-col justify-between p-12 bg-slate-900 text-white relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-accent/20 z-0" />
        <div className="relative z-10">
          <div className="flex items-center gap-2 mb-8">
            <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-primary to-accent flex items-center justify-center">
              <Map className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-2xl font-bold font-display">GeoData</h1>
          </div>
          <h2 className="text-5xl font-bold font-display leading-tight mb-6">
            Secure Access to <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary to-accent">
              Global Geodata.
            </span>
          </h2>
          <p className="text-slate-400 text-lg max-w-md">
            The world's most comprehensive repository for topographical, geological, and climate mapping data.
          </p>
        </div>

        <div className="relative z-10 grid grid-cols-2 gap-8 text-sm text-slate-400">
          <div>
            <strong className="text-white block text-lg mb-1">2.4k+</strong>
            High-res Maps
          </div>
          <div>
            <strong className="text-white block text-lg mb-1">150+</strong>
            Research Orgs
          </div>
        </div>
      </div>

      {/* ---------------- Right Side (Form) ---------------- */}
      <div className="flex items-center justify-center p-6 lg:p-12">
        <Card className="w-full max-w-md border-0 shadow-none bg-transparent">
          <CardHeader className="text-center pb-8">
            <CardTitle className="text-3xl font-bold font-display text-foreground">
              {activeTab === "login" ? "Welcome Back" : "Create Account"}
            </CardTitle>
            <CardDescription>
              {activeTab === "login"
                ? "Enter your credentials to access the vault."
                : "Join the network to start borrowing maps."}
            </CardDescription>
          </CardHeader>

          <CardContent>
            {/* ---------------- FormProvider ---------------- */}
            <FormProvider {...form}>
              {/* ---------------- Tabs ---------------- */}
              <Tabs
                value={activeTab}
                onValueChange={(val) =>
                  setActiveTab(val as "login" | "register")
                }
              >
                <TabsList className="grid w-full grid-cols-2 mb-8">
                  <TabsTrigger value="login">Login</TabsTrigger>
                  <TabsTrigger value="register">Register</TabsTrigger>
                </TabsList>

                {/* ---------------- Form ---------------- */}
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                  {/* Username */}
                  <FormField
                    control={form.control}
                    name="username"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Username</FormLabel>
                        <FormControl>
                          <Input {...field} placeholder="Enter username..." />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* Name (only for register) */}
                  {activeTab === "register" && (
                    <FormField
                      control={form.control}
                      name="name"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Name</FormLabel>
                          <FormControl>
                            <Input {...field} placeholder="Enter name..." />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  )}

                  {/* Password */}
                  <FormField
                    control={form.control}
                    name="password"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Password</FormLabel>
                        <FormControl>
                          <Input type="password" {...field} placeholder="••••••••" />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {/* Error */}
                  {error && <p className="text-sm text-destructive text-center">{error}</p>}

                  {/* Submit */}
                  <Button type="submit" className="w-full">
                    {isLoading
                      ? "Authenticating..."
                      : activeTab === "login"
                        ? "Log In"
                        : "Register"}
                  </Button>
                </form>
              </Tabs>
            </FormProvider>

            {/* ---------------- Mock Credentials ---------------- */}
            <div className="mt-6 text-center text-xs text-muted-foreground">
              <p>Mock Credentials:</p>
              <p>User: user / password</p>
              <p>Admin: admin / password</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
