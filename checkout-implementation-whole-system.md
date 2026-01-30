# Checkout Implementation (Stripe) – End-to-End Guide

## 1) High-level flow
1. **Cart → Order draft**: Validate cart, ensure shipping address, create backend order (status `PENDING`).
2. **Stripe Checkout Session**: Backend creates Stripe Checkout Session for that order; frontend redirects via `stripe.redirectToCheckout`.
3. **Payment completion**: Stripe calls our webhook; backend marks order `PAID` (or `PAYMENT_FAILED`).
4. **Success screen**: Frontend success page polls order status and can call a finalize endpoint as fallback; cart is refreshed once paid.

## 2) Frontend pieces

### 2.1 Services (API contracts)
Key methods from `frontend/src/app/services/api.ts`:
```typescript
// ...existing code...
createCheckoutOrder(request: CreatePaymentIntentRequest): Observable<CreateOrderResponse> {
  return this.http.post<CreateOrderResponse>(`${this.baseUrl}/checkout/create-order`, request);
}

/** Create a Stripe Checkout Session for an existing order */
createCheckoutSession(orderId: string): Observable<{ id: string }> {
  return this.http.post<{ id: string }>(`${this.baseUrl}/checkout/create-session`, { orderId });
}

finalizeCheckout(orderId: number, sessionId?: string): Observable<Order> {
  const payload = sessionId ? { sessionId } : {};
  return this.http.post<Order>(`${this.baseUrl}/orders/${orderId}/finalize`, payload);
}

getOrderByCheckoutSession(sessionId: string): Observable<Order> {
  return this.http.get<Order>(`${this.baseUrl}/orders/checkout-session/${sessionId}`);
}

getLatestOrder(): Observable<Order> {
  return this.http.get<Order>(`${this.baseUrl}/orders/me/latest`);
}
// ...existing code...
```

Types involved:
- `CreatePaymentIntentRequest`: `{ shippingAddress, shippingCity, shippingPostalCode, shippingCountry, shippingAmount?, taxAmount? }`
- `CreateOrderResponse`: `{ orderId, orderNumber, totalCents, currency }`
- `Order`: includes `status`, `stripeCheckoutSessionId`, `paymentIntentId`, amounts, items, etc.

### 2.2 Cart page (`CustomerCartComponent`)
- Validates auth; calls `cartService.validateCart()`.
- Ensures a preferred shipping address is present (`ShippingAddressService`).
- Loads Stripe via `loadStripe(window.STRIPE_PUBLISHABLE_KEY)`.
- Calls `api.createCheckoutOrder` with shipping/tax from the selected address.
- Calls `api.createCheckoutSession(orderId)`; receives `{ id: sessionId }`.
- Redirects: `stripe.redirectToCheckout({ sessionId })`.
- Handles button disabling, error banners, and item-level validation feedback.

### 2.3 Legacy checkout wizard (`CheckoutComponent`)
- Three steps (Review → Shipping → Payment).
- Mirrors the same: create order → create session → redirect.
- Marked TODO to retire; kept for compatibility.

### 2.4 Success page (`CheckoutSuccessComponent`)
- Reads query params: `orderId`, `session_id/sessionId`, `orderNumber`.
- Strategy:
  - If `orderId` provided → fetch order by id.
  - Else if `session_id` provided → `getOrderByCheckoutSession`.
  - Else → `getLatestOrder`.
- Polling loop: stops when `status === 'PAID'` or attempts exhausted.
- On each poll (if not paid) and when sessionId exists → calls `finalizeCheckout(orderId, sessionId)` as a server-side verification fallback.
- Once paid: refreshes cart (`cartService.loadCart`) and shows success state; buttons for “View Order” and “Continue Shopping”.

### 2.5 Stripe publishable key injection
- `frontend/src/index.html` injects `window.STRIPE_PUBLISHABLE_KEY` via script tag (populated at deploy/runtime).

## 3) Backend pieces

### 3.1 CheckoutController
- `POST /api/checkout/create-order`
  - Builds a `PENDING` order from the user’s active cart.
  - Includes shipping/tax from `CreatePaymentIntentRequest`.
  - Locks inventory (if configured) and returns `{ orderId, orderNumber, totalCents, currency }`.
- `POST /api/checkout/create-session`
  - Validates order is `PENDING` and non-empty.
  - Recomputes totals to avoid client tampering.
  - Creates Stripe Checkout Session with line items, amount, currency.
  - Success URL: `<frontend>/customer/checkout-success?orderId={id}&session_id={CHECKOUT_SESSION_ID}`
  - Cancel URL: `<frontend>/customer/cart`
  - Stores `stripeCheckoutSessionId` on the order; returns `{ id: sessionId }`.

