import type { Express } from "express";
import { createServer, type Server } from "http";
import { storage } from "./storage";
import { api } from "@shared/routes";
import { z } from "zod";

export async function registerRoutes(
  httpServer: Server,
  app: Express
): Promise<Server> {
  // Frontend-only app: these routes are just placeholders and won't be used by the client.
  
  app.get(api.maps.list.path, async (req, res) => {
    const maps = await storage.getMaps();
    res.json(maps);
  });

  return httpServer;
}
