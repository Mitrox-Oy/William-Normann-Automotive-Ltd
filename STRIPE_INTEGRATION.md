# Stripe Checkout Integration Guide

## Overview

This project implements Stripe Checkout for payment processing, matching the pattern used in the e-commerce-site project. The system creates an order in the database first, then redirects to Stripe Checkout for payment, and finally processes the payment webhook to mark the order as paid.

## Architecture

### Flow Diagram
```
1. User adds items to cart
2. User clicks "Proceed to Checkout"
3. Frontend creates order via POST /api/checkout/create-order (status: PENDING)
4. Frontend creates Stripe session via POST /api/checkout/create-session
5. Frontend redirects user to Stripe Checkout
6. User completes payment on Stripe
7. Stripe redirects to /checkout/success?orderId=X&session_id=Y
8. Stripe sends webhook to /api/stripe/webhook (event: checkout.session.completed)
9. Backend marks order as PAID and sends confirmation email
10. Success page polls order status until PAID
```

## Backend Setup (Already Completed)

The backend already has:
- ✅ Stripe Java SDK (version 24.18.0)
- ✅ CheckoutController with create-order and create-session endpoints
- ✅ StripeWebhookController for handling payment events
- ✅ STRIPE_SECRET_KEY environment variable (configured in Heroku)
- ✅ STRIPE_WEBHOOK_SECRET environment variable (configured in Heroku)

## Frontend Setup (Just Completed)

### 1. Installed Dependencies
```bash
npm install @stripe/stripe-js
```

### 2. Environment Variables

Add to `.env.production` (for Netlify):
```env
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_your_key_here
```

**IMPORTANT:** You need to set this in Netlify:
1. Go to Netlify Dashboard → Site settings → Environment variables
2. Add `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` with your Stripe publishable key
3. Get your key from: https://dashboard.stripe.com/test/apikeys

### 3. Files Created/Modified

#### Created:
- `/app/checkout/success/page.tsx` - Order confirmation page with payment polling
- Added checkout functions to `/lib/shopApi.ts`

#### Modified:
- `/app/cart/page.tsx` - Added Stripe checkout button and flow

## Stripe Dashboard Setup

### 1. Get API Keys
1. Go to https://dashboard.stripe.com/test/apikeys
2. Copy your **Publishable key** (starts with `pk_test_`)
3. Copy your **Secret key** (starts with `sk_test_`) - this is already in Heroku

### 2. Configure Webhook
1. Go to https://dashboard.stripe.com/test/webhooks
2. Click "Add endpoint"
3. Enter URL: `https://william-normann-330b912b355b.herokuapp.com/api/stripe/webhook`
4. Select events to listen for:
   - `checkout.session.completed`
   - `payment_intent.payment_failed`
5. Copy the **Signing secret** (starts with `whsec_`)
6. Add to Heroku config vars as `STRIPE_WEBHOOK_SECRET`

## Configuration Checklist

### Heroku (Backend)
- [x] `STRIPE_SECRET_KEY` - Your Stripe secret key (sk_test_...)
- [x] `STRIPE_WEBHOOK_SECRET` - Your webhook signing secret (whsec_...)
- [x] `FRONTEND_URL` - https://william-normann.netlify.app

### Netlify (Frontend)
- [ ] `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` - Your Stripe publishable key (pk_test_...)
- [x] `NEXT_PUBLIC_API_URL` - https://william-normann-330b912b355b.herokuapp.com

## Testing the Integration

### Test Mode (No Real Money)
1. Use Stripe test mode keys (pk_test_, sk_test_)
2. Test cards: https://stripe.com/docs/testing
   - Success: 4242 4242 4242 4242
   - Decline: 4000 0000 0000 0002
   - 3D Secure: 4000 0027 6000 3184

### Test Flow
1. Login as a customer (customer@example.com / password)
2. Add a product to cart (e.g., /shop/carclean001)
3. Click "Proceed to Checkout"
4. You'll be redirected to Stripe Checkout
5. Use test card: 4242 4242 4242 4242
   - Any future expiry date
   - Any 3-digit CVC
   - Any postal code
6. Complete payment
7. You'll be redirected to /checkout/success
8. Page will poll until order status is PAID
9. Check Heroku logs for webhook processing

### Verify Webhook
```bash
# Check Heroku logs after test payment
heroku logs --tail --app william-normann

# Look for:
# - "Created Stripe Checkout Session"
# - "Order marked as PAID via webhook"
```

## Order Lifecycle

### Status Flow
```
PENDING → CHECKOUT_CREATED → PAID → PROCESSING → SHIPPED → DELIVERED
```

- **PENDING**: Order created from cart
- **CHECKOUT_CREATED**: Stripe session attached
- **PAID**: Payment confirmed via webhook
- **PROCESSING**: Order being prepared (manual step)
- **SHIPPED**: Order dispatched (manual step)
- **DELIVERED**: Order received (manual step)

## URLs

### Success/Cancel URLs
Configured in backend CheckoutController:
- Success: `https://william-normann.netlify.app/checkout/success?orderId={orderId}&session_id={CHECKOUT_SESSION_ID}`
- Cancel: `https://william-normann.netlify.app/cart`

## API Endpoints

### Public (No Auth)
- `POST /api/checkout/create-session` - Create Stripe session

### Authenticated (Bearer Token)
- `POST /api/checkout/create-order` - Create order from cart
- `GET /orders/{id}` - Get order details
- `GET /orders/checkout-session/{sessionId}` - Get order by session

### Webhook (Stripe Signature)
- `POST /api/stripe/webhook` - Handle payment events

## Troubleshooting

### "Stripe secret key not configured"
- Check Heroku config: `heroku config --app william-normann`
- Should see `STRIPE_SECRET_KEY=sk_test_...`

### "Invalid webhook signature"
- Check `STRIPE_WEBHOOK_SECRET` in Heroku
- Make sure webhook endpoint URL is correct in Stripe Dashboard
- Verify webhook is sending to HTTPS (not HTTP)

### Order stuck in CHECKOUT_CREATED
- Check if webhook is being received (Heroku logs)
- Verify webhook endpoint is accessible: https://william-normann-330b912b355b.herokuapp.com/api/stripe/webhook
- Check Stripe Dashboard → Webhooks → Recent events

### "Failed to initialize payment system"
- Check Netlify environment variables
- Make sure `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` is set
- Key should start with `pk_test_` or `pk_live_`

## Moving to Production

### Switch to Live Mode
1. Get live API keys from Stripe Dashboard (live mode)
2. Update environment variables:
   - Heroku: `STRIPE_SECRET_KEY=sk_live_...`
   - Netlify: `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_live_...`
3. Create new webhook endpoint in live mode
   - URL: `https://william-normann-330b912b355b.herokuapp.com/api/stripe/webhook`
   - Update Heroku: `STRIPE_WEBHOOK_SECRET=whsec_live_...`

### Important Notes
- Test mode and live mode have separate keys
- Test mode orders won't appear in live mode
- Always test thoroughly in test mode first
- Enable Stripe Radar for fraud prevention in live mode

## Support

### Stripe Resources
- Dashboard: https://dashboard.stripe.com
- Documentation: https://stripe.com/docs
- Testing: https://stripe.com/docs/testing
- Webhooks: https://stripe.com/docs/webhooks

### Project Resources
- Backend: https://william-normann-330b912b355b.herokuapp.com
- Frontend: https://william-normann.netlify.app
- GitHub: https://github.com/Mitrox-Oy/William-Normann-Automotive-Ltd
