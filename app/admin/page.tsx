"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
import { RequireAuth } from "@/components/AuthProvider"
import { getDashboardStats, type DashboardStats } from "@/lib/adminApi"
import { formatCurrency } from "@/lib/shopApi"
import { DollarSign, ShoppingBag, Users, Package, TrendingUp, AlertTriangle } from "lucide-react"
import Link from "next/link"

function AdminDashboardContent() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadStats() {
      try {
        const data = await getDashboardStats()
        setStats(data)
      } catch (error) {
        console.error("Failed to load dashboard stats:", error)
        // Set default empty stats on error
        setStats({
          totalRevenue: 0,
          totalOrders: 0,
          totalCustomers: 0,
          totalProducts: 0,
          recentOrders: [],
          lowStockProducts: [],
          revenueByMonth: [],
        })
      } finally {
        setLoading(false)
      }
    }

    loadStats()
  }, [])

  const statCards = stats ? [
    { title: "Total Revenue", value: formatCurrency(stats.totalRevenue, "GBP"), icon: DollarSign, color: "text-green-500" },
    { title: "Total Orders", value: stats.totalOrders.toString(), icon: ShoppingBag, color: "text-blue-500" },
    { title: "Customers", value: stats.totalCustomers.toString(), icon: Users, color: "text-purple-500" },
    { title: "Products", value: stats.totalProducts.toString(), icon: Package, color: "text-orange-500" },
  ] : []

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <SectionHeading
          title="Admin Dashboard"
          subtitle="Overview of your store performance"
          className="mb-8"
        />

        {/* Stats Cards */}
        <div className="mb-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {loading ? (
            Array.from({ length: 4 }).map((_, i) => (
              <Card key={i}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-4" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-8 w-24" />
                </CardContent>
              </Card>
            ))
          ) : (
            statCards.map((stat) => (
              <Card key={stat.title}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
                  <stat.icon className={`h-4 w-4 ${stat.color}`} />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stat.value}</div>
                </CardContent>
              </Card>
            ))
          )}
        </div>

        <div className="grid gap-8 lg:grid-cols-2">
          {/* Recent Orders */}
          <Card>
            <CardHeader>
              <CardTitle>Recent Orders</CardTitle>
              <CardDescription>Latest customer orders</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="space-y-4">
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-16 w-full" />
                  ))}
                </div>
              ) : stats?.recentOrders && stats.recentOrders.length > 0 ? (
                <div className="space-y-4">
                  {stats.recentOrders.map((order) => (
                    <Link
                      key={order.id}
                      href={`/admin/orders/${order.id}`}
                      className="flex items-center justify-between rounded-lg border p-4 transition-colors hover:bg-muted/50"
                    >
                      <div>
                        <p className="font-medium">#{order.orderNumber}</p>
                        <p className="text-sm text-muted-foreground">{order.customer.name}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold">{formatCurrency(order.total, order.currency)}</p>
                        <Badge variant="outline" className="text-xs">
                          {order.status}
                        </Badge>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <p className="text-center text-sm text-muted-foreground py-8">No recent orders</p>
              )}
            </CardContent>
          </Card>

          {/* Low Stock Alerts */}
          <Card>
            <CardHeader>
              <CardTitle>Low Stock Alerts</CardTitle>
              <CardDescription>Products running low on inventory</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="space-y-4">
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-16 w-full" />
                  ))}
                </div>
              ) : stats?.lowStockProducts && stats.lowStockProducts.length > 0 ? (
                <div className="space-y-4">
                  {stats.lowStockProducts.map((product) => (
                    <Link
                      key={product.id}
                      href={`/admin/products/${product.id}`}
                      className="flex items-center justify-between rounded-lg border p-4 transition-colors hover:bg-muted/50"
                    >
                      <div className="flex items-center gap-3">
                        <AlertTriangle className="h-5 w-5 text-orange-500" />
                        <div>
                          <p className="font-medium line-clamp-1">{product.name}</p>
                          <p className="text-sm text-muted-foreground">SKU: {product.sku}</p>
                        </div>
                      </div>
                      <Badge variant="destructive">{product.stockLevel} left</Badge>
                    </Link>
                  ))}
                </div>
              ) : (
                <p className="text-center text-sm text-muted-foreground py-8">All products well stocked</p>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Quick Links */}
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Link href="/admin/products">
            <Card className="transition-all hover:shadow-lg hover:border-primary">
              <CardContent className="flex items-center gap-4 p-6">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Package className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <h4 className="font-semibold">Products</h4>
                  <p className="text-sm text-muted-foreground">Manage inventory</p>
                </div>
              </CardContent>
            </Card>
          </Link>

          <Link href="/admin/orders">
            <Card className="transition-all hover:shadow-lg hover:border-primary">
              <CardContent className="flex items-center gap-4 p-6">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <ShoppingBag className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <h4 className="font-semibold">Orders</h4>
                  <p className="text-sm text-muted-foreground">Process orders</p>
                </div>
              </CardContent>
            </Card>
          </Link>

          <Link href="/admin/customers">
            <Card className="transition-all hover:shadow-lg hover:border-primary">
              <CardContent className="flex items-center gap-4 p-6">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Users className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <h4 className="font-semibold">Customers</h4>
                  <p className="text-sm text-muted-foreground">Manage users</p>
                </div>
              </CardContent>
            </Card>
          </Link>

          <Link href="/admin/inventory">
            <Card className="transition-all hover:shadow-lg hover:border-primary">
              <CardContent className="flex items-center gap-4 p-6">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <TrendingUp className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <h4 className="font-semibold">Inventory</h4>
                  <p className="text-sm text-muted-foreground">Stock levels</p>
                </div>
              </CardContent>
            </Card>
          </Link>
        </div>
      </Container>
    </section>
  )
}

export default function AdminDashboardPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <AdminDashboardContent />
    </RequireAuth>
  )
}

