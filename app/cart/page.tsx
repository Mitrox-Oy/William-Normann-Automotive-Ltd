"use client"

import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { useCart } from "@/components/CartContext"
import { useAuth } from "@/components/AuthProvider"
import { formatCurrency, createCheckoutOrder, createCheckoutSession, previewDiscountCode, syncCartToBackend, type DiscountPreviewResponse } from "@/lib/shopApi"
import { openLeadInWhatsApp } from "@/lib/whatsappLead"
import { getImageUrl } from "@/lib/utils"
import { ShoppingCart, Minus, Plus, Trash2, ArrowLeft, FileText, Package, CreditCard, Loader2, CheckCircle2, MessageSquare } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { motion } from "framer-motion"
import { loadStripe } from "@stripe/stripe-js"
import { useEffect, useState } from "react"

// Initialize Stripe
const stripePublishableKey = process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY || ''
const stripePromise = loadStripe(stripePublishableKey)
const STRIPE_CHECKOUT_THRESHOLD = 2000

export default function CartPage() {
  const { items, removeItem, updateQuantity, clearCart, getTotalItems, getTotalPrice } = useCart()
  const { isAuthenticated, token, user } = useAuth()
  const router = useRouter()
  const [isProcessingCheckout, setIsProcessingCheckout] = useState(false)
  const [checkoutError, setCheckoutError] = useState<string>("")
  const [discountCodeInput, setDiscountCodeInput] = useState("")
  const [isApplyingDiscount, setIsApplyingDiscount] = useState(false)
  const [discountError, setDiscountError] = useState("")
  const [appliedDiscountCode, setAppliedDiscountCode] = useState<string>("")
  const [discountPreview, setDiscountPreview] = useState<DiscountPreviewResponse | null>(null)
  const [showQuoteDialog, setShowQuoteDialog] = useState(false)
  const [quoteFormState, setQuoteFormState] = useState<"idle" | "loading" | "success" | "error">("idle")

  const totalItems = getTotalItems()
  const totalPrice = getTotalPrice()
  const currency = items[0]?.product.currency || 'USD'
  const requiresQuoteOnly = totalPrice > STRIPE_CHECKOUT_THRESHOLD
  const appliedCodeSavings = discountPreview?.valid ? discountPreview.codeSavings : 0
  const finalCheckoutTotal = discountPreview?.valid ? discountPreview.totalAfterDiscount : totalPrice
  const cartSummaryLines = items.map((item, index) => {
    const partNumber = item.product.partNumber || item.product.sku || "N/A"
    const lineTotal = formatCurrency(item.product.price * item.quantity, item.product.currency)
    return `${index + 1}. ${item.product.name} (Part #: ${partNumber}) x ${item.quantity} - ${lineTotal}`
  })

  useEffect(() => {
    if (!appliedDiscountCode || items.length === 0) return
    let canceled = false

    async function refreshDiscountPreview() {
      try {
        const preview = await previewDiscountCode(
          appliedDiscountCode,
          items.map((item) => ({ productId: item.product.id, quantity: item.quantity })),
        )
        if (canceled) return
        if (!preview.valid) {
          setAppliedDiscountCode("")
          setDiscountPreview(null)
          setDiscountError(preview.message || "Discount code is no longer valid.")
          return
        }
        setDiscountPreview(preview)
        setDiscountError("")
      } catch (error: any) {
        if (canceled) return
        setAppliedDiscountCode("")
        setDiscountPreview(null)
        setDiscountError(error?.message || "Failed to refresh discount.")
      }
    }

    refreshDiscountPreview()
    return () => {
      canceled = true
    }
  }, [appliedDiscountCode, items])

  useEffect(() => {
    if (items.length !== 0) return
    setAppliedDiscountCode("")
    setDiscountPreview(null)
    setDiscountCodeInput("")
    setDiscountError("")
  }, [items.length])

  const handleRequestQuote = () => {
    setCheckoutError("")
    setQuoteFormState("idle")
    setShowQuoteDialog(true)
  }

  const handleApplyDiscount = async () => {
    const normalizedCode = discountCodeInput.trim().toUpperCase()
    if (!normalizedCode) {
      setDiscountError("Enter a discount code first.")
      return
    }

    setIsApplyingDiscount(true)
    setDiscountError("")

    try {
      const preview = await previewDiscountCode(
        normalizedCode,
        items.map((item) => ({ productId: item.product.id, quantity: item.quantity })),
      )

      if (!preview.valid) {
        setAppliedDiscountCode("")
        setDiscountPreview(null)
        setDiscountError(preview.message || "Discount code is invalid.")
        return
      }

      setAppliedDiscountCode(preview.code || normalizedCode)
      setDiscountPreview(preview)
      setDiscountCodeInput(preview.code || normalizedCode)
      setDiscountError("")
    } catch (error: any) {
      setAppliedDiscountCode("")
      setDiscountPreview(null)
      setDiscountError(error?.message || "Failed to apply discount code.")
    } finally {
      setIsApplyingDiscount(false)
    }
  }

  const handleRemoveDiscount = () => {
    setAppliedDiscountCode("")
    setDiscountPreview(null)
    setDiscountError("")
  }

  const handleCartQuoteSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setQuoteFormState("loading")

    const formData = new FormData(e.currentTarget)
    const name = String(formData.get("name") || "")
    const email = String(formData.get("email") || "")
    const phone = String(formData.get("phone") || "")
    const userMessage = String(formData.get("message") || "").trim()
    const cartMessage = `Cart Items:\n${cartSummaryLines.join("\n")}\nEstimated Total: ${formatCurrency(totalPrice, currency)}`
    const composedMessage = userMessage ? `${userMessage}\n\n${cartMessage}` : cartMessage

    try {
      openLeadInWhatsApp({
        source: "cart_quote",
        name,
        email,
        phone: phone || null,
        message: composedMessage,
        product: `${totalItems} cart item${totalItems === 1 ? "" : "s"}`,
        quantity: totalItems.toString(),
      })
      setQuoteFormState("success")
    } catch (error) {
      console.error("Cart quote submission error:", error)
      setQuoteFormState("error")
    }
  }

  const handleCheckout = async () => {
    if (requiresQuoteOnly) {
      setCheckoutError("Orders above $2,000 must be submitted as a quote request.")
      return
    }

    if (!isAuthenticated || !token) {
      router.push('/login?redirect=/cart')
      return
    }

    setIsProcessingCheckout(true)
    setCheckoutError("")

    if (!stripePublishableKey) {
      setCheckoutError("Stripe key missing: configure NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY")
      setIsProcessingCheckout(false)
      return
    }

    try {
      // Step 0: Sync cart to backend (localStorage cart → backend cart)
      await syncCartToBackend(items.map(item => ({
        productId: item.product.id,
        quantity: item.quantity,
      })))

      // Step 1: Create order from cart
      const orderResponse = await createCheckoutOrder({
        shippingAddress: "To be provided",
        shippingCity: "To be provided",
        shippingPostalCode: "To be provided",
        shippingCountry: "GB",
        shippingAmount: 0,
        taxAmount: 0,
        discountCode: appliedDiscountCode || undefined,
      })

      if (!orderResponse?.orderId) {
        throw new Error('Failed to create order')
      }

      // Step 2: Create Stripe Checkout Session
      const sessionResponse = await createCheckoutSession(String(orderResponse.orderId))

      if (!sessionResponse?.id || !sessionResponse?.url) {
        throw new Error('Failed to create checkout session')
      }

      // Step 3: Redirect to Stripe Checkout
      // Using direct URL redirect (redirectToCheckout is deprecated)
      window.location.href = sessionResponse.url
    } catch (error: any) {
      console.error('Error proceeding to checkout:', error)
      let errorMessage = 'Failed to proceed to checkout. Please try again.'

      // Extract error message from ApiException or error object
      if (error?.message) {
        errorMessage = error.message
      } else if (error?.error) {
        errorMessage = error.error
      }

      // Log detailed error for debugging
      console.error('Checkout error details:', {
        message: error?.message,
        status: error?.status,
        errors: error?.errors,
        error: error?.error
      })

      setCheckoutError(errorMessage)
    } finally {
      setIsProcessingCheckout(false)
    }
  }

  if (items.length === 0) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card>
            <CardContent className="flex flex-col items-center justify-center gap-6 py-20 text-center">
              <ShoppingCart className="h-24 w-24 text-muted-foreground" />
              <div>
                <h2 className="mb-2 text-2xl font-bold">Your cart is empty</h2>
                <p className="text-muted-foreground">
                  Add some products from our shop to get started
                </p>
              </div>
              <Button asChild size="lg">
                <Link href="/shop">
                  Browse Shop
                </Link>
              </Button>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link
            href="/shop"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Continue Shopping
          </Link>
        </div>

        <SectionHeading
          title={`Shopping Cart (${totalItems} ${totalItems === 1 ? 'item' : 'items'})`}
          subtitle="Review your items and request a quote"
          className="mb-12"
        />

        <div className="grid gap-8 lg:grid-cols-3">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {items.map((item, index) => (
              <motion.div
                key={item.product.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
              >
                <Card>
                  <CardContent className="p-6">
                    <div className="flex gap-6">
                      {/* Image */}
                      <Link
                        href={`/shop/${item.product.slug}`}
                        className="relative h-24 w-24 flex-shrink-0 overflow-hidden rounded-lg bg-muted"
                      >
                        {item.product.images[0] ? (
                          <Image
                            src={getImageUrl(item.product.images[0])}
                            alt={item.product.name}
                            fill
                            className="object-cover"
                          />
                        ) : (
                          <div className="flex h-full items-center justify-center">
                            <Package className="h-8 w-8 text-muted-foreground" />
                          </div>
                        )}
                      </Link>

                      {/* Product Info */}
                      <div className="flex flex-1 flex-col">
                        <div className="flex justify-between gap-4">
                          <div className="flex-1">
                            <Link
                              href={`/shop/${item.product.slug}`}
                              className="font-semibold hover:text-primary transition-colors"
                            >
                              {item.product.name}
                            </Link>
                            {item.product.partNumber && (
                              <p className="mt-1 text-sm text-muted-foreground">
                                Part #: {item.product.partNumber}
                              </p>
                            )}
                            {item.product.leadTime && (
                              <p className="mt-1 text-xs text-muted-foreground">
                                Lead time: {item.product.leadTime}
                              </p>
                            )}
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => removeItem(item.product.id)}
                            className="h-8 w-8 text-destructive hover:text-destructive bg-white hover:bg-white/90 rounded-full"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>

                        <div className="mt-4 flex items-center justify-between">
                          {/* Quantity Controls */}
                          <div className="flex items-center gap-3">
                            <Button
                              variant="outline"
                              size="icon"
                              onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                              disabled={item.quantity <= (item.product.minQuantity || 1)}
                              className="h-8 w-8"
                            >
                              <Minus className="h-4 w-4" />
                            </Button>
                            <span className="w-12 text-center font-semibold">
                              {item.quantity}
                            </span>
                            <Button
                              variant="outline"
                              size="icon"
                              onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                              className="h-8 w-8"
                            >
                              <Plus className="h-4 w-4" />
                            </Button>
                          </div>

                          {/* Price */}
                          <div className="text-right">
                            <p className="text-lg font-bold">
                              {formatCurrency(item.product.price * item.quantity, item.product.currency)}
                            </p>
                            <p className="text-xs text-muted-foreground">
                              {formatCurrency(item.product.price, item.product.currency)} each
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}

            {/* Clear Cart Button */}
            <Button
              variant="outline"
              onClick={clearCart}
              className="w-full text-destructive hover:bg-destructive hover:text-destructive-foreground"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              Clear Cart
            </Button>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-24">
              <CardContent className="p-6 space-y-6">
                <h3 className="text-xl font-bold">Order Summary</h3>

                <Separator />

                <div className="space-y-3">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Subtotal</span>
                    <span className="font-medium">{formatCurrency(totalPrice, currency)}</span>
                  </div>
                  {appliedCodeSavings > 0 && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Discount ({appliedDiscountCode})</span>
                      <span className="font-medium text-green-600">-{formatCurrency(appliedCodeSavings, currency)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Total Items</span>
                    <span className="font-medium">{totalItems}</span>
                  </div>
                  <Separator />
                  <div className="flex justify-between">
                    <span className="font-semibold">Estimated Total</span>
                    <span className="text-xl font-bold">{formatCurrency(finalCheckoutTotal, currency)}</span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Final pricing, shipping, and duties will be calculated in your quote
                  </p>
                </div>

                <Separator />

                <div className="space-y-3">
                  <Label htmlFor="discount-code">Discount Code</Label>
                  <div className="flex gap-2">
                    <Input
                      id="discount-code"
                      value={discountCodeInput}
                      onChange={(e) => setDiscountCodeInput(e.target.value)}
                      placeholder="Enter code"
                      className="uppercase"
                    />
                    <Button type="button" variant="outline" onClick={handleApplyDiscount} disabled={isApplyingDiscount}>
                      {isApplyingDiscount ? "Applying..." : "Apply"}
                    </Button>
                  </div>
                  {appliedDiscountCode && discountPreview?.valid && (
                    <div className="flex items-center justify-between rounded-lg border border-green-200 bg-green-50 px-3 py-2">
                      <p className="text-xs text-green-800">
                        {appliedDiscountCode} applied. You save {formatCurrency(appliedCodeSavings, currency)}.
                      </p>
                      <Button type="button" variant="ghost" size="sm" onClick={handleRemoveDiscount}>
                        Remove
                      </Button>
                    </div>
                  )}
                  {discountError && (
                    <p className="text-xs text-red-700">{discountError}</p>
                  )}
                </div>

                <Separator />

                {checkoutError && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-3">
                    <p className="text-sm text-red-900">{checkoutError}</p>
                  </div>
                )}

                <div className="space-y-3">
                  {requiresQuoteOnly ? (
                    <>
                      {/* Quote-required info panel */}
                      <div className="rounded-xl border border-white/10 bg-gradient-to-br from-white/5 to-white/[0.02] p-4">
                        <div className="flex items-start gap-3">
                          <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/15 ring-1 ring-primary/30">
                            <MessageSquare className="h-4 w-4 text-primary" />
                          </div>
                          <div>
                            <p className="text-sm font-semibold leading-tight">
                              This order goes through our quote process
                            </p>
                            <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                              Orders above $2,000 receive a personalised quote with accurate shipping, duties, and lead times — handled directly by our team.
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* Quote is primary CTA */}
                      <Button onClick={handleRequestQuote} size="lg" className="w-full">
                        <FileText className="mr-2 h-5 w-5" />
                        Request a Quote
                      </Button>

                      {/* Stripe checkout quietly disabled */}
                      <Button
                        size="lg"
                        variant="outline"
                        className="w-full cursor-not-allowed opacity-40"
                        disabled
                        title="Checkout is not available for orders above $2,000"
                      >
                        <CreditCard className="mr-2 h-5 w-5" />
                        Checkout Unavailable
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button
                        onClick={handleCheckout}
                        size="lg"
                        className="w-full"
                        disabled={isProcessingCheckout}
                      >
                        {isProcessingCheckout ? (
                          <>
                            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                            Processing...
                          </>
                        ) : (
                          <>
                            <CreditCard className="mr-2 h-5 w-5" />
                            Proceed to Checkout
                          </>
                        )}
                      </Button>
                      <Button onClick={handleRequestQuote} variant="outline" size="lg" className="w-full">
                        <FileText className="mr-2 h-5 w-5" />
                        Request Quote Instead
                      </Button>
                      {!isAuthenticated && (
                        <p className="text-center text-xs text-muted-foreground">
                          You'll be asked to sign in to complete checkout
                        </p>
                      )}
                    </>
                  )}
                </div>

                <Separator />

                {/* Info Cards */}
                <div className="space-y-3">
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </Container>

      <Dialog open={showQuoteDialog} onOpenChange={setShowQuoteDialog}>
        <DialogContent className="sm:max-w-[520px]">
          <DialogHeader>
            <DialogTitle>Request a Cart Quote</DialogTitle>
            <DialogDescription>
              Submit your cart details through the same WhatsApp quote flow.
            </DialogDescription>
          </DialogHeader>

          {quoteFormState === "success" ? (
            <div className="py-6 text-center">
              <CheckCircle2 className="mx-auto mb-4 h-16 w-16 text-primary" />
              <h3 className="mb-3 text-xl font-bold">Quote Request Ready</h3>
              <p className="mb-6 text-muted-foreground">
                Your cart quote details were prepared for WhatsApp.
              </p>
              <Button onClick={() => setShowQuoteDialog(false)}>Close</Button>
            </div>
          ) : (
            <form onSubmit={handleCartQuoteSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="cart-quote-name">
                  Full Name <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cart-quote-name"
                  name="name"
                  required
                  placeholder="John Smith"
                  defaultValue={user?.name || ""}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="cart-quote-email">
                  Email Address <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="cart-quote-email"
                  name="email"
                  type="email"
                  required
                  placeholder="john@example.com"
                  defaultValue={user?.email || ""}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="cart-quote-phone">Phone Number</Label>
                <Input id="cart-quote-phone" name="phone" type="tel" placeholder="+1 (555) 123-4567" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="cart-quote-message">Additional Details</Label>
                <Textarea
                  id="cart-quote-message"
                  name="message"
                  placeholder="Any specific requirements or delivery notes..."
                  rows={4}
                />
              </div>

              <div className="rounded-lg border border-white/15 bg-gradient-to-br from-slate-900/85 via-slate-800/75 to-slate-900/85 p-3 text-sm shadow-sm backdrop-blur-sm">
                <p className="font-medium">Cart Quote Details:</p>
                <p className="text-slate-200">Items: {totalItems}</p>
                <p className="text-slate-200">Estimated Total: {formatCurrency(totalPrice, currency)}</p>
                <div className="mt-2 space-y-1 text-xs text-slate-200">
                  {cartSummaryLines.map((line, index) => (
                    <p key={`${index}-${line}`}>{line}</p>
                  ))}
                </div>
              </div>

              {quoteFormState === "error" && (
                <p className="text-sm text-destructive">
                  There was an error preparing your quote request. Please try again.
                </p>
              )}

              <div className="flex gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  className="flex-1"
                  disabled={quoteFormState === "loading"}
                  onClick={() => setShowQuoteDialog(false)}
                >
                  Cancel
                </Button>
                <Button type="submit" className="flex-1" disabled={quoteFormState === "loading"}>
                  {quoteFormState === "loading" ? "Submitting..." : "Submit Request"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </section>
  )
}
