"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { RequireAuth } from "@/components/AuthProvider"
import { getOrders, type CustomerOrder } from "@/lib/accountApi"
import { Package, Search, ChevronLeft, ChevronRight } from "lucide-react"
import Link from "next/link"
import { formatCurrency } from "@/lib/shopApi"

function OrdersPageContent() {
  const [orders, setOrders] = useState<CustomerOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState("all")
  const [searchQuery, setSearchQuery] = useState("")
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    async function loadOrders() {
      try {
        setLoading(true)
        const response = await getOrders({
          page,
          limit: 10,
          status: statusFilter !== "all" ? statusFilter : undefined,
        })
        setOrders(response.orders)
        setTotalPages(response.totalPages)
      } catch (error) {
        console.error("Failed to load orders:", error)
        setOrders([])
      } finally {
        setLoading(false)
      }
    }

    loadOrders()
  }, [page, statusFilter])

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case "delivered": return "default"
      case "shipped": return "secondary"
      case "processing": return "outline"
      case "cancelled": return "destructive"
      default: return "outline"
    }
  }

  const filteredOrders = searchQuery && orders
    ? orders.filter((order) =>
      order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase())
    )
    : orders || []

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/account" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Account
          </Link>
        </div>

        <SectionHeading
          title="My Orders"
          subtitle="Track and manage your orders"
          className="mb-8"
        />

        {/* Filters */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="relative flex-1 sm:max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by order number..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="All Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Orders</SelectItem>
              <SelectItem value="pending">Pending</SelectItem>
              <SelectItem value="processing">Processing</SelectItem>
              <SelectItem value="shipped">Shipped</SelectItem>
              <SelectItem value="delivered">Delivered</SelectItem>
              <SelectItem value="cancelled">Cancelled</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Orders List */}
        {loading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <Card key={i}>
                <CardContent className="p-6">
                  <Skeleton className="h-24 w-full" />
                </CardContent>
              </Card>
            ))}
          </div>
        ) : filteredOrders.length > 0 ? (
          <>
            <div className="space-y-4">
              {filteredOrders.map((order) => (
                <Card key={order.id} className="transition-all hover:shadow-lg">
                  <CardContent className="p-6">
                    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                      <div className="space-y-2 flex-1">
                        <div className="flex items-center gap-3">
                          <h3 className="font-semibold">Order #{order.orderNumber}</h3>
                          <Badge variant={getStatusBadgeVariant(order.status)}>
                            {order.status.charAt(0).toUpperCase() + order.status.slice(1)}
                          </Badge>
                        </div>
                        <div className="text-sm text-muted-foreground space-y-1">
                          <p>Placed on {new Date(order.createdAt).toLocaleDateString()}</p>
                          <p>{order.items.length} {order.items.length === 1 ? "item" : "items"}</p>
                          {order.trackingNumber && (
                            <p className="font-mono">Tracking: {order.trackingNumber}</p>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <p className="text-sm text-muted-foreground">Total</p>
                          <p className="text-xl font-bold">
                            {formatCurrency(order.total, order.currency)}
                          </p>
                        </div>
                        <Button asChild variant="outline">
                          <Link href={`/account/orders/${order.id}`}>View</Link>
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="mt-8 flex justify-center gap-2">
                <Button
                  variant="outline"
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page === 1}
                >
                  <ChevronLeft className="h-4 w-4" />
                  Previous
                </Button>
                <div className="flex items-center px-4">
                  <span className="text-sm text-muted-foreground">
                    Page {page} of {totalPages}
                  </span>
                </div>
                <Button
                  variant="outline"
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                  disabled={page === totalPages}
                >
                  Next
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </>
        ) : (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-16 text-center">
              <Package className="mb-4 h-16 w-16 text-muted-foreground" />
              <h3 className="mb-2 text-lg font-semibold">No orders found</h3>
              <p className="mb-6 text-sm text-muted-foreground">
                {searchQuery
                  ? "Try a different search term"
                  : statusFilter !== "all"
                    ? "No orders with this status"
                    : "You haven't placed any orders yet"}
              </p>
              {!searchQuery && statusFilter === "all" && (
                <Button asChild>
                  <Link href="/shop">Start Shopping</Link>
                </Button>
              )}
            </CardContent>
          </Card>
        )}
      </Container>
    </section>
  )
}

export default function OrdersPage() {
  return (
    <RequireAuth allowedRoles={["customer", "owner"]}>
      <OrdersPageContent />
    </RequireAuth>
  )
}