### 3.2 StripeWebhookController
- Verifies signature using webhook secret.
- Handles events:
  - `checkout.session.completed`: extracts `orderId` + `payment_intent`; idempotently marks order as `PAID` via `OrderService.markPaid`.
  - `payment_intent.payment_failed`: marks order as failed.
- Delegates side effects (email, inventory, analytics) to `OrderService`.

### 3.3 OrderController finalize fallback
- `POST /api/orders/{orderId}/finalize` (optional `sessionId` body)
  - If webhook already processed → returns current order.
  - Else → retrieves Stripe session using stored/received sessionId, checks payment status, and, if paid, finalizes the order.
  - Returns the latest order state.

### 3.4 OrderService highlights
- `attachCheckoutSession(orderId, sessionId)`: persists session id.
- `markPaid(orderId, paymentIntentId)`: idempotent; updates status, captures payment ids, finalizes side effects.
- `markPaymentFailed(orderId, reason?)`: sets failure status, releases inventory if needed.
- `finalizeCheckout(orderId, sessionId?)`: server-side verification when webhook lagging.

## 4) Webhook and dev tooling
- `start-stripe-listener.sh` (dev helper): runs `stripe listen` and writes the webhook signing secret to `.stripe/webhook-secret`.
- Backend reads Stripe secret from `stripe.secret.key` or `STRIPE_SECRET_KEY`.
- Webhook signing secret read from env/config (ensure set in dev/prod).

## 5) Success/cancel URLs
- Success: `<frontend>/customer/checkout-success?orderId={orderId}&session_id={CHECKOUT_SESSION_ID}`
- Cancel: `<frontend>/customer/cart`

## 6) Implementation checklist (reuse elsewhere)
1. **Frontend**
   - Load Stripe publishable key into `window.STRIPE_PUBLISHABLE_KEY`.
   - Validate cart; ensure shipping address; compute shipping/tax if needed.
   - `createCheckoutOrder(request)` → get `orderId`.
   - `createCheckoutSession(orderId)` → get `sessionId`.
   - `stripe.redirectToCheckout({ sessionId })`.
   - Build a success page that:
     - Reads `orderId`/`session_id` from query params.
     - Polls order status; optionally calls `finalizeCheckout`.
     - Refreshes cart after `PAID`.
2. **Backend**
   - Endpoint to create `PENDING` order from cart (recalculate totals server-side).
   - Endpoint to create Stripe Checkout Session; store `stripeCheckoutSessionId`; return session id.
   - Webhook to handle `checkout.session.completed` and `payment_intent.payment_failed`; mark order accordingly.
   - Optional finalize endpoint to reconcile when webhook delayed.
3. **Config**
   - Stripe secret key (server), webhook secret, publishable key (client).
   - Frontend base URLs for success/cancel; backend base URL exposed via `ENV_API_BASE_URL` when not localhost.

## 7) Edge cases / defenses
- Recompute totals server-side before creating session.
- Validate order status is `PENDING` before session creation.
- Idempotent webhook handling (check existing status/payment ids).
- Fallback finalize endpoint for webhook delays.
- Cart re-validation before order creation; handle expired/invalid carts.
- Inventory lock/release tied to order status transitions.
- Error UX: show item-level availability issues and general failures.

## 8) File map (where to replicate/adjust)
- **Frontend**
  - Services: `frontend/src/app/services/api.ts` (checkout methods), `cart.ts`, `shipping-address.ts`, `auth.ts`.
  - Pages: `customer-cart`, `checkout` (legacy), `checkout-success`.
  - Config: `frontend/src/index.html` (publishable key).
- **Backend**
  - Controllers: `CheckoutController`, `StripeWebhookController`, `OrderController`.
  - Service: `OrderService`.
  - Dev tooling: `start-stripe-listener.sh`.
  - Config: Stripe keys and webhook secret in environment/application properties.

---

### Editor prompt (reuse)
Use this prompt inside the code editor to scaffold or adapt the checkout flow elsewhere:

> “Implement a Stripe Checkout flow that (1) validates cart and shipping, (2) creates a PENDING order server-side, (3) creates and stores a Stripe Checkout Session with success/cancel URLs, (4) redirects with `stripe.redirectToCheckout`, (5) processes `checkout.session.completed` via webhook to mark the order PAID, (6) provides a success page that polls order status and can call a finalize endpoint when webhook is delayed, and (7) refreshes the cart once paid. Match the patterns in `api.ts` (createCheckoutOrder/createCheckoutSession/finalizeCheckout), the cart and checkout components, and the Stripe webhook controller.”