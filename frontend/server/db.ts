import { drizzle } from "drizzle-orm/node-postgres";
import pg from "pg";
import * as schema from "@shared/schema";

const { Pool } = pg;

// For frontend-only app, we might not have a real DB, but we provide this to satisfy imports.
// If DATABASE_URL is missing, we won't crash immediately, but the pool will fail on query.
// This is fine as the frontend won't use it.
const connectionString = process.env.DATABASE_URL || "postgres://postgres:postgres@localhost:5432/postgres";

export const pool = new Pool({ connectionString });
export const db = drizzle(pool, { schema });
