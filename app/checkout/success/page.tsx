"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Container } from "@/components/container"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { CheckCircle2, Loader2, ShoppingBag, FileText } from "lucide-react"
import Link from "next/link"
import { apiRequest } from "@/lib/apiClient"

interface Order {
  id: number
  orderNumber: string
  status: string
  totalAmount: number
  currency: string
  shippingAmount?: number
  taxAmount?: number
  orderItems?: Array<{
    id: number
    productName: string
    quantity: number
    unitPrice: number
    totalPrice: number
  }>
}

export default function CheckoutSuccessPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [polling, setPolling] = useState(true)

  const orderId = searchParams.get("orderId")
  const sessionId = searchParams.get("session_id")

  useEffect(() => {
    if (!orderId && !sessionId) {
      setError("No order information found")
      setLoading(false)
      setPolling(false)
      return
    }

    let pollCount = 0
    const maxPolls = 20
    const pollInterval = 1500 // 1.5 seconds

    const fetchOrder = async () => {
      try {
        let response: Order

        if (orderId) {
          response = await apiRequest<Order>(`/api/orders/${orderId}`)
        } else if (sessionId) {
          response = await apiRequest<Order>(`/api/orders/checkout-session/${sessionId}`)
        } else {
          return
        }

        if (response) {
          setOrder(response)
          setError("")

          // If order is paid, stop polling
          if (response.status === "PAID") {
            setPolling(false)
            setLoading(false)
          } else if (pollCount < maxPolls) {
            pollCount++
            setTimeout(fetchOrder, pollInterval)
          } else {
            setPolling(false)
            setLoading(false)
            setError("Payment is still being confirmed. Please check your email or contact support.")
          }
        }
      } catch (err: any) {
        console.error("Error fetching order:", err)
        
        if (pollCount < maxPolls) {
          pollCount++
          setTimeout(fetchOrder, pollInterval)
        } else {
          setError("Unable to load order details. Please check your email for confirmation.")
          setLoading(false)
          setPolling(false)
        }
      }
    }

    fetchOrder()
  }, [orderId, sessionId])

  const calculateSubtotal = () => {
    if (!order?.orderItems) return 0
    return order.orderItems.reduce((sum, item) => sum + (item.totalPrice || item.unitPrice * item.quantity), 0)
  }

  if (loading && !order) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card>
            <CardContent className="flex flex-col items-center justify-center gap-6 py-20 text-center">
              <Loader2 className="h-16 w-16 animate-spin text-primary" />
              <div>
                <h2 className="mb-2 text-2xl font-bold">Processing your order...</h2>
                <p className="text-muted-foreground">
                  Please wait while we confirm your payment
                </p>
              </div>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <Card>
          <CardContent className="py-12 px-6 sm:px-12">
            {/* Success Icon */}
            <div className="flex justify-center mb-6">
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-green-100">
                <CheckCircle2 className="h-12 w-12 text-green-600" />
              </div>
            </div>

            {/* Title */}
            <h1 className="mb-4 text-center text-3xl font-bold">Order Confirmed!</h1>
            <p className="mb-8 text-center text-lg text-muted-foreground">
              Thank you for your purchase. Your order has been successfully placed.
            </p>

            {/* Loading Status */}
            {polling && (
              <div className="mb-6 rounded-lg border border-blue-200 bg-blue-50 p-4">
                <p className="text-sm text-blue-900">
                  We're confirming your payment details. This usually takes just a moment.
                </p>
              </div>
            )}

            {/* Payment Confirmed */}
            {order?.status === "PAID" && (
              <div className="mb-6 rounded-lg border border-green-200 bg-green-50 p-4">
                <p className="text-sm font-medium text-green-900">
                  ✓ Payment confirmed. We've finalized your order and emailed a receipt for your records.
                </p>
              </div>
            )}

            {/* Payment Processing */}
            {order && order.status !== "PAID" && !polling && (
              <div className="mb-6 rounded-lg border border-yellow-200 bg-yellow-50 p-4">
                <p className="text-sm font-medium text-yellow-900">
                  Your payment is still processing. We're waiting for final confirmation from your payment provider.
                </p>
              </div>
            )}

            {/* Error Message */}
            {error && (
              <div className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4">
                <p className="text-sm text-red-900">{error}</p>
              </div>
            )}

            {/* Order Details */}
            {order && (
              <div className="mx-auto max-w-2xl">
                <div className="mb-6 rounded-lg bg-muted p-6">
                  <div className="mb-4 flex justify-between">
                    <span className="font-medium">Order Number:</span>
                    <span className="font-semibold">{order.orderNumber}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-medium">Status:</span>
                    <span className={`rounded-full px-3 py-1 text-sm font-medium ${
                      order.status === "PAID" 
                        ? "bg-green-100 text-green-800" 
                        : "bg-yellow-100 text-yellow-800"
                    }`}>
                      {order.status}
                    </span>
                  </div>
                </div>

                {/* Payment Summary */}
                <div className="mb-8 rounded-lg border p-6">
                  <h2 className="mb-4 text-xl font-bold">Payment Summary</h2>
                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Items total</span>
                      <span className="font-medium">
                        £{calculateSubtotal().toFixed(2)}
                      </span>
                    </div>
                    {order.shippingAmount && order.shippingAmount > 0 && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Shipping</span>
                        <span className="font-medium">
                          £{order.shippingAmount.toFixed(2)}
                        </span>
                      </div>
                    )}
                    {order.taxAmount && order.taxAmount > 0 && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Tax</span>
                        <span className="font-medium">
                          £{order.taxAmount.toFixed(2)}
                        </span>
                      </div>
                    )}
                    <div className="border-t pt-3">
                      <div className="flex justify-between text-lg font-bold">
                        <span>Total paid</span>
                        <span>£{order.totalAmount.toFixed(2)}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-col gap-4 sm:flex-row sm:justify-center">
              <Button asChild size="lg">
                <Link href="/shop">
                  <ShoppingBag className="mr-2 h-5 w-5" />
                  Continue Shopping
                </Link>
              </Button>
              {order && (
                <Button asChild variant="outline" size="lg">
                  <Link href="/account/orders">
                    <FileText className="mr-2 h-5 w-5" />
                    View Orders
                  </Link>
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </Container>
    </section>
  )
}
