# Copilot Custom Instructions for This Repository

Use this file as the default operating guide when making changes in this project.

## 1) Scope and priorities

- This repository is a **split app**:
  - Frontend at repo root: Next.js + TypeScript + Tailwind.
  - Backend in `backend/`: Spring Boot (Java 21, Maven).
- Keep changes **minimal and targeted**. Do not refactor unrelated areas.
- Prefer existing patterns over introducing new architecture.
- If docs conflict, prefer:
  1. Actual code behavior in the current tree,
  2. Newer operational docs,
  3. Older implementation summaries.

## 2) Canonical tooling and commands

### Frontend (root)

- Package manager default: **npm**.
- Use scripts from `package.json`:
  - `npm run dev`
  - `npm run build`
  - `npm run lint`

### Backend (`backend/`)

- Java version target: **21**.
- Build/test with Maven:
  - `mvn test`
  - `mvn verify` (when broader validation is needed)

## 3) Deployment assumptions

- Treat **Netlify (frontend) + Heroku (backend)** as canonical deployment path.
- Keep CORS and API URL behavior compatible with this flow:
  - `NEXT_PUBLIC_API_URL` must not have a trailing slash.
  - Heroku `ALLOWED_ORIGINS` / `FRONTEND_URL` must match the Netlify origin exactly.
- GitHub Pages docs/workflow exist but are secondary; do not optimize primary behavior for static-only GH Pages unless explicitly requested.

## 4) Sensitive areas (light guardrails)

When modifying the areas below, keep logic equivalent unless the task explicitly changes behavior.

### Checkout / Stripe

Preserve these invariants unless asked to change them:

- Order is created server-side as `PENDING` before payment session.
- Checkout session is created server-side and tied to an order.
- Backend recomputes totals server-side before charging.
- Webhook updates payment state (`PAID` / failed) idempotently.
- Success/finalize fallback should remain robust to webhook delays.

### Quote-only products

- `quoteOnly` products must not be addable to cart via backend APIs.
- Frontend should present quote-request UX instead of normal add-to-cart for quote-only products.

### CORS / API base URL

- Avoid introducing double-slash API paths (e.g., `//api/...`).
- Keep frontend URL generation and backend CORS config aligned to deployed origins.

## 5) Change workflow expectations

- Before editing, locate existing implementation first (search/read) instead of rewriting from scratch.
- Touch only required files.
- Keep naming and code style consistent with nearby code.
- Do not add dependencies unless necessary.
- Update docs only when behavior or operational steps actually change.

## 6) Validation expectations

Run the smallest useful checks for changed areas, then broaden if needed.

### Frontend changes

- Run `npm run lint`.
- Run `npm run build` if routing/config/build behavior changed.

### Backend changes

- Run `mvn test` in `backend/`.
- Use `mvn verify` if changes affect integration-heavy areas.

### Checkout, quote-only, or CORS changes

- Add a quick manual verification note in PR/summary for:
  - checkout happy path,
  - quote-only cart blocking,
  - API URL/CORS behavior.

## 7) Source docs to consult when relevant

- `DEPLOYMENT_GUIDE.md`
- `DEPLOYMENT_CHECKLIST.md`
- `IMMEDIATE_FIX_GUIDE.md`
- `CORS_FIX_NOW.md`
- `checkout-implementation-whole-system.md`
- `STRIPE_INTEGRATION.md`
- `QUOTE_ONLY_IMPLEMENTATION.md`
- `DEPLOYMENT.md` (GH Pages notes; secondary)

## 8) What to avoid

- Do not switch the repo to pnpm by default.
- Do not remove webhook/finalize safeguards in checkout flows.
- Do not weaken backend enforcement for quote-only products.
- Do not hardcode environment-specific production URLs in source code.