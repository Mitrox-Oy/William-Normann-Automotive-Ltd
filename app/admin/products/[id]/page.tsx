"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { ChevronLeft, Save, Plus, Trash2, Upload, X } from "lucide-react"
import { Container } from "@/components/container"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Switch } from "@/components/ui/switch"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
    getAdminProduct,
    updateProduct,
    createProductVariant,
    uploadProductImage,
    uploadVariantImage,
    deleteProductImage,
    setMainProductImage,
    updateProductVariant,
    deleteProductVariant,
    getAllCategories,
    type AdminProduct,
    type ProductVariantCreateInput
} from "@/lib/adminApi"
import type { ProductVariant } from "@/lib/shopApi"
import { getImageUrl } from "@/lib/utils"

function showToast(message: string, type: 'success' | 'error' | 'warning' = 'success') {
    console.log(`[${type.toUpperCase()}] ${message}`)
    if (type === 'error' || type === 'warning') {
        alert(`${type === 'error' ? 'Error' : 'Warning'}: ${message}`)
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


export default function EditProductPage() {
    const params = useParams()
    const router = useRouter()
    const productId = params.id as string

    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [product, setProduct] = useState<AdminProduct | null>(null)
    const [categories, setCategories] = useState<Category[]>([])
    const [loadingCategories, setLoadingCategories] = useState(true)

    // Form fields - Basic Info
    const [name, setName] = useState("")
    const [description, setDescription] = useState("")
    const [price, setPrice] = useState("")
    const [sku, setSku] = useState("")
    const [stockQuantity, setStockQuantity] = useState("")
    const [categoryId, setCategoryId] = useState<string>("")
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
    const [existingVariants, setExistingVariants] = useState<ProductVariant[]>([])
    const [variants, setVariants] = useState<(ProductVariantCreateInput & { imageFile?: File, imagePreview?: string })[]>([])
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
    const [variantImageFile, setVariantImageFile] = useState<File | null>(null)
    const [variantImagePreview, setVariantImagePreview] = useState<string | null>(null)

    // Images
    const [images, setImages] = useState<File[]>([])
    const [imageUrls, setImageUrls] = useState<string[]>([])
    const [mainImageIndex, setMainImageIndex] = useState<number | null>(null)
    const [existingImages, setExistingImages] = useState<Array<{ id: string, url: string, isMain: boolean }>>([])
    const [editingVariant, setEditingVariant] = useState<ProductVariant | null>(null)
    const [showEditVariantForm, setShowEditVariantForm] = useState(false)
    const [editVariantImageFile, setEditVariantImageFile] = useState<File | null>(null)
    const [editVariantImagePreview, setEditVariantImagePreview] = useState<string | null>(null)

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

    useEffect(() => {
        if (!productId) return

        setLoading(true)
        getAdminProduct(productId)
            .then((product) => {
                setProduct(product)
                // Populate form fields
                setName(product.name || "")
                setDescription(product.description || "")
                setPrice(product.price?.toString() || "")
                setSku(product.sku || "")
                setStockQuantity(product.stockQuantity?.toString() || product.stockLevel?.toString() || "0")
                setCategoryId(product.categoryId?.toString() || product.category || "")
                setWeight(product.weight?.toString() || "")
                setBrand(product.brand || "")
                setActive(product.active ?? true)
                setFeatured(product.featured ?? false)

                // Populate existing images
                if (product.images && product.images.length > 0) {
                    // Handle both array of strings and array of objects (ProductImageResponse)
                    const imageList = product.images.map((img: any) => {
                        if (typeof img === 'string') {
                            return { id: '', url: img, isMain: false }
                        }
                        return {
                            id: img.id?.toString() || '',
                            url: img.imageUrl || img.url || img,
                            isMain: img.isMain || false
                        }
                    })
                    setExistingImages(imageList)
                } else if (product.imageUrl) {
                    setExistingImages([{ id: '', url: product.imageUrl, isMain: true }])
                }

                // Populate existing variants
                if (product.variants && product.variants.length > 0) {
                    setExistingVariants(product.variants)
                }

                // Populate info sections if they exist
                const sections = [...infoSections]
                for (let i = 1; i <= 7; i++) {
                    const titleKey = `infoSection${i}Title` as keyof AdminProduct
                    const contentKey = `infoSection${i}Content` as keyof AdminProduct
                    const enabledKey = `infoSection${i}Enabled` as keyof AdminProduct

                    if (product[titleKey] || product[contentKey]) {
                        sections[i - 1] = {
                            title: (product[titleKey] as string) || "",
                            content: (product[contentKey] as string) || "",
                            enabled: (product[enabledKey] as boolean) ?? false,
                        }
                    }
                }
                setInfoSections(sections)

                setLoading(false)
            })
            .catch((err) => {
                console.error("Failed to load product:", err)
                setError(err.message || "Failed to load product")
                setLoading(false)
            })
    }, [productId])

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

        // Store the variant with its image file if provided
        const variantToAdd: ProductVariantCreateInput & { imageFile?: File, imagePreview?: string } = {
            ...newVariant,
            imageFile: variantImageFile || undefined,
            imagePreview: variantImagePreview || undefined
        }

        setVariants([...variants, variantToAdd])
        setNewVariant({
            name: "",
            sku: "",
            price: undefined,
            stockQuantity: 0,
            active: true,
            defaultVariant: false,
            options: {},
        })
        setVariantImageFile(null)
        setVariantImagePreview(null)
        setShowVariantForm(false)
    }

    function handleVariantImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0]
        if (file) {
            setVariantImageFile(file)
            const reader = new FileReader()
            reader.onload = (e) => {
                setVariantImagePreview(e.target?.result as string)
            }
            reader.readAsDataURL(file)
        }
    }

    function handleEditVariantImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0]
        if (file) {
            setEditVariantImageFile(file)
            const reader = new FileReader()
            reader.onload = (e) => {
                setEditVariantImagePreview(e.target?.result as string)
            }
            reader.readAsDataURL(file)
        }
    }

    function removeVariant(index: number) {
        setVariants(variants.filter((_, i) => i !== index))
    }

    async function handleDeleteExistingImage(imageId: string) {
        if (!confirm('Are you sure you want to delete this image?')) return

        try {
            await deleteProductImage(productId, imageId)
            setExistingImages(existingImages.filter(img => img.id !== imageId))
            showToast('Image deleted successfully', 'success')
        } catch (error) {
            console.error('Failed to delete image:', error)
            showToast('Failed to delete image', 'error')
        }
    }

    async function handleSetMainImage(imageId: string) {
        try {
            await setMainProductImage(productId, imageId)
            setExistingImages(existingImages.map(img => ({
                ...img,
                isMain: img.id === imageId
            })))
            showToast('Main image updated', 'success')
        } catch (error) {
            console.error('Failed to set main image:', error)
            showToast('Failed to set main image', 'error')
        }
    }

    function handleEditVariant(variant: ProductVariant) {
        setEditingVariant(variant)
        setShowEditVariantForm(true)
        // Reset image state
        setEditVariantImageFile(null)
        setEditVariantImagePreview(null)
    }

    async function handleUpdateVariant() {
        if (!editingVariant) return

        try {
            // First upload image if a new one was selected
            let imageUrl = editingVariant.imageUrl
            if (editVariantImageFile) {
                const uploadedImage = await uploadVariantImage(productId, editingVariant.id.toString(), editVariantImageFile)
                imageUrl = uploadedImage.imageUrl
            }

            await updateProductVariant(productId, editingVariant.id.toString(), {
                name: editingVariant.name,
                sku: editingVariant.sku,
                price: editingVariant.price,
                stockQuantity: editingVariant.stockQuantity,
                active: editingVariant.active,
                defaultVariant: editingVariant.defaultVariant,
                options: editingVariant.options,
                imageUrl: imageUrl
            })

            setExistingVariants(existingVariants.map(v =>
                v.id === editingVariant.id ? { ...editingVariant, imageUrl } : v
            ))
            setEditingVariant(null)
            setShowEditVariantForm(false)
            setEditVariantImageFile(null)
            setEditVariantImagePreview(null)
            showToast('Variant updated successfully', 'success')
        } catch (error) {
            console.error('Failed to update variant:', error)
            showToast('Failed to update variant', 'error')
        }
    }

    async function handleDeleteVariant(variantId: number) {
        if (!confirm('Are you sure you want to delete this variant?')) return

        try {
            await deleteProductVariant(productId, variantId.toString())
            setExistingVariants(existingVariants.filter(v => v.id !== variantId))
            showToast('Variant deleted successfully', 'success')
        } catch (error) {
            console.error('Failed to delete variant:', error)
            showToast('Failed to delete variant', 'error')
        }
    }

    function handleImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const files = Array.from(e.target.files || [])
        setImages([...images, ...files])

        const newUrls: string[] = []
        files.forEach((file) => {
            const reader = new FileReader()
            reader.onload = (e) => {
                const url = e.target?.result as string
                newUrls.push(url)
                if (newUrls.length === files.length) {
                    setImageUrls([...imageUrls, ...newUrls])
                }
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

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        if (!product) return

        try {
            setSaving(true)

            const updateData: any = {
                name,
                description: description || undefined,
                price: parseFloat(price),
                sku,
                stockQuantity: parseInt(stockQuantity) || 0,
                weight: weight ? parseFloat(weight) : undefined,
                brand: brand || undefined,
                active,
                featured,
            }

            if (categoryId) {
                updateData.categoryId = parseInt(categoryId)
            }

            // Add info sections
            infoSections.forEach((section, index) => {
                const sectionNum = index + 1
                if (section.enabled && section.title && section.content) {
                    updateData[`infoSection${sectionNum}Title`] = section.title
                    updateData[`infoSection${sectionNum}Content`] = section.content
                    updateData[`infoSection${sectionNum}Enabled`] = true
                } else {
                    // Clear disabled sections
                    updateData[`infoSection${sectionNum}Title`] = ""
                    updateData[`infoSection${sectionNum}Content`] = ""
                    updateData[`infoSection${sectionNum}Enabled`] = false
                }
            })

            await updateProduct(productId, updateData)
            showToast("Product updated successfully!", "success")

            // Upload new images if any
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

            // Create new variants if any
            if (variants.length > 0) {
                for (let i = 0; i < variants.length; i++) {
                    const variant = variants[i]
                    try {
                        const createdVariant = await createProductVariant(productId, variant)

                        // Upload variant image if there's one
                        if (variant.imageFile) {
                            try {
                                await uploadVariantImage(productId, createdVariant.id.toString(), variant.imageFile)
                            } catch (imgError) {
                                console.error(`Failed to upload image for variant ${variant.name}:`, imgError)
                                showToast(`Variant created but image upload failed for ${variant.name}`, "warning")
                            }
                        }
                    } catch (error) {
                        console.error(`Failed to create variant ${variant.name}:`, error)
                        showToast(`Failed to create variant ${variant.name}`, "warning")
                    }
                }
            }

            router.push("/admin/products")
        } catch (error: any) {
            console.error("Failed to update product:", error)
            showToast(error.message || "Failed to update product", "error")
        } finally {
            setSaving(false)
        }
    }

    if (loading) {
        return (
            <section className="py-24 lg:py-32">
                <Container>
                    <div className="mb-8">
                        <Link href="/admin/products" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
                            <ChevronLeft className="mr-1 h-4 w-4" />
                            Back to Products
                        </Link>
                    </div>
                    <p>Loading product...</p>
                </Container>
            </section>
        )
    }

    if (error || !product) {
        return (
            <section className="py-24 lg:py-32">
                <Container>
                    <div className="mb-8">
                        <Link href="/admin/products" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
                            <ChevronLeft className="mr-1 h-4 w-4" />
                            Back to Products
                        </Link>
                    </div>
                    <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
                        <p className="text-destructive">{error || "Product not found"}</p>
                    </div>
                    <Button onClick={() => router.push("/admin/products")} className="mt-4">
                        Return to Products
                    </Button>
                </Container>
            </section>
        )
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
                        <h1 className="text-3xl font-bold mb-2">Edit Product</h1>
                        <p className="text-muted-foreground">Update product details</p>
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
                                    <CardDescription>Core product details</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div>
                                        <Label htmlFor="name">Product Name *</Label>
                                        <Input
                                            id="name"
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            required
                                            placeholder="e.g., Premium Brake Pads"
                                        />
                                    </div>

                                    <div>
                                        <Label htmlFor="description">Description</Label>
                                        <Textarea
                                            id="description"
                                            value={description}
                                            onChange={(e) => setDescription(e.target.value)}
                                            rows={4}
                                            placeholder="Product description..."
                                            maxLength={1000}
                                        />
                                        <p className="text-xs text-muted-foreground mt-1">{description.length}/1000 characters</p>
                                    </div>

                                    <div className="grid gap-4 sm:grid-cols-2">
                                        <div>
                                            <Label htmlFor="sku">SKU *</Label>
                                            <Input
                                                id="sku"
                                                value={sku}
                                                onChange={(e) => setSku(e.target.value)}
                                                required
                                                placeholder="e.g., BRK-001"
                                            />
                                        </div>

                                        <div>
                                            <Label htmlFor="category">Category</Label>
                                            <Select value={categoryId} onValueChange={setCategoryId}>
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
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>Pricing & Inventory</CardTitle>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="grid gap-4 sm:grid-cols-3">
                                        <div>
                                            <Label htmlFor="price">Price (€) *</Label>
                                            <Input
                                                id="price"
                                                type="number"
                                                step="0.01"
                                                min="0"
                                                value={price}
                                                onChange={(e) => setPrice(e.target.value)}
                                                required
                                                placeholder="0.00"
                                            />
                                        </div>

                                        <div>
                                            <Label htmlFor="stockQuantity">Stock Quantity *</Label>
                                            <Input
                                                id="stockQuantity"
                                                type="number"
                                                min="0"
                                                value={stockQuantity}
                                                onChange={(e) => setStockQuantity(e.target.value)}
                                                required
                                                placeholder="0"
                                            />
                                        </div>

                                        <div>
                                            <Label htmlFor="weight">Weight (kg)</Label>
                                            <Input
                                                id="weight"
                                                type="number"
                                                step="0.01"
                                                min="0"
                                                value={weight}
                                                onChange={(e) => setWeight(e.target.value)}
                                                placeholder="0.00"
                                            />
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>Product Status</CardTitle>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <Label htmlFor="active">Active</Label>
                                            <p className="text-sm text-muted-foreground">
                                                Product is visible in the shop
                                            </p>
                                        </div>
                                        <Switch
                                            id="active"
                                            checked={active}
                                            onCheckedChange={setActive}
                                        />
                                    </div>

                                    <div className="flex items-center justify-between">
                                        <div>
                                            <Label htmlFor="featured">Featured</Label>
                                            <p className="text-sm text-muted-foreground">
                                                Show on homepage and featured sections
                                            </p>
                                        </div>
                                        <Switch
                                            id="featured"
                                            checked={featured}
                                            onCheckedChange={setFeatured}
                                        />
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
                                    <div>
                                        <Label htmlFor="brand">Brand</Label>
                                        <Input
                                            id="brand"
                                            value={brand}
                                            onChange={(e) => setBrand(e.target.value)}
                                            placeholder="e.g., Bosch"
                                            maxLength={100}
                                        />
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
                                    {/* Existing Images */}
                                    {existingImages.length > 0 && (
                                        <div>
                                            <Label className="mb-2 block">Current Images</Label>
                                            <div className="grid grid-cols-4 gap-4 mb-4">
                                                {existingImages.map((img, index) => (
                                                    <div key={`existing-${index}`} className="relative group">
                                                        <img
                                                            src={img.url}
                                                            alt={`Existing image ${index + 1}`}
                                                            className="w-full h-32 object-cover rounded-lg border"
                                                        />
                                                        {img.isMain && (
                                                            <div className="absolute top-2 left-2 bg-primary text-primary-foreground text-xs px-2 py-1 rounded">
                                                                Main
                                                            </div>
                                                        )}
                                                        <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-lg flex items-center justify-center gap-2">
                                                            {!img.isMain && img.id && (
                                                                <Button
                                                                    type="button"
                                                                    variant="secondary"
                                                                    size="sm"
                                                                    onClick={() => handleSetMainImage(img.id)}
                                                                >
                                                                    Set Main
                                                                </Button>
                                                            )}
                                                            {img.id && (
                                                                <Button
                                                                    type="button"
                                                                    variant="destructive"
                                                                    size="sm"
                                                                    onClick={() => handleDeleteExistingImage(img.id)}
                                                                >
                                                                    <Trash2 className="h-4 w-4" />
                                                                </Button>
                                                            )}
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    )}

                                    {/* Upload New Images */}
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

                                    {/* New Images Preview */}
                                    {imageUrls.length > 0 && (
                                        <div>
                                            <Label className="mb-2 block">New Images to Upload</Label>
                                            <div className="grid grid-cols-4 gap-4">
                                                {imageUrls.map((url, index) => (
                                                    <div key={`new-${index}`} className="relative group">
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
                                    {/* Existing Variants */}
                                    {existingVariants.length > 0 && (
                                        <div className="space-y-2">
                                            <Label>Current Variants</Label>
                                            {existingVariants.map((variant, index) => (
                                                <div key={`existing-${variant.id}`} className="flex items-center justify-between p-3 border rounded-lg bg-muted/30">
                                                    <div className="flex items-center gap-4">
                                                        {variant.imageUrl && (
                                                            <img
                                                                src={variant.imageUrl}
                                                                alt={variant.name}
                                                                className="w-12 h-12 object-cover rounded"
                                                            />
                                                        )}
                                                        <div>
                                                            <p className="font-medium">{variant.name}</p>
                                                            <p className="text-sm text-muted-foreground">SKU: {variant.sku}</p>
                                                            {variant.price && <p className="text-sm">Price: €{variant.price}</p>}
                                                            <p className="text-sm text-muted-foreground">Stock: {variant.stockQuantity}</p>
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center gap-2">
                                                        {variant.defaultVariant && (
                                                            <span className="text-xs bg-primary text-primary-foreground px-2 py-1 rounded">Default</span>
                                                        )}
                                                        {variant.active ? (
                                                            <span className="text-xs bg-green-500 text-white px-2 py-1 rounded">Active</span>
                                                        ) : (
                                                            <span className="text-xs bg-gray-500 text-white px-2 py-1 rounded">Inactive</span>
                                                        )}
                                                        <Button
                                                            type="button"
                                                            variant="outline"
                                                            size="sm"
                                                            onClick={() => handleEditVariant(variant)}
                                                        >
                                                            Edit
                                                        </Button>
                                                        <Button
                                                            type="button"
                                                            variant="destructive"
                                                            size="sm"
                                                            onClick={() => handleDeleteVariant(variant.id)}
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {/* New Variants to Create */}
                                    {variants.length > 0 && (
                                        <div className="space-y-2">
                                            <Label>New Variants to Create</Label>
                                            {variants.map((variant, index) => (
                                                <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                                                    <div className="flex items-center gap-4">
                                                        {variant.imagePreview && (
                                                            <img
                                                                src={variant.imagePreview}
                                                                alt={variant.name}
                                                                className="w-12 h-12 object-cover rounded"
                                                            />
                                                        )}
                                                        <div>
                                                            <p className="font-medium">{variant.name}</p>
                                                            <p className="text-sm text-muted-foreground">SKU: {variant.sku}</p>
                                                            {variant.price && <p className="text-sm">Price: €{variant.price}</p>}
                                                            {variant.imageFile && (
                                                                <p className="text-sm text-primary">📷 Image attached</p>
                                                            )}
                                                        </div>
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
                                                            placeholder="e.g., Pink / 128 GB"
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label htmlFor="variant-sku">Variant SKU *</Label>
                                                        <Input
                                                            id="variant-sku"
                                                            value={newVariant.sku}
                                                            onChange={(e) => setNewVariant({ ...newVariant, sku: e.target.value })}
                                                            placeholder="e.g., PHN-128-PNK"
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

                                                {/* Variant Image Upload */}
                                                <div className="space-y-2">
                                                    <Label htmlFor="variant-image">Variant Image (Optional)</Label>
                                                    <div className="flex items-center gap-4">
                                                        <Input
                                                            id="variant-image"
                                                            type="file"
                                                            accept="image/*"
                                                            onChange={handleVariantImageSelect}
                                                            className="flex-1"
                                                        />
                                                        {variantImagePreview && (
                                                            <div className="relative">
                                                                <img
                                                                    src={variantImagePreview}
                                                                    alt="Variant preview"
                                                                    className="w-16 h-16 object-cover rounded border"
                                                                />
                                                                <Button
                                                                    type="button"
                                                                    variant="destructive"
                                                                    size="sm"
                                                                    className="absolute -top-2 -right-2 h-5 w-5 p-0"
                                                                    onClick={() => {
                                                                        setVariantImageFile(null)
                                                                        setVariantImagePreview(null)
                                                                    }}
                                                                >
                                                                    <X className="h-3 w-3" />
                                                                </Button>
                                                            </div>
                                                        )}
                                                    </div>
                                                    <p className="text-xs text-muted-foreground">Upload a variant-specific image (e.g., pink phone for pink variant)</p>
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
                                                    <Button type="button" variant="outline" onClick={() => {
                                                        setShowVariantForm(false)
                                                        setVariantImageFile(null)
                                                        setVariantImagePreview(null)
                                                    }}>
                                                        Cancel
                                                    </Button>
                                                </div>
                                            </CardContent>
                                        </Card>
                                    ) : (
                                        <Button type="button" variant="outline" onClick={() => setShowVariantForm(true)}>
                                            <Plus className="mr-2 h-4 w-4" />
                                            Add New Variant
                                        </Button>
                                    )}

                                    {/* Edit Variant Form */}
                                    {showEditVariantForm && editingVariant && (
                                        <Card className="border-primary">
                                            <CardHeader>
                                                <CardTitle>Edit Variant</CardTitle>
                                            </CardHeader>
                                            <CardContent className="space-y-4">
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-name">Variant Name *</Label>
                                                        <Input
                                                            id="edit-variant-name"
                                                            value={editingVariant.name}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, name: e.target.value })}
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-sku">Variant SKU *</Label>
                                                        <Input
                                                            id="edit-variant-sku"
                                                            value={editingVariant.sku}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, sku: e.target.value })}
                                                        />
                                                    </div>
                                                </div>
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-price">Price Override</Label>
                                                        <Input
                                                            id="edit-variant-price"
                                                            type="number"
                                                            step="0.01"
                                                            min="0"
                                                            value={editingVariant.price || ""}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, price: e.target.value ? parseFloat(e.target.value) : undefined })}
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-stock">Stock Quantity</Label>
                                                        <Input
                                                            id="edit-variant-stock"
                                                            type="number"
                                                            min="0"
                                                            value={editingVariant.stockQuantity || 0}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, stockQuantity: parseInt(e.target.value) || 0 })}
                                                        />
                                                    </div>
                                                </div>

                                                {/* Variant Image Upload/Edit */}
                                                <div className="space-y-2">
                                                    <Label htmlFor="edit-variant-image">Variant Image</Label>
                                                    <div className="space-y-2">
                                                        {/* Current variant image */}
                                                        {editingVariant.imageUrl && !editVariantImagePreview && (
                                                            <div className="flex items-center gap-4">
                                                                <div className="relative">
                                                                    <img
                                                                        src={getImageUrl(editingVariant.imageUrl)}
                                                                        alt="Current variant"
                                                                        className="w-20 h-20 object-cover rounded border"
                                                                    />
                                                                    <Button
                                                                        type="button"
                                                                        variant="destructive"
                                                                        size="sm"
                                                                        className="absolute -top-2 -right-2 h-5 w-5 p-0"
                                                                        onClick={() => {
                                                                            setEditingVariant({ ...editingVariant, imageUrl: undefined })
                                                                        }}
                                                                    >
                                                                        <X className="h-3 w-3" />
                                                                    </Button>
                                                                </div>
                                                                <span className="text-sm text-muted-foreground">Current image</span>
                                                            </div>
                                                        )}

                                                        {/* New image upload */}
                                                        <div className="flex items-center gap-4">
                                                            <Input
                                                                id="edit-variant-image"
                                                                type="file"
                                                                accept="image/*"
                                                                onChange={handleEditVariantImageSelect}
                                                                className="flex-1"
                                                            />
                                                            {editVariantImagePreview && (
                                                                <div className="relative">
                                                                    <img
                                                                        src={editVariantImagePreview}
                                                                        alt="New variant preview"
                                                                        className="w-16 h-16 object-cover rounded border"
                                                                    />
                                                                    <Button
                                                                        type="button"
                                                                        variant="destructive"
                                                                        size="sm"
                                                                        className="absolute -top-2 -right-2 h-5 w-5 p-0"
                                                                        onClick={() => {
                                                                            setEditVariantImageFile(null)
                                                                            setEditVariantImagePreview(null)
                                                                        }}
                                                                    >
                                                                        <X className="h-3 w-3" />
                                                                    </Button>
                                                                </div>
                                                            )}
                                                        </div>
                                                        <p className="text-xs text-muted-foreground">
                                                            {editingVariant.imageUrl || editVariantImagePreview
                                                                ? "Upload a new image to replace the current one"
                                                                : "Upload a variant-specific image"}
                                                        </p>
                                                    </div>
                                                </div>

                                                <div className="flex items-center space-x-4">
                                                    <div className="flex items-center space-x-2">
                                                        <Switch
                                                            checked={editingVariant.active ?? true}
                                                            onCheckedChange={(checked) => setEditingVariant({ ...editingVariant, active: checked })}
                                                        />
                                                        <Label>Active</Label>
                                                    </div>
                                                    <div className="flex items-center space-x-2">
                                                        <Switch
                                                            checked={editingVariant.defaultVariant ?? false}
                                                            onCheckedChange={(checked) => setEditingVariant({ ...editingVariant, defaultVariant: checked })}
                                                        />
                                                        <Label>Default Variant</Label>
                                                    </div>
                                                </div>
                                                <div className="flex gap-2">
                                                    <Button type="button" onClick={handleUpdateVariant}>
                                                        Save Changes
                                                    </Button>
                                                    <Button type="button" variant="outline" onClick={() => {
                                                        setShowEditVariantForm(false)
                                                        setEditingVariant(null)
                                                    }}>
                                                        Cancel
                                                    </Button>
                                                </div>
                                            </CardContent>
                                        </Card>
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

                    <div className="mt-6 flex gap-4">
                        <Button type="submit" disabled={saving} className="flex-1">
                            <Save className="mr-2 h-4 w-4" />
                            {saving ? "Saving..." : "Save Changes"}
                        </Button>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => router.push("/admin/products")}
                        >
                            Cancel
                        </Button>
                    </div>
                </form>
            </Container>
        </section>
    )
}
