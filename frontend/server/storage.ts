import { users, maps, type User, type InsertUser, type MapItem, type InsertMap } from "@shared/schema";

// Minimal storage interface for the backend skeleton
export interface IStorage {
  getUser(id: number): Promise<User | undefined>;
  getUserByUsername(username: string): Promise<User | undefined>;
  createUser(user: InsertUser): Promise<User>;
  getMaps(): Promise<MapItem[]>;
}

export class MemStorage implements IStorage {
  private users: Map<number, User>;
  private maps: Map<number, MapItem>;
  private currentId: number;

  constructor() {
    this.users = new Map();
    this.maps = new Map();
    this.currentId = 1;
  }

  async getUser(id: number): Promise<User | undefined> {
    return this.users.get(id);
  }

  async getUserByUsername(username: string): Promise<User | undefined> {
    return Array.from(this.users.values()).find(
      (user) => user.username === username,
    );
  }

  async createUser(insertUser: InsertUser): Promise<User> {
    const id = this.currentId++;
    const user: User = { ...insertUser, id, role: insertUser.role || "user" };
    this.users.set(id, user);
    return user;
  }

  async getMaps(): Promise<MapItem[]> {
    return Array.from(this.maps.values());
  }
}

export const storage = new MemStorage();
