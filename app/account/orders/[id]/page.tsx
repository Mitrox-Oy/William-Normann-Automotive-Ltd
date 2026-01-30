"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { RequireAuth } from "@/components/AuthProvider"
import { getOrder, type CustomerOrder } from "@/lib/accountApi"
import { formatCurrency } from "@/lib/shopApi"
import { ChevronLeft, Loader2 } from "lucide-react"

function AccountOrderDetailContent() {
    const params = useParams()
    const id = Array.isArray(params?.id) ? params.id[0] : params?.id
    const [order, setOrder] = useState<CustomerOrder | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!id) return
        async function loadOrder() {
            try {
                setLoading(true)
                const data = await getOrder(String(id))
                setOrder(data)
                setError(null)
            } catch (err: any) {
                console.error("Failed to load order:", err)
                setError(err?.message || "Failed to load order")
            } finally {
                setLoading(false)
            }
        }

        loadOrder()
    }, [id])

    const statusLabel = order?.status ? order.status.charAt(0).toUpperCase() + order.status.slice(1) : ""

    return (
        <section className="py-24 lg:py-32">
            <Container>
                <div className="mb-8">
                    <Link href="/account/orders" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
                        <ChevronLeft className="mr-1 h-4 w-4" />
                        Back to Orders
                    </Link>
                </div>

                <SectionHeading
                    title={order ? `Order #${order.orderNumber}` : "Order Details"}
                    subtitle="Review your order details"
                    className="mb-8"
                />

                {loading ? (
                    <Card>
                        <CardContent className="flex items-center justify-center gap-3 py-16">
                            <Loader2 className="h-5 w-5 animate-spin" />
                            <span className="text-muted-foreground">Loading order...</span>
                        </CardContent>
                    </Card>
                ) : error ? (
                    <Card>
                        <CardContent className="py-10 text-center">
                            <p className="text-sm text-destructive mb-4">{error}</p>
                            <Button asChild variant="outline">
                                <Link href="/account/orders">Back to Orders</Link>
                            </Button>
                        </CardContent>
                    </Card>
                ) : order ? (
                    <div className="space-y-6">
                        <Card>
                            <CardContent className="p-6 grid gap-4 md:grid-cols-3">
                                <div>
                                    <p className="text-sm text-muted-foreground">Status</p>
                                    <Badge variant={order.status === "delivered" ? "default" : "secondary"}>{statusLabel}</Badge>
                                </div>
                                <div>
                                    <p className="text-sm text-muted-foreground">Total</p>
                                    <p className="font-semibold">{formatCurrency(order.total, order.currency)}</p>
                                </div>
                                <div>
                                    <p className="text-sm text-muted-foreground">Placed</p>
                                    <p className="font-medium">{new Date(order.createdAt).toLocaleDateString()}</p>
                                </div>
                            </CardContent>
                        </Card>

                        <Card>
                            <CardContent className="p-0">
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>Product</TableHead>
                                            <TableHead>Qty</TableHead>
                                            <TableHead>Unit Price</TableHead>
                                            <TableHead>Total</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {order.items.length === 0 ? (
                                            <TableRow>
                                                <TableCell colSpan={4} className="text-center py-6 text-muted-foreground">
                                                    No items in this order.
                                                </TableCell>
                                            </TableRow>
                                        ) : (
                                            order.items.map((item) => (
                                                <TableRow key={item.id}>
                                                    <TableCell className="font-medium">{item.productName}</TableCell>
                                                    <TableCell>{item.quantity}</TableCell>
                                                    <TableCell>{formatCurrency(item.price, order.currency)}</TableCell>
                                                    <TableCell>{formatCurrency(item.price * item.quantity, order.currency)}</TableCell>
                                                </TableRow>
                                            ))
                                        )}
                                    </TableBody>
                                </Table>
                            </CardContent>
                        </Card>
                    </div>
                ) : null}
            </Container>
        </section>
    )
}

export default function AccountOrderDetailPage() {
    return (
        <RequireAuth allowedRoles={["customer", "owner"]}>
            <AccountOrderDetailContent />
        </RequireAuth>
    )
}
