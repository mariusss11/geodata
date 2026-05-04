# Instructions for Claude Code — Frontend (React)

You are helping Marius work on the **React/TypeScript** web app for the
Geodata project. This is the **web** client — the mobile Flutter app
lives in `../mobile/` and the Spring Boot services live in `../backend/`.

## Project at a glance

- **Stack:** React 18 + TypeScript, Vite, Tailwind, shadcn/ui (Radix),
  TanStack Query, axios, wouter (router), react-hook-form + zod.
- **Source root:** `client/src/` — the Vite `root` is `client/`.
- **Path aliases:** `@/*` → `client/src/*`, `@shared/*` → `shared/*`,
  `@assets/*` → `attached_assets/*`.
- **Dev:** `npm run dev` (vite). **Build:** `npm run build`.
- **Backend:** three Spring Boot services. Frontend talks to them over
  HTTP using the env vars listed below. Do **not** invent routes — read
  the controllers in `../backend/*/src/main/java/com/geodata/.../controller*/`
  before adding or changing an endpoint call.
- **Marius's background:** strong Java/Spring Boot, comfortable with
  React/TS. Prefer concrete pointers over hand-holding.

## Backend services and base routes

| Service          | Default port | Base path          | Env var                      |
|------------------|--------------|--------------------|------------------------------|
| identity-service | 8010         | `/api/auth`, `/api/users`, `/api/admin` | `VITE_API_IDENTITY_SERVICE`, `VITE_AUTH_SERVICE_HOSTNAME` |
| map-service      | 8020         | `/api/maps`, `/api/maps/manager` | `VITE_API_BACKEND` |
| borrow-service   | 8030         | `/api/borrows`, `/api/borrows/librarian` | `VITE_API_BORROWS_SERVICE` |

The "current user's borrowed maps" data lives on **borrow-service**
(`GET /api/borrows/current`), not identity-service. There is **no**
`GET /api/users/me` whoami endpoint — the user is captured into
localStorage at login and read from there.

## How to work with Marius

- **One step at a time.** Finish a change, confirm it compiles
  (`npm run dev` or at minimum `tsc --noEmit`), then move on.
- **Ask before branching/committing.** When making non-trivial changes,
  branch off `dev` (never `main`). Prefer one branch per logical concern
  (`fix/...`, `chore/...`, `security/...`) over one giant branch.
- **Confirm endpoint shape against the backend controller** before
  changing a request — request bodies and response wrappers
  (`PagedResponse<T>`) come from Java DTOs that you can read directly.
- **Don't reformat or refactor** code that isn't part of the current task.

## Hard rules

1. **Never commit secrets.** `.env`, tokens, keys — all gitignored.
   If you see one being added, stop and warn.
2. **Don't touch `../mobile/` or `../backend/`** unless the task
   explicitly requires a coordinated change (e.g. an auth migration).
   When you do, call it out in the commit message.
3. **No new state-management library** (Redux, Zustand, Jotai) until
   `useState` + TanStack Query actually causes pain.
4. **No new UI kit.** Use the existing shadcn/ui + Radix components in
   `client/src/components/ui/`.
5. **Don't store JWTs/secrets in `localStorage`** going forward —
   the auth migration to httpOnly cookies is in progress; new code
   should rely on `withCredentials: true` rather than reading a token.
6. **Don't `console.log` tokens, passwords, or full user objects.**
   Strip these before finishing.
7. **Don't write giant files.** If a `.tsx` file passes ~300 lines,
   split it into smaller components.

## Conventions

- **Files:** `kebab-case.tsx` for components/pages, `camelCase.ts` for
  utility modules. **Components:** `PascalCase`. **Hooks:** `useFoo`.
- **Imports:** absolute (`@/...`, `@shared/...`) before relative,
  alphabetized within groups.
- **Async:** `async`/`await`, not `.then` chains.
- **Forms:** `react-hook-form` + `zod` via `@hookform/resolvers/zod`.
- **Server state:** prefer TanStack Query (`useQuery` / `useMutation`)
  for new data-fetching code rather than ad-hoc `useEffect` + `useState`.
- **Errors surfaced to UI:** show a friendly message; don't dump raw
  axios error objects.

## Definition of done

- `npm run dev` starts cleanly and the touched page renders.
- TypeScript is happy (no new errors in changed files).
- No new `console.log` left behind for sensitive data.
- New API calls match an actual backend route (verified by reading the
  controller).
- Secrets are not in the diff.
- A short note in the chat explains what changed and why.

## Vestigial code to ignore (slated for removal)

- `frontend/server/`, `frontend/shared/schema.ts`, `drizzle.config.ts`,
  and the `express` / `passport` / `drizzle-orm` / `pg` /
  `connect-pg-simple` / `memorystore` / `express-session` deps are
  **leftover Replit scaffold**. The real backend is the Spring services.
  Don't add code that depends on them; they're being removed.

## When unsure

Ask Marius. Don't guess about:
- Which backend service owns an endpoint (read the controllers).
- Whether an endpoint exists at all (read the controllers).
- Auth/session changes — these affect mobile + backend too.
