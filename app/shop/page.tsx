"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { useCart } from "@/components/CartContext"
import { 
  fetchProducts, 
  fetchCategories, 
  formatCurrency, 
  getAvailabilityBadge,
  type Product,
  type Category,
  type SearchParams 
} from "@/lib/shopApi"
import { Search, ShoppingCart, Filter, X } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { motion } from "framer-motion"

export default function ShopPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedCategory, setSelectedCategory] = useState<string>("all")
  const [selectedAvailability, setSelectedAvailability] = useState<string>("all")
  const [sortBy, setSortBy] = useState<SearchParams['sortBy']>("newest")
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [showMobileFilters, setShowMobileFilters] = useState(false)
  
  const { addItem } = useCart()

  // Load categories
  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch((err) => console.error('Failed to load categories:', err))
  }, [])

  // Load products
  useEffect(() => {
    setLoading(true)
    setError(null)

    const params: SearchParams = {
      page,
      limit: 12,
      sortBy,
    }

    if (searchQuery) params.query = searchQuery
    if (selectedCategory && selectedCategory !== "all") params.category = selectedCategory
    if (selectedAvailability && selectedAvailability !== "all") params.availability = selectedAvailability

    fetchProducts(params)
      .then((response) => {
        setProducts(response.products)
        setTotalPages(response.totalPages)
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message)
        setLoading(false)
      })
  }, [searchQuery, selectedCategory, selectedAvailability, sortBy, page])

  const handleSearch = (value: string) => {
    setSearchQuery(value)
    setPage(1)
  }

  const clearFilters = () => {
    setSearchQuery("")
    setSelectedCategory("all")
    setSelectedAvailability("all")
    setSortBy("newest")
    setPage(1)
  }

  const hasActiveFilters = searchQuery || selectedCategory !== "all" || selectedAvailability !== "all" || sortBy !== "newest"

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <SectionHeading
          title="Parts Shop"
          subtitle="Browse our selection of OEM and aftermarket automotive parts"
          centered
          className="mb-12"
        />

        <div className="flex gap-8">
          {/* Sidebar Filters - Desktop */}
          <aside className="hidden w-64 flex-shrink-0 lg:block">
            <Card>
              <CardContent className="p-6 space-y-6">
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="font-semibold">Filters</h3>
                    {hasActiveFilters && (
                      <Button variant="ghost" size="sm" onClick={clearFilters}>
                        Clear
                      </Button>
                    )}
                  </div>
                  <Separator />
                </div>

                {/* Category Filter */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Category</label>
                  <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                    <SelectTrigger>
                      <SelectValue placeholder="All Categories" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Categories</SelectItem>
                      {categories.map((cat) => (
                        <SelectItem key={cat.id} value={cat.slug}>
                          {cat.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Availability Filter */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Availability</label>
                  <Select value={selectedAvailability} onValueChange={setSelectedAvailability}>
                    <SelectTrigger>
                      <SelectValue placeholder="All" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="in_stock">In Stock</SelectItem>
                      <SelectItem value="low_stock">Low Stock</SelectItem>
                      <SelectItem value="pre_order">Pre-Order</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Price Range Placeholder */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Price Range</label>
                  <p className="text-xs text-muted-foreground">Coming soon</p>
                </div>
              </CardContent>
            </Card>
          </aside>

          {/* Main Content */}
          <div className="flex-1">
            {/* Search and Sort Bar */}
            <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="relative flex-1 sm:max-w-md">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder="Search parts..."
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="pl-10"
                />
              </div>

              <div className="flex gap-2">
                {/* Mobile Filters Button */}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowMobileFilters(!showMobileFilters)}
                  className="lg:hidden"
                >
                  <Filter className="mr-2 h-4 w-4" />
                  Filters
                </Button>

                <Select value={sortBy} onValueChange={(value) => setSortBy(value as SearchParams['sortBy'])}>
                  <SelectTrigger className="w-[180px]">
                    <SelectValue placeholder="Sort by" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="newest">Newest First</SelectItem>
                    <SelectItem value="price_asc">Price: Low to High</SelectItem>
                    <SelectItem value="price_desc">Price: High to Low</SelectItem>
                    <SelectItem value="name_asc">Name: A to Z</SelectItem>
                    <SelectItem value="name_desc">Name: Z to A</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Mobile Filters */}
            {showMobileFilters && (
              <Card className="mb-6 lg:hidden">
                <CardContent className="p-4 space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="font-semibold">Filters</h3>
                    <Button variant="ghost" size="sm" onClick={() => setShowMobileFilters(false)}>
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                  
                  <div>
                    <label className="mb-2 block text-sm font-medium">Category</label>
                    <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                      <SelectTrigger>
                        <SelectValue placeholder="All Categories" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Categories</SelectItem>
                        {categories.map((cat) => (
                          <SelectItem key={cat.id} value={cat.slug}>
                            {cat.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div>
                    <label className="mb-2 block text-sm font-medium">Availability</label>
                    <Select value={selectedAvailability} onValueChange={setSelectedAvailability}>
                      <SelectTrigger>
                        <SelectValue placeholder="All" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All</SelectItem>
                        <SelectItem value="in_stock">In Stock</SelectItem>
                        <SelectItem value="low_stock">Low Stock</SelectItem>
                        <SelectItem value="pre_order">Pre-Order</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {hasActiveFilters && (
                    <Button variant="outline" size="sm" onClick={clearFilters} className="w-full">
                      Clear Filters
                    </Button>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Error State */}
            {error && (
              <Card className="border-destructive">
                <CardContent className="p-8 text-center">
                  <p className="mb-4 text-destructive">{error}</p>
                  <p className="text-sm text-muted-foreground">
                    Unable to load products. Please check your connection or try again later.
                  </p>
                </CardContent>
              </Card>
            )}

            {/* Loading State */}
            {loading && (
              <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                {Array.from({ length: 6 }).map((_, i) => (
                  <Card key={i}>
                    <CardContent className="p-0">
                      <Skeleton className="aspect-square w-full" />
                      <div className="p-4 space-y-2">
                        <Skeleton className="h-4 w-3/4" />
                        <Skeleton className="h-4 w-1/2" />
                        <Skeleton className="h-8 w-full" />
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}

            {/* Empty State */}
            {!loading && !error && products.length === 0 && (
              <Card>
                <CardContent className="p-12 text-center">
                  <ShoppingCart className="mx-auto mb-4 h-16 w-16 text-muted-foreground" />
                  <h3 className="mb-2 text-lg font-semibold">No products found</h3>
                  <p className="mb-4 text-sm text-muted-foreground">
                    {hasActiveFilters
                      ? "Try adjusting your filters or search query"
                      : process.env.NEXT_PUBLIC_SHOP_API_BASE_URL
                        ? "No products available at the moment"
                        : "Shop API not configured. Set NEXT_PUBLIC_SHOP_API_BASE_URL in .env.local"}
                  </p>
                  {hasActiveFilters && (
                    <Button onClick={clearFilters}>Clear Filters</Button>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Products Grid */}
            {!loading && !error && products.length > 0 && (
              <>
                <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
                  {products.map((product, i) => {
                    const availabilityBadge = getAvailabilityBadge(product.availability)
                    
                    return (
                      <motion.div
                        key={product.id}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.05 }}
                      >
                        <Card className="group h-full overflow-hidden transition-all hover:shadow-lg">
                          <CardContent className="p-0">
                            <Link href={`/shop/${product.slug}`} className="block">
                              <div className="relative aspect-square overflow-hidden bg-muted">
                                {product.images && product.images[0] ? (
                                  <Image
                                    src={product.images[0]}
                                    alt={product.name}
                                    fill
                                    className="object-cover transition-transform group-hover:scale-105"
                                  />
                                ) : (
                                  <div className="flex h-full items-center justify-center">
                                    <ShoppingCart className="h-16 w-16 text-muted-foreground" />
                                  </div>
                                )}
                                <Badge 
                                  variant={availabilityBadge.variant}
                                  className="absolute right-2 top-2"
                                >
                                  {availabilityBadge.label}
                                </Badge>
                              </div>
                            </Link>

                            <div className="p-4 space-y-3">
                              <Link href={`/shop/${product.slug}`}>
                                <h3 className="font-semibold line-clamp-2 hover:text-primary">
                                  {product.name}
                                </h3>
                              </Link>
                              
                              {product.partNumber && (
                                <p className="text-xs text-muted-foreground">
                                  Part #: {product.partNumber}
                                </p>
                              )}

                              <div className="flex items-center justify-between">
                                <p className="text-lg font-bold">
                                  {formatCurrency(product.price, product.currency)}
                                </p>
                                {product.leadTime && (
                                  <p className="text-xs text-muted-foreground">
                                    {product.leadTime}
                                  </p>
                                )}
                              </div>

                              <div className="flex gap-2">
                                <Button asChild variant="outline" className="flex-1">
                                  <Link href={`/shop/${product.slug}`}>View</Link>
                                </Button>
                                <Button
                                  onClick={() => addItem(product)}
                                  disabled={product.availability === 'out_of_stock'}
                                  className="flex-1"
                                >
                                  <ShoppingCart className="mr-2 h-4 w-4" />
                                  Add
                                </Button>
                              </div>
                            </div>
                          </CardContent>
                        </Card>
                      </motion.div>
                    )
                  })}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="mt-8 flex justify-center gap-2">
                    <Button
                      variant="outline"
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      disabled={page === 1}
                    >
                      Previous
                    </Button>
                    <div className="flex items-center gap-2 px-4">
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
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </Container>
    </section>
  )
}

