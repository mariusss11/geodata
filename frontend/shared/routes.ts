import { z } from 'zod';
import { insertUserSchema, insertMapSchema, maps, users } from './schema';

export const errorSchemas = {
  validation: z.object({
    message: z.string(),
    field: z.string().optional(),
  }),
  notFound: z.object({
    message: z.string(),
  }),
  internal: z.object({
    message: z.string(),
  }),
};

export const api = {
  auth: {
    login: {
      method: 'POST' as const,
      path: '/api/login',
      input: z.object({ username: z.string(), password: z.string() }),
      responses: {
        200: z.custom<typeof users.$inferSelect>(),
        401: errorSchemas.validation,
      },
    },
    register: {
      method: 'POST' as const,
      path: '/api/register',
      input: insertUserSchema,
      responses: {
        201: z.custom<typeof users.$inferSelect>(),
        400: errorSchemas.validation,
      },
    },
  },
  maps: {
    list: {
      method: 'GET' as const,
      path: '/api/maps',
      responses: {
        200: z.array(z.custom<typeof maps.$inferSelect>()),
      },
    },
    get: {
      method: 'GET' as const,
      path: '/api/maps/:id',
      responses: {
        200: z.custom<typeof maps.$inferSelect>(),
        404: errorSchemas.notFound,
      },
    },
    create: {
      method: 'POST' as const,
      path: '/api/maps',
      input: insertMapSchema,
      responses: {
        201: z.custom<typeof maps.$inferSelect>(),
        400: errorSchemas.validation,
      },
    },
    update: {
      method: 'PUT' as const,
      path: '/api/maps/:id',
      input: insertMapSchema.partial(),
      responses: {
        200: z.custom<typeof maps.$inferSelect>(),
        404: errorSchemas.notFound,
      },
    },
    delete: {
      method: 'DELETE' as const,
      path: '/api/maps/:id',
      responses: {
        204: z.void(),
        404: errorSchemas.notFound,
      },
    },
    borrow: {
      method: 'POST' as const,
      path: '/api/maps/:id/borrow',
      responses: {
        200: z.custom<typeof maps.$inferSelect>(),
        400: errorSchemas.validation,
      },
    },
    return: {
      method: 'POST' as const,
      path: '/api/maps/:id/return',
      responses: {
        200: z.custom<typeof maps.$inferSelect>(),
      },
    },
  },
};

export function buildUrl(path: string, params?: Record<string, string | number>): string {
  let url = path;
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (url.includes(`:${key}`)) {
        url = url.replace(`:${key}`, String(value));
      }
    });
  }
  return url;
}
