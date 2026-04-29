import { pgTable, text, serial, integer, boolean, jsonb } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

export const users = pgTable("users", {
  id: serial("id").primaryKey(),
  username: text("username").notNull().unique(),
  password: text("password").notNull(),
  role: text("role", { enum: ["user", "manager", "admin"] }).notNull().default("user"),
});

export const maps = pgTable("maps", {
  id: serial("id").primaryKey(),
  name: text("name").notNull(),
  description: text("description").notNull(),
  status: text("status", { enum: ["available", "borrowed"] }).notNull().default("available"),
  // Let's use custom types or just jsonb for location if we want strict adherence to "locationData" object, or separate columns.
  // Prompt: locationData?: { lat: number; lng: number }
  // DB usually stores lat/lng as float/decimal. Drizzle pg-core doesn't have float? It has real/doublePrecision.
  // Let's use jsonb for locationData to match the object structure easily, or just mock it in frontend. 
  // For schema strictly:
});

export const insertUserSchema = createInsertSchema(users);
export const insertMapSchema = createInsertSchema(maps);

export type User = typeof users.$inferSelect;
export type InsertUser = z.infer<typeof insertUserSchema>;
export type MapItem = typeof maps.$inferSelect;
export type InsertMap = z.infer<typeof insertMapSchema>;
