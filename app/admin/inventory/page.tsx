"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { RequireAuth } from "@/components/AuthProvider"
import { getAdminProducts, updateProductStock, type AdminProduct } from "@/lib/adminApi"
import { ChevronLeft } from "lucide-react"
import Link from "next/link"

function AdminInventoryContent() {
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadProducts() {
      try {
        const response = await getAdminProducts({ limit: 100 })
        setProducts(response.products)
      } catch (error) {
        console.error("Failed to load products:", error)
        setProducts([])
      } finally {
        setLoading(false)
      }
    }
    loadProducts()
  }, [])

  async function handleStockUpdate(id: string, newStock: number) {
    try {
      await updateProductStock(id, newStock)
      setProducts((prev) =>
        prev.map((p) => (p.id === id ? { ...p, stockLevel: newStock } : p))
      )
    } catch (error) {
      console.error("Failed to update stock:", error)
    }
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/admin" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Dashboard
          </Link>
        </div>

        <SectionHeading title="Inventory" subtitle="Manage stock levels" className="mb-8" />

        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product</TableHead>
                  <TableHead>SKU</TableHead>
                  <TableHead>Current Stock</TableHead>
                  <TableHead>Update Stock</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8">Loading...</TableCell>
                  </TableRow>
                ) : products.length > 0 ? (
                  products.map((product) => (
                    <TableRow key={product.id}>
                      <TableCell className="font-medium">{product.name}</TableCell>
                      <TableCell className="font-mono text-sm">{product.sku}</TableCell>
                      <TableCell>
                        <Badge variant={product.stockLevel < 10 ? "destructive" : "secondary"}>
                          {product.stockLevel}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Input
                            type="number"
                            min="0"
                            defaultValue={product.stockLevel}
                            className="w-24"
                            onBlur={(e) => {
                              const newValue = parseInt(e.target.value)
                              if (!isNaN(newValue) && newValue !== product.stockLevel) {
                                handleStockUpdate(product.id, newValue)
                              }
                            }}
                          />
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8 text-muted-foreground">No products found</TableCell>
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

export default function AdminInventoryPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <AdminInventoryContent />
    </RequireAuth>
  )
}

