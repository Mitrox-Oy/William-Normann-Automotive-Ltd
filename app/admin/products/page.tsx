"use client"

import { useState, useEffect, useRef, type ChangeEvent } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { RequireAuth } from "@/components/AuthProvider"
import { getAdminProducts, deleteProduct, getAllCategories, getAdminCategories, getCategoryBySlug, createCategory, updateCategory, deleteCategory, importProductsCsv, uploadCategoryImage, type AdminProduct, type AdminCategory, type ProductCsvImportResult } from "@/lib/adminApi"
import { formatCurrency, SHOP_TOPICS, TOPIC_INFO, type ShopTopic } from "@/lib/shopApi"
import { Plus, Search, Edit, Trash2, ChevronLeft, ChevronRight, Package, FolderTree, Car, Settings, Sparkles, Upload } from "lucide-react"
import Link from "next/link"
import Image from "next/image"
import { getCategoryImageUrl } from "@/lib/utils"


// Topic icons mapping
const TOPIC_ICONS: Record<ShopTopic, React.ReactNode> = {
  cars: <Car className="h-5 w-5" />,
  parts: <Settings className="h-5 w-5" />,
  custom: <Sparkles className="h-5 w-5" />,
}

function AdminProductsContent() {
  const [activeTab, setActiveTab] = useState("products")

  // Topic state - default to 'cars'
  const [selectedTopic, setSelectedTopic] = useState<ShopTopic>("cars")
  const [topicRootCategoryId, setTopicRootCategoryId] = useState<number | null>(null)

  // Products state
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [productsLoading, setProductsLoading] = useState(true)
  const [csvImportLoading, setCsvImportLoading] = useState(false)
  const [csvImportSummary, setCsvImportSummary] = useState<ProductCsvImportResult | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState("active")
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const csvFileInputRef = useRef<HTMLInputElement | null>(null)

  // Categories state
  const [categories, setCategories] = useState<AdminCategory[]>([])
  const [allTopicCategories, setAllTopicCategories] = useState<AdminCategory[]>([])  // For parent dropdown
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [categoryDialogOpen, setCategoryDialogOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<AdminCategory | null>(null)
  const [categorySlugTouched, setCategorySlugTouched] = useState(false)
  const [categoryImageUploading, setCategoryImageUploading] = useState(false)
  const categoryImageInputRef = useRef<HTMLInputElement | null>(null)
  const [categoryForm, setCategoryForm] = useState({
    name: "",
    slug: "",
    description: "",
    imageUrl: "",
    status: "active" as "active" | "inactive",
    parentId: "",
  })

  // Load topic root category ID when topic changes
  useEffect(() => {
    async function loadTopicRoot() {
      const rootCat = await getCategoryBySlug(selectedTopic)
      if (rootCat) {
        setTopicRootCategoryId(rootCat.id)
      }
    }
    loadTopicRoot()
  }, [selectedTopic])

  // Reload data when topic or tab changes
  useEffect(() => {
    if (activeTab === "products" && topicRootCategoryId) {
      loadProducts()
    } else if (activeTab === "categories") {
      loadCategories()
    }
  }, [activeTab, page, statusFilter, topicRootCategoryId, selectedTopic])

  // Products functions
  async function loadProducts() {
    if (!topicRootCategoryId) return

    try {
      setProductsLoading(true)
      const response = await getAdminProducts({
        page,
        limit: 20,
        status: statusFilter as any,
        search: searchQuery || undefined,
        rootCategoryId: topicRootCategoryId,  // Filter by topic
      })
      setProducts(response?.products || [])
      setTotalPages(response?.totalPages || 1)
    } catch (error) {
      console.error("Failed to load products:", error)
      setProducts([])
    } finally {
      setProductsLoading(false)
    }
  }

  async function handleDeleteProduct(id: string, name: string) {
    if (!confirm(`Delete product "${name}"?`)) return

    try {
      await deleteProduct(id)
      await loadProducts()
    } catch (error: any) {
      console.error("Failed to delete product:", error)
      alert(error?.message || "Failed to delete product")
    }
  }

  async function handleCsvImport(file: File, dryRun: boolean) {
    try {
      setCsvImportLoading(true)
      const summary = await importProductsCsv(file, dryRun)
      setCsvImportSummary(summary)

      if (!dryRun && summary.successCount > 0) {
        await loadProducts()
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : "CSV import failed"
      alert(message)
    } finally {
      setCsvImportLoading(false)
    }
  }

  function openCsvFileDialog() {
    csvFileInputRef.current?.click()
  }

  async function onCsvFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return

    const importNow = window.confirm("Click OK to IMPORT valid rows now. Click Cancel to run DRY-RUN validation only.")
    await handleCsvImport(file, !importNow)

    event.target.value = ""
  }

  // Categories functions - now topic-aware
  async function loadCategories() {
    try {
      setCategoriesLoading(true)
      const all = await getAdminCategories()
      if (!topicRootCategoryId) {
        setCategories([])
        setAllTopicCategories([])
        return
      }

      const categoryMap = new Map<number, AdminCategory>()
      all.forEach((c) => categoryMap.set(c.id, c))

      function isDescendantOf(category: AdminCategory, rootId: number): boolean {
        if (category.id === rootId) return true
        if (!category.parentId) return false
        const parentId = typeof category.parentId === "number" ? category.parentId : Number.parseInt(String(category.parentId), 10)
        const parent = categoryMap.get(parentId)
        if (!parent) return false
        return isDescendantOf(parent, rootId)
      }

      const inTopic = all.filter((c) => isDescendantOf(c, topicRootCategoryId))
      const subcategories = inTopic.filter((c) => c.id !== topicRootCategoryId)
      setCategories(subcategories)
      setAllTopicCategories(inTopic)
    } catch (error) {
      console.error("Failed to load categories:", error)
      setCategories([])
      setAllTopicCategories([])
    } finally {
      setCategoriesLoading(false)
    }
  }

  function openCategoryDialog(category?: AdminCategory) {
    if (category) {
      setEditingCategory(category)
      setCategorySlugTouched(true)
      setCategoryForm({
        name: category.name,
        slug: category.slug,
        description: category.description || "",
        imageUrl: category.imageUrl || "",
        status: category.status || "active",
        parentId: category.parentId?.toString() || "",
      })
    } else {
      setEditingCategory(null)
      setCategorySlugTouched(false)
      // Default parent to topic root when creating new category
      setCategoryForm({
        name: "",
        slug: "",
        description: "",
        imageUrl: "",
        status: "active",
        parentId: topicRootCategoryId?.toString() || "",
      })
    }
    setCategoryDialogOpen(true)
  }

  async function handleCategoryImageSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return

    try {
      setCategoryImageUploading(true)
      const uploaded = await uploadCategoryImage(file)
      setCategoryForm((prev) => ({
        ...prev,
        imageUrl: uploaded.imageUrl,
      }))
    } catch (error) {
      console.error("Failed to upload category image:", error)
      alert(error instanceof Error ? error.message : "Failed to upload category image")
    } finally {
      setCategoryImageUploading(false)
      event.target.value = ""
    }
  }

  async function handleSaveCategory() {
    const computedSlug = editingCategory
      ? categoryForm.slug
      : composeCategorySlug(categoryForm.slug || categoryForm.name, categoryForm.parentId)

    if (!categoryForm.name || !computedSlug) {
      alert("Name and slug are required")
      return
    }

    const payload = {
      ...categoryForm,
      slug: computedSlug,
    }

    try {
      if (editingCategory) {
        await updateCategory(editingCategory.id.toString(), payload)
      } else {
        await createCategory(payload)
      }
      setCategoryDialogOpen(false)
      await loadCategories()
    } catch (error) {
      console.error("Failed to save category:", error)
      alert("Failed to save category")
    }
  }

  async function handleDeleteCategory(id: string, name: string) {
    if (!confirm(`Delete category "${name}"? This will also delete all subcategories.`)) return

    try {
      await deleteCategory(id)
      await loadCategories()
    } catch (error) {
      console.error("Failed to delete category:", error)
      alert("Failed to delete category")
    }
  }

  // Generate slug from name
  function generateSlug(name: string) {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/(^-|-$)/g, "")
  }

  function composeCategorySlug(value: string, parentIdRaw?: string) {
    const childSlug = generateSlug(value)
    const parentId = Number.parseInt(String(parentIdRaw || topicRootCategoryId || ""), 10)
    const parentSlug = Number.isFinite(parentId)
      ? allTopicCategories.find((category) => category.id === parentId)?.slug
      : ""

    const normalizedParentSlug = generateSlug(parentSlug || "")

    if (!normalizedParentSlug) {
      return childSlug
    }

    if (!childSlug) {
      return normalizedParentSlug
    }

    if (childSlug === normalizedParentSlug || childSlug.startsWith(`${normalizedParentSlug}-`)) {
      return childSlug
    }

    return `${normalizedParentSlug}-${childSlug}`
  }

  const filteredProducts = products && Array.isArray(products)
    ? (searchQuery
      ? products.filter((p) =>
        p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.sku.toLowerCase().includes(searchQuery.toLowerCase())
      )
      : products)
    : []

  const parentCategoryOptions = (() => {
    if (!topicRootCategoryId || allTopicCategories.length === 0) return [] as Array<{ id: number; label: string }>

    const categoryMap = new Map<number, AdminCategory>()
    const childrenByParent = new Map<number, AdminCategory[]>()

    allTopicCategories.forEach((category) => {
      categoryMap.set(category.id, category)
      const parentId = typeof category.parentId === "number"
        ? category.parentId
        : Number.parseInt(String(category.parentId), 10)
      if (!Number.isFinite(parentId)) return
      const children = childrenByParent.get(parentId) || []
      children.push(category)
      childrenByParent.set(parentId, children)
    })

    const blockedIds = new Set<number>()
    if (editingCategory) {
      blockedIds.add(editingCategory.id)

      const collectDescendants = (parentId: number) => {
        const children = childrenByParent.get(parentId) || []
        children.forEach((child) => {
          if (blockedIds.has(child.id)) return
          blockedIds.add(child.id)
          collectDescendants(child.id)
        })
      }

      collectDescendants(editingCategory.id)
    }

    const sortByName = (left: AdminCategory, right: AdminCategory) =>
      left.name.localeCompare(right.name, undefined, { sensitivity: "base" })

    const flattened: Array<{ id: number; label: string }> = []

    const walk = (parentId: number, depth: number) => {
      const children = [...(childrenByParent.get(parentId) || [])].sort(sortByName)
      children.forEach((child) => {
        if (blockedIds.has(child.id)) return
        const prefix = depth === 1 ? "└ " : `${"— ".repeat(depth - 1)}└ `
        flattened.push({ id: child.id, label: `${prefix}${child.name}` })
        walk(child.id, depth + 1)
      })
    }

    walk(topicRootCategoryId, 1)
    return flattened
  })()

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
            title={`Products & Categories — ${TOPIC_INFO[selectedTopic].label}`}
            subtitle={`Manage ${TOPIC_INFO[selectedTopic].label.toLowerCase()} catalog and categories`}
          />
          {activeTab === "products" && (
            <Button asChild>
              <Link href={`/admin/products/new?topic=${selectedTopic}`}>
                <Plus className="mr-2 h-4 w-4" />
                Add Product
              </Link>
            </Button>
          )}
          {activeTab === "categories" && (
            <Button onClick={() => openCategoryDialog()}>
              <Plus className="mr-2 h-4 w-4" />
              Add Category
            </Button>
          )}
        </div>

        {/* Topic Selector */}
        <div className="mb-6">
          <div className="flex flex-wrap gap-2">
            {SHOP_TOPICS.map((topic) => (
              <Button
                key={topic}
                variant={selectedTopic === topic ? "default" : "outline"}
                onClick={() => {
                  setSelectedTopic(topic)
                  setPage(1)  // Reset pagination when switching topics
                }}
                className="flex items-center gap-2"
              >
                {TOPIC_ICONS[topic]}
                {TOPIC_INFO[topic].label}
              </Button>
            ))}
          </div>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList>
            <TabsTrigger value="products">
              <Package className="mr-2 h-4 w-4" />
              Products
            </TabsTrigger>
            <TabsTrigger value="categories">
              <FolderTree className="mr-2 h-4 w-4" />
              Categories
            </TabsTrigger>
          </TabsList>

          {/* Products Tab */}
          <TabsContent value="products" className="space-y-6">
            {/* Filters */}
            <div className="flex flex-col gap-4 sm:flex-row">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search products or SKU..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && loadProducts()}
                  className="pl-10"
                />
              </div>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="active">Active</SelectItem>
                  <SelectItem value="archived">Archived</SelectItem>
                </SelectContent>
              </Select>
              <Button onClick={loadProducts}>Search</Button>
              <Button variant="outline" onClick={openCsvFileDialog} disabled={csvImportLoading}>
                <Upload className="mr-2 h-4 w-4" />
                {csvImportLoading ? "Importing..." : "Import CSV"}
              </Button>
              <input
                ref={csvFileInputRef}
                type="file"
                accept=".csv,text/csv"
                className="hidden"
                onChange={onCsvFileSelected}
              />
            </div>

            {csvImportSummary && (
              <Card>
                <CardContent className="pt-6">
                  <div className="flex flex-wrap items-center gap-3 text-sm">
                    <Badge variant={csvImportSummary.dryRun ? "secondary" : "default"}>
                      {csvImportSummary.dryRun ? "Dry Run" : "Imported"}
                    </Badge>
                    <span>Total: {csvImportSummary.totalRows}</span>
                    <span>Success: {csvImportSummary.successCount}</span>
                    <span>Failed: {csvImportSummary.failedCount}</span>
                  </div>
                  {csvImportSummary.failedCount > 0 && (
                    <div className="mt-3 space-y-1 text-xs text-destructive max-h-36 overflow-auto">
                      {csvImportSummary.rows
                        .filter((row) => row.errors?.length)
                        .slice(0, 10)
                        .map((row) => (
                          <p key={`csv-row-${row.rowNumber}`}>
                            Row {row.rowNumber}: {row.errors.join(", ")}
                          </p>
                        ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Products Table */}
            <Card>
              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead>SKU</TableHead>
                        <TableHead>Price</TableHead>
                        <TableHead>Stock</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {productsLoading ? (
                        Array.from({ length: 5 }).map((_, i) => (
                          <TableRow key={i}>
                            {Array.from({ length: 6 }).map((_, j) => (
                              <TableCell key={j}>
                                <Skeleton className="h-4 w-full" />
                              </TableCell>
                            ))}
                          </TableRow>
                        ))
                      ) : filteredProducts.length > 0 ? (
                        filteredProducts.map((product) => (
                          <TableRow key={product.id}>
                            <TableCell className="font-medium">
                              <div>
                                <p className="line-clamp-1">{product.name}</p>
                                <p className="text-xs text-muted-foreground">{product.partNumber}</p>
                              </div>
                            </TableCell>
                            <TableCell className="font-mono text-sm">{product.sku}</TableCell>
                            <TableCell>{formatCurrency(product.price, product.currency)}</TableCell>
                            <TableCell>
                              <Badge variant={product.stockLevel < 10 ? "destructive" : "secondary"}>
                                {product.stockLevel}
                              </Badge>
                            </TableCell>
                            <TableCell>
                              <Badge
                                variant={
                                  product.status === "active"
                                    ? "default"
                                    : "outline"
                                }
                              >
                                {product.status === "draft" ? "archived" : product.status}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <Button asChild variant="ghost" size="sm" className="bg-white text-black hover:bg-white/90 hover:text-primary border border-white/30">
                                  <Link href={`/admin/products/${product.id}`}>
                                    <Edit className="h-4 w-4" />
                                  </Link>
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="bg-white text-black hover:bg-white/90 hover:text-destructive border border-white/30"
                                  onClick={() => handleDeleteProduct(product.id, product.name)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                            No products found
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </div>
              </CardContent>
            </Card>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2">
                <Button variant="outline" onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}>
                  <ChevronLeft className="h-4 w-4" />
                  Previous
                </Button>
                <div className="flex items-center px-4">
                  <span className="text-sm text-muted-foreground">
                    Page {page} of {totalPages}
                  </span>
                </div>
                <Button variant="outline" onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page === totalPages}>
                  Next
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            )}
          </TabsContent>

          {/* Categories Tab */}
          <TabsContent value="categories" className="space-y-6">
            <Card>
              <CardContent className="p-0">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Slug</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead>Products</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {categoriesLoading ? (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center py-8">
                          <Skeleton className="h-4 w-full" />
                        </TableCell>
                      </TableRow>
                    ) : categories.length > 0 ? (
                      categories.map((category) => (
                        <TableRow key={category.id}>
                          <TableCell className="font-medium">{category.name}</TableCell>
                          <TableCell className="font-mono text-sm">{category.slug}</TableCell>
                          <TableCell className="max-w-xs truncate">{category.description || "-"}</TableCell>
                          <TableCell>{category.productCount || 0}</TableCell>
                          <TableCell>
                            <Badge variant={category.status === "active" ? "default" : "secondary"}>
                              {category.status}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            <div className="flex justify-end gap-2">
                              <Button variant="ghost" size="sm" className="bg-white text-black hover:bg-white/90 hover:text-primary border border-white/30" onClick={() => openCategoryDialog(category)}>
                                <Edit className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="bg-white text-black hover:bg-white/90 hover:text-destructive border border-white/30"
                                onClick={() => handleDeleteCategory(category.id.toString(), category.name)}
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      ))
                    ) : (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                          No categories found
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        {/* Category Dialog */}
        <Dialog open={categoryDialogOpen} onOpenChange={setCategoryDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{editingCategory ? "Edit Category" : `Create Category — ${TOPIC_INFO[selectedTopic].label}`}</DialogTitle>
              <DialogDescription>
                {editingCategory
                  ? "Update category information"
                  : `Add a new category under ${TOPIC_INFO[selectedTopic].label.toLowerCase()} topic`}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="category-name">Name *</Label>
                <Input
                  id="category-name"
                  value={categoryForm.name}
                  onChange={(e) => {
                    const nextName = e.target.value
                    setCategoryForm((prev) => ({
                      ...prev,
                      name: nextName,
                      slug: !editingCategory && !categorySlugTouched
                        ? composeCategorySlug(nextName, prev.parentId)
                        : (prev.slug || generateSlug(nextName)),
                    }))
                  }}
                  placeholder="e.g., Engine Parts"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="category-slug">Slug *</Label>
                <Input
                  id="category-slug"
                  value={categoryForm.slug}
                  onChange={(e) => {
                    setCategorySlugTouched(true)
                    setCategoryForm({ ...categoryForm, slug: e.target.value })
                  }}
                  placeholder="e.g., engine-parts"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="category-parent">Parent Category</Label>
                <Select
                  value={categoryForm.parentId || topicRootCategoryId?.toString() || ""}
                  onValueChange={(value) => {
                    setCategoryForm((prev) => ({
                      ...prev,
                      parentId: value,
                      slug: !editingCategory && !categorySlugTouched
                        ? composeCategorySlug(prev.name, value)
                        : prev.slug,
                    }))
                  }}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select parent category" />
                  </SelectTrigger>
                  <SelectContent>
                    {/* Topic root as default parent */}
                    {topicRootCategoryId && (
                      <SelectItem value={topicRootCategoryId.toString()}>
                        {TOPIC_INFO[selectedTopic].label} (Root)
                      </SelectItem>
                    )}
                    {/* Hierarchical subcategories within this topic */}
                    {parentCategoryOptions.map((option) => (
                      <SelectItem key={option.id} value={option.id.toString()}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Categories are organized under the {TOPIC_INFO[selectedTopic].label} topic
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="category-description">Description</Label>
                <Textarea
                  id="category-description"
                  value={categoryForm.description}
                  onChange={(e) => setCategoryForm({ ...categoryForm, description: e.target.value })}
                  placeholder="Category description..."
                  rows={3}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="category-image-url">Category Image</Label>
                <div className="flex gap-2">
                  <Input
                    id="category-image-url"
                    value={categoryForm.imageUrl}
                    onChange={(e) => setCategoryForm({ ...categoryForm, imageUrl: e.target.value })}
                    placeholder="e.g., categories/toyota-logo.png"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => categoryImageInputRef.current?.click()}
                    disabled={categoryImageUploading}
                  >
                    {categoryImageUploading ? "Uploading..." : "Upload"}
                  </Button>
                </div>
                <input
                  ref={categoryImageInputRef}
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleCategoryImageSelected}
                />
                {categoryForm.imageUrl && (
                  <div className="relative h-16 w-16 overflow-hidden rounded-md border">
                    <Image
                      src={getCategoryImageUrl(categoryForm.imageUrl)}
                      alt="Category preview"
                      fill
                      className="object-cover"
                      sizes="64px"
                    />
                  </div>
                )}
                <p className="text-xs text-muted-foreground">
                  Upload a logo or background image for this category tile.
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="category-status">Status</Label>
                <Select
                  value={categoryForm.status}
                  onValueChange={(value: "active" | "inactive") => setCategoryForm({ ...categoryForm, status: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="active">Active</SelectItem>
                    <SelectItem value="inactive">Inactive</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setCategoryDialogOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleSaveCategory}>
                {editingCategory ? "Update" : "Create"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </Container>
    </section>
  )
}

export default function AdminProductsPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <AdminProductsContent />
    </RequireAuth>
  )
}
