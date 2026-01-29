"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { RequireAuth } from "@/components/AuthProvider"
import { getAdminCustomers, type AdminCustomer } from "@/lib/adminApi"
import { formatCurrency } from "@/lib/shopApi"
import { ChevronLeft } from "lucide-react"
import Link from "next/link"

function AdminCustomersContent() {
  const [customers, setCustomers] = useState<AdminCustomer[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadCustomers() {
      try {
        const response = await getAdminCustomers({ limit: 50 })
        setCustomers(response.customers)
      } catch (error) {
        console.error("Failed to load customers:", error)
        setCustomers([])
      } finally {
        setLoading(false)
      }
    }
    loadCustomers()
  }, [])

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/admin" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Dashboard
          </Link>
        </div>

        <SectionHeading title="Customers" subtitle="Manage your customers" className="mb-8" />

        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Orders</TableHead>
                  <TableHead>Total Spent</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Joined</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8">Loading...</TableCell>
                  </TableRow>
                ) : customers.length > 0 ? (
                  customers.map((customer) => (
                    <TableRow key={customer.id}>
                      <TableCell className="font-medium">{customer.name}</TableCell>
                      <TableCell>{customer.email}</TableCell>
                      <TableCell>{customer.totalOrders}</TableCell>
                      <TableCell>{formatCurrency(customer.totalSpent, "GBP")}</TableCell>
                      <TableCell>
                        <Badge variant={customer.status === "active" ? "default" : "destructive"}>
                          {customer.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{new Date(customer.createdAt).toLocaleDateString()}</TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">No customers found</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </Container>
    </section>
  )
}

export default function AdminCustomersPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <AdminCustomersContent />
    </RequireAuth>
  )
}

