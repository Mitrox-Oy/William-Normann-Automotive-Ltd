"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { RequireAuth } from "@/components/AuthProvider"
import { createProduct, createProductVariant, uploadProductImage, getAllCategories, type ProductCreateInput, type ProductVariantCreateInput } from "@/lib/adminApi"
import { ChevronLeft, Plus, Trash2, Upload, X } from "lucide-react"
import Link from "next/link"
// Simple toast implementation - replace with proper toast library if needed
function showToast(message: string, type: 'success' | 'error' | 'warning' = 'success') {
  // For now, use console and alert - replace with proper toast library
  console.log(`[${type.toUpperCase()}] ${message}`)
  if (type === 'error') {
    alert(`Error: ${message}`)
  } else if (type === 'warning') {
    alert(`Warning: ${message}`)
  } else {
    // Success messages can be less intrusive
    console.log(`Success: ${message}`)
  }
}

interface Category {
  id: number
  name: string
  slug: string
  description?: string
}

interface InfoSection {
  title: string
  content: string
  enabled: boolean
}

function NewProductPageContent() {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const [categories, setCategories] = useState<Category[]>([])
  const [loadingCategories, setLoadingCategories] = useState(true)
  
  // Basic product info
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [price, setPrice] = useState("")
  const [sku, setSku] = useState("")
  const [stockQuantity, setStockQuantity] = useState("0")
  const [categoryId, setCategoryId] = useState<string>("")
  const [imageUrl, setImageUrl] = useState("")
  const [weight, setWeight] = useState("")
  const [brand, setBrand] = useState("")
  const [active, setActive] = useState(true)
  const [featured, setFeatured] = useState(false)
  
  // Info sections (1-7)
  const [infoSections, setInfoSections] = useState<InfoSection[]>([
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
  ])
  
  // Variants
  const [variants, setVariants] = useState<ProductVariantCreateInput[]>([])
  const [showVariantForm, setShowVariantForm] = useState(false)
  const [newVariant, setNewVariant] = useState<ProductVariantCreateInput>({
    name: "",
    sku: "",
    price: undefined,
    stockQuantity: 0,
    active: true,
    defaultVariant: false,
    options: {},
  })
  
  // Images
  const [images, setImages] = useState<File[]>([])
  const [imageUrls, setImageUrls] = useState<string[]>([])
  const [mainImageIndex, setMainImageIndex] = useState<number | null>(null)
  
  useEffect(() => {
    loadCategories()
  }, [])
  
  async function loadCategories() {
    try {
      setLoadingCategories(true)
      const cats = await getAllCategories()
      setCategories(cats)
    } catch (error) {
      console.error("Failed to load categories:", error)
      showToast("Failed to load categories", "error")
    } finally {
      setLoadingCategories(false)
    }
  }
  
  function updateInfoSection(index: number, field: keyof InfoSection, value: string | boolean) {
    const updated = [...infoSections]
    updated[index] = { ...updated[index], [field]: value }
    setInfoSections(updated)
  }
  
  function addVariant() {
    if (!newVariant.name || !newVariant.sku) {
      showToast("Variant name and SKU are required", "error")
      return
    }
    
    setVariants([...variants, { ...newVariant }])
    setNewVariant({
      name: "",
      sku: "",
      price: undefined,
      stockQuantity: 0,
      active: true,
      defaultVariant: false,
      options: {},
    })
    setShowVariantForm(false)
  }
  
  function removeVariant(index: number) {
    setVariants(variants.filter((_, i) => i !== index))
  }
  
  function handleImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files || [])
    setImages([...images, ...files])
    files.forEach((file) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        setImageUrls([...imageUrls, e.target?.result as string])
      }
      reader.readAsDataURL(file)
    })
  }
  
  function removeImage(index: number) {
    setImages(images.filter((_, i) => i !== index))
    setImageUrls(imageUrls.filter((_, i) => i !== index))
    if (mainImageIndex === index) {
      setMainImageIndex(null)
    } else if (mainImageIndex !== null && mainImageIndex > index) {
      setMainImageIndex(mainImageIndex - 1)
    }
  }
  
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    
    if (!name || !sku || !price || !categoryId) {
      showToast("Please fill in all required fields", "error")
      return
    }
    
    try {
      setLoading(true)
      
      // Build product data
      const productData: ProductCreateInput = {
        name,
        description: description || undefined,
        price: parseFloat(price),
        stockQuantity: parseInt(stockQuantity) || 0,
        sku,
        categoryId: parseInt(categoryId),
        imageUrl: imageUrl || undefined,
        active,
        featured,
        weight: weight ? parseFloat(weight) : undefined,
        brand: brand || undefined,
      }
      
      // Add info sections
      infoSections.forEach((section, index) => {
        const sectionNum = index + 1
        if (section.enabled && section.title && section.content) {
          productData[`infoSection${sectionNum}Title` as keyof ProductCreateInput] = section.title
          productData[`infoSection${sectionNum}Content` as keyof ProductCreateInput] = section.content
          productData[`infoSection${sectionNum}Enabled` as keyof ProductCreateInput] = true
        }
      })
      
      // Create product
      const createdProduct = await createProduct(productData)
      const productId = createdProduct.id.toString()
      
      showToast("Product created successfully!", "success")
      
      // Upload images
      if (images.length > 0) {
        for (let i = 0; i < images.length; i++) {
          const isMain = mainImageIndex === i
          try {
            await uploadProductImage(productId, images[i], isMain)
          } catch (error) {
            console.error(`Failed to upload image ${i + 1}:`, error)
            showToast(`Failed to upload image ${i + 1}`, "warning")
          }
        }
      }
      
      // Create variants
      if (variants.length > 0) {
        for (const variant of variants) {
          try {
            await createProductVariant(productId, variant)
          } catch (error) {
            console.error(`Failed to create variant ${variant.name}:`, error)
            showToast(`Failed to create variant ${variant.name}`, "warning")
          }
        }
      }
      
      // Redirect to product list or edit page
      router.push(`/admin/products`)
    } catch (error: any) {
      console.error("Failed to create product:", error)
      showToast(error.message || "Failed to create product", "error")
    } finally {
      setLoading(false)
    }
  }
  
  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/admin/products" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Products
          </Link>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="mb-8">
            <SectionHeading title="Create New Product" subtitle="Add a new product to your catalog" />
          </div>
          
          <Tabs defaultValue="basic" className="space-y-6">
            <TabsList>
              <TabsTrigger value="basic">Basic Info</TabsTrigger>
              <TabsTrigger value="details">Details</TabsTrigger>
              <TabsTrigger value="images">Images</TabsTrigger>
              <TabsTrigger value="variants">Variants</TabsTrigger>
              <TabsTrigger value="info-sections">Info Sections</TabsTrigger>
            </TabsList>
            
            {/* Basic Info Tab */}
            <TabsContent value="basic" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Basic Information</CardTitle>
                  <CardDescription>Essential product details</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="name">Product Name *</Label>
                    <Input
                      id="name"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="e.g., iPhone 15 Pro"
                      required
                      minLength={2}
                      maxLength={200}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="sku">SKU *</Label>
                    <Input
                      id="sku"
                      value={sku}
                      onChange={(e) => setSku(e.target.value)}
                      placeholder="e.g., IPH15P-256-BLK"
                      required
                      minLength={3}
                      maxLength={50}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="category">Category *</Label>
                    <Select value={categoryId} onValueChange={setCategoryId} required>
                      <SelectTrigger>
                        <SelectValue placeholder="Select a category" />
                      </SelectTrigger>
                      <SelectContent>
                        {loadingCategories ? (
                          <SelectItem value="loading" disabled>Loading categories...</SelectItem>
                        ) : categories.length > 0 ? (
                          categories.map((cat) => (
                            <SelectItem key={cat.id} value={cat.id.toString()}>
                              {cat.name}
                            </SelectItem>
                          ))
                        ) : (
                          <SelectItem value="none" disabled>No categories available</SelectItem>
                        )}
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="price">Price *</Label>
                      <Input
                        id="price"
                        type="number"
                        step="0.01"
                        min="0"
                        value={price}
                        onChange={(e) => setPrice(e.target.value)}
                        placeholder="0.00"
                        required
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="stock">Stock Quantity</Label>
                      <Input
                        id="stock"
                        type="number"
                        min="0"
                        value={stockQuantity}
                        onChange={(e) => setStockQuantity(e.target.value)}
                        placeholder="0"
                      />
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="description">Description</Label>
                    <Textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Product description (max 1000 characters)"
                      maxLength={1000}
                      rows={4}
                    />
                    <p className="text-xs text-muted-foreground">{description.length}/1000 characters</p>
                  </div>
                  
                  <div className="flex items-center space-x-6">
                    <div className="flex items-center space-x-2">
                      <Switch id="active" checked={active} onCheckedChange={setActive} />
                      <Label htmlFor="active">Active</Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Switch id="featured" checked={featured} onCheckedChange={setFeatured} />
                      <Label htmlFor="featured">Featured</Label>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
            
            {/* Details Tab */}
            <TabsContent value="details" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Additional Details</CardTitle>
                  <CardDescription>Optional product information</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="brand">Brand</Label>
                      <Input
                        id="brand"
                        value={brand}
                        onChange={(e) => setBrand(e.target.value)}
                        placeholder="e.g., Apple"
                        maxLength={100}
                      />
                    </div>
                    
                    <div className="space-y-2">
                      <Label htmlFor="weight">Weight (kg)</Label>
                      <Input
                        id="weight"
                        type="number"
                        step="0.001"
                        min="0"
                        value={weight}
                        onChange={(e) => setWeight(e.target.value)}
                        placeholder="0.000"
                      />
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <Label htmlFor="imageUrl">Main Image URL</Label>
                    <Input
                      id="imageUrl"
                      value={imageUrl}
                      onChange={(e) => setImageUrl(e.target.value)}
                      placeholder="https://example.com/image.jpg"
                    />
                    <p className="text-xs text-muted-foreground">Or upload images in the Images tab</p>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
            
            {/* Images Tab */}
            <TabsContent value="images" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Product Images</CardTitle>
                  <CardDescription>Upload product images (max 10 images)</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-6">
                    <div className="flex flex-col items-center justify-center space-y-4">
                      <Upload className="h-12 w-12 text-muted-foreground" />
                      <div className="text-center">
                        <Label htmlFor="image-upload" className="cursor-pointer">
                          <span className="text-primary hover:underline">Click to upload</span> or drag and drop
                        </Label>
                        <Input
                          id="image-upload"
                          type="file"
                          accept="image/*"
                          multiple
                          onChange={handleImageSelect}
                          className="hidden"
                        />
                        <p className="text-xs text-muted-foreground mt-2">PNG, JPG, GIF up to 5MB each</p>
                      </div>
                    </div>
                  </div>
                  
                  {imageUrls.length > 0 && (
                    <div className="grid grid-cols-4 gap-4">
                      {imageUrls.map((url, index) => (
                        <div key={index} className="relative group">
                          <img
                            src={url}
                            alt={`Product image ${index + 1}`}
                            className="w-full h-32 object-cover rounded-lg border"
                          />
                          {mainImageIndex === index && (
                            <div className="absolute top-2 left-2 bg-primary text-primary-foreground text-xs px-2 py-1 rounded">
                              Main
                            </div>
                          )}
                          <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-lg flex items-center justify-center gap-2">
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              onClick={() => setMainImageIndex(index)}
                            >
                              Set Main
                            </Button>
                            <Button
                              type="button"
                              variant="destructive"
                              size="sm"
                              onClick={() => removeImage(index)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
            
            {/* Variants Tab */}
            <TabsContent value="variants" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Product Variants</CardTitle>
                  <CardDescription>Add product variants (e.g., sizes, colors)</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  {variants.length > 0 && (
                    <div className="space-y-2">
                      {variants.map((variant, index) => (
                        <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                          <div>
                            <p className="font-medium">{variant.name}</p>
                            <p className="text-sm text-muted-foreground">SKU: {variant.sku}</p>
                            {variant.price && <p className="text-sm">Price: ${variant.price}</p>}
                          </div>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => removeVariant(index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                  
                  {showVariantForm ? (
                    <Card>
                      <CardContent className="pt-6 space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="variant-name">Variant Name *</Label>
                            <Input
                              id="variant-name"
                              value={newVariant.name}
                              onChange={(e) => setNewVariant({ ...newVariant, name: e.target.value })}
                              placeholder="e.g., Midnight / 256 GB"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="variant-sku">Variant SKU *</Label>
                            <Input
                              id="variant-sku"
                              value={newVariant.sku}
                              onChange={(e) => setNewVariant({ ...newVariant, sku: e.target.value })}
                              placeholder="e.g., IPH15P-256-MID"
                            />
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="variant-price">Price Override</Label>
                            <Input
                              id="variant-price"
                              type="number"
                              step="0.01"
                              min="0"
                              value={newVariant.price || ""}
                              onChange={(e) => setNewVariant({ ...newVariant, price: e.target.value ? parseFloat(e.target.value) : undefined })}
                              placeholder="Leave empty to use product price"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="variant-stock">Stock Quantity</Label>
                            <Input
                              id="variant-stock"
                              type="number"
                              min="0"
                              value={newVariant.stockQuantity || 0}
                              onChange={(e) => setNewVariant({ ...newVariant, stockQuantity: parseInt(e.target.value) || 0 })}
                            />
                          </div>
                        </div>
                        <div className="flex items-center space-x-4">
                          <div className="flex items-center space-x-2">
                            <Switch
                              checked={newVariant.active ?? true}
                              onCheckedChange={(checked) => setNewVariant({ ...newVariant, active: checked })}
                            />
                            <Label>Active</Label>
                          </div>
                          <div className="flex items-center space-x-2">
                            <Switch
                              checked={newVariant.defaultVariant ?? false}
                              onCheckedChange={(checked) => setNewVariant({ ...newVariant, defaultVariant: checked })}
                            />
                            <Label>Default Variant</Label>
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <Button type="button" onClick={addVariant}>
                            Add Variant
                          </Button>
                          <Button type="button" variant="outline" onClick={() => setShowVariantForm(false)}>
                            Cancel
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  ) : (
                    <Button type="button" variant="outline" onClick={() => setShowVariantForm(true)}>
                      <Plus className="mr-2 h-4 w-4" />
                      Add Variant
                    </Button>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
            
            {/* Info Sections Tab */}
            <TabsContent value="info-sections" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Information Sections</CardTitle>
                  <CardDescription>Add up to 7 custom information sections</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  {infoSections.map((section, index) => (
                    <Card key={index}>
                      <CardHeader>
                        <div className="flex items-center justify-between">
                          <CardTitle className="text-lg">Section {index + 1}</CardTitle>
                          <div className="flex items-center space-x-2">
                            <Switch
                              checked={section.enabled}
                              onCheckedChange={(checked) => updateInfoSection(index, "enabled", checked)}
                            />
                            <Label>Enable</Label>
                          </div>
                        </div>
                      </CardHeader>
                      {section.enabled && (
                        <CardContent className="space-y-4">
                          <div className="space-y-2">
                            <Label htmlFor={`section-${index}-title`}>Title</Label>
                            <Input
                              id={`section-${index}-title`}
                              value={section.title}
                              onChange={(e) => updateInfoSection(index, "title", e.target.value)}
                              placeholder="e.g., Warranty Information"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor={`section-${index}-content`}>Content</Label>
                            <Textarea
                              id={`section-${index}-content`}
                              value={section.content}
                              onChange={(e) => updateInfoSection(index, "content", e.target.value)}
                              placeholder="Section content..."
                              rows={4}
                            />
                          </div>
                        </CardContent>
                      )}
                    </Card>
                  ))}
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
          
          <div className="mt-6 flex justify-end gap-4">
            <Button type="button" variant="outline" onClick={() => router.back()}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Creating..." : "Create Product"}
            </Button>
          </div>
        </form>
      </Container>
    </section>
  )
}

export default function NewProductPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <NewProductPageContent />
    </RequireAuth>
  )
}

