"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { RequireAuth } from "@/components/AuthProvider"
import { getCategoriesByTopic } from "@/lib/adminApi"
import { SHOP_TOPICS, TOPIC_INFO, type ShopTopic } from "@/lib/shopApi"
import type { Category } from "@/lib/shopApi"
import { ChevronLeft, Car, Settings, Wrench, Sparkles } from "lucide-react"
import Link from "next/link"

// Topic icons mapping
const TOPIC_ICONS: Record<ShopTopic, React.ReactNode> = {
  cars: <Car className="h-5 w-5" />,
  parts: <Settings className="h-5 w-5" />,
  tools: <Wrench className="h-5 w-5" />,
  custom: <Sparkles className="h-5 w-5" />,
}

function AdminCategoriesContent() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedTopic, setSelectedTopic] = useState<ShopTopic>("parts")

  useEffect(() => {
    async function loadCategories() {
      try {
        setLoading(true)
        const data = await getCategoriesByTopic(selectedTopic)
        // Exclude the root category itself
        const subcategories = data.filter(c => c.slug !== selectedTopic)
        setCategories(subcategories)
      } catch (error) {
        console.error("Failed to load categories:", error)
        setCategories([])
      } finally {
        setLoading(false)
      }
    }
    loadCategories()
  }, [selectedTopic])

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/admin" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Dashboard
          </Link>
        </div>

        <div className="mb-8 flex items-center justify-between">
          <SectionHeading 
            title={`Categories — ${TOPIC_INFO[selectedTopic].label}`} 
            subtitle={`View ${TOPIC_INFO[selectedTopic].label.toLowerCase()} categories`} 
          />
          <Button asChild>
            <Link href={`/admin/products?tab=categories`}>
              Manage Categories
            </Link>
          </Button>
        </div>

        {/* Topic Selector */}
        <div className="mb-6">
          <div className="flex flex-wrap gap-2">
            {SHOP_TOPICS.map((topic) => (
              <Button
                key={topic}
                variant={selectedTopic === topic ? "default" : "outline"}
                onClick={() => setSelectedTopic(topic)}
                className="flex items-center gap-2"
              >
                {TOPIC_ICONS[topic]}
                {TOPIC_INFO[topic].label}
              </Button>
            ))}
          </div>
        </div>

        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Slug</TableHead>
                  <TableHead>Products</TableHead>
                  <TableHead>Parent</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8">Loading...</TableCell>
                  </TableRow>
                ) : categories.length > 0 ? (
                  categories.map((category) => (
                    <TableRow key={category.id}>
                      <TableCell className="font-medium">{category.name}</TableCell>
                      <TableCell className="font-mono text-sm">{category.slug}</TableCell>
                      <TableCell>{category.productCount || 0}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">
                          {category.parentId ? `Parent ID: ${category.parentId}` : TOPIC_INFO[selectedTopic].label}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8 text-muted-foreground">
                      No categories found for {TOPIC_INFO[selectedTopic].label}
                    </TableCell>
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

export default function AdminCategoriesPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <AdminCategoriesContent />
    </RequireAuth>
  )
}

