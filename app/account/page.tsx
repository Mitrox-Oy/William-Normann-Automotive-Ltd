"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { RequireAuth, useAuth } from "@/components/AuthProvider"
import { getOrders, type CustomerOrder } from "@/lib/accountApi"
import { User, Package, Heart, MapPin, Settings, ChevronRight } from "lucide-react"
import Link from "next/link"
import { formatCurrency } from "@/lib/shopApi"

function AccountDashboardContent() {
  const { user } = useAuth()
  const [recentOrders, setRecentOrders] = useState<CustomerOrder[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadRecentOrders() {
      try {
        const response = await getOrders({ limit: 3 })
        setRecentOrders(Array.isArray(response?.orders) ? response.orders : [])
      } catch (error) {
        console.error("Failed to load recent orders:", error)
        setRecentOrders([])
      } finally {
        setLoading(false)
      }
    }

    loadRecentOrders()
  }, [])

  const getUserInitials = () => {
    if (!user || !user.name) return "U"
    return user.name
      .split(" ")
      .map((n) => n[0])
      .filter(Boolean)
      .join("")
      .toUpperCase()
      .slice(0, 2) || "U"
  }

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case "delivered":
        return "default"
      case "shipped":
        return "secondary"
      case "processing":
        return "outline"
      case "cancelled":
        return "destructive"
      default:
        return "outline"
    }
  }

  const quickLinks = [
    { href: "/account/orders", icon: Package, label: "Orders", description: "View order history" },
    { href: "/account/wishlist", icon: Heart, label: "Wishlist", description: "Saved items" },
    { href: "/account/addresses", icon: MapPin, label: "Addresses", description: "Manage addresses" },
    { href: "/account/settings", icon: Settings, label: "Settings", description: "Account settings" },
  ]

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <SectionHeading
          title="My Account"
          subtitle="Manage your profile, orders, and preferences"
          className="mb-12"
        />

        <div className="grid gap-8 lg:grid-cols-3">
          {/* Profile Card */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <CardTitle>Profile</CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="flex flex-col items-center text-center">
                  <Avatar className="h-24 w-24 mb-4">
                    <AvatarImage src={user?.avatar} alt={user?.name} />
                    <AvatarFallback className="text-2xl">{getUserInitials()}</AvatarFallback>
                  </Avatar>
                  <h3 className="text-xl font-bold">{user?.name}</h3>
                  <p className="text-sm text-muted-foreground">{user?.email}</p>
                  {user?.phone && (
                    <p className="text-sm text-muted-foreground mt-1">{user?.phone}</p>
                  )}
                  <Badge variant="secondary" className="mt-3">
                    Customer Account
                  </Badge>
                </div>

                <Button asChild variant="outline" className="w-full">
                  <Link href="/account/settings">
                    <Settings className="mr-2 h-4 w-4" />
                    Edit Profile
                  </Link>
                </Button>
              </CardContent>
            </Card>
          </div>

          {/* Main Content */}
          <div className="lg:col-span-2 space-y-8">
            {/* Quick Links */}
            <div>
              <h3 className="mb-4 text-lg font-semibold">Quick Actions</h3>
              <div className="grid gap-4 sm:grid-cols-2">
                {quickLinks.map((link) => (
                  <Link key={link.href} href={link.href}>
                    <Card className="transition-all hover:shadow-lg hover:border-primary">
                      <CardContent className="flex items-center gap-4 p-6">
                        <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                          <link.icon className="h-6 w-6 text-primary" />
                        </div>
                        <div className="flex-1">
                          <h4 className="font-semibold">{link.label}</h4>
                          <p className="text-sm text-muted-foreground">{link.description}</p>
                        </div>
                        <ChevronRight className="h-5 w-5 text-muted-foreground" />
                      </CardContent>
                    </Card>
                  </Link>
                ))}
              </div>
            </div>

            {/* Recent Orders */}
            <div>
              <div className="mb-4 flex items-center justify-between">
                <h3 className="text-lg font-semibold">Recent Orders</h3>
                <Button asChild variant="ghost" size="sm">
                  <Link href="/account/orders">
                    View all
                    <ChevronRight className="ml-1 h-4 w-4" />
                  </Link>
                </Button>
              </div>

              <Card>
                <CardContent className="p-0">
                  {loading ? (
                    <div className="divide-y">
                      {[1, 2, 3].map((i) => (
                        <div key={i} className="p-6">
                          <Skeleton className="h-20 w-full" />
                        </div>
                      ))}
                    </div>
                  ) : recentOrders.length > 0 ? (
                    <div className="divide-y">
                      {recentOrders.map((order) => order && order.id ? (
                        <Link
                          key={order.id}
                          href={`/account/orders/${order.id}`}
                          className="block p-6 transition-colors hover:bg-muted/50"
                        >
                          <div className="flex items-start justify-between">
                            <div className="space-y-1">
                              <p className="font-medium">Order #{order.orderNumber || order.id}</p>
                              <p className="text-sm text-muted-foreground">
                                {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : 'N/A'}
                              </p>
                              <p className="text-sm">
                                {order.items?.length || 0} {(order.items?.length || 0) === 1 ? "item" : "items"}
                              </p>
                            </div>
                            <div className="text-right space-y-2">
                              <p className="font-bold">
                                {formatCurrency(order.total || 0, order.currency || 'USD')}
                              </p>
                              <Badge variant={getStatusBadgeVariant(order.status || 'pending')}>
                                {(order.status || 'pending').charAt(0).toUpperCase() + (order.status || 'pending').slice(1)}
                              </Badge>
                            </div>
                          </div>
                        </Link>
                      ) : null)}
                    </div>
                  ) : (
                    <div className="py-12 text-center">
                      <Package className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
                      <p className="text-sm text-muted-foreground">No orders yet</p>
                      <Button asChild size="sm" className="mt-4">
                        <Link href="/shop">Start Shopping</Link>
                      </Button>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </Container>
    </section>
  )
}

export default function AccountPage() {
  return (
    <RequireAuth allowedRoles={["customer", "owner"]}>
      <AccountDashboardContent />
    </RequireAuth>
  )
}

