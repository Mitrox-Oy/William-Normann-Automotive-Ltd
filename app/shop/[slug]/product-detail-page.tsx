"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { useCart } from "@/components/CartContext"
import {
  fetchProductBySlug,
  formatCurrency,
  getAvailabilityBadge,
  type Product
} from "@/lib/shopApi"
import { openLeadInWhatsApp } from "@/lib/whatsappLead"
import { getImageUrl } from "@/lib/utils"
import { ShoppingCart, Minus, Plus, ArrowLeft, Package, Truck, Shield, FileText, CheckCircle2 } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { motion } from "framer-motion"
import type { ProductVariant } from "@/lib/shopApi"

interface ProductDetailPageProps {
  slug: string
}

export default function ProductDetailPage({ slug }: ProductDetailPageProps) {
  const router = useRouter()
  const searchParams = useSearchParams()

  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedImage, setSelectedImage] = useState(0)
  const [quantity, setQuantity] = useState(1)
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null)
  const [showQuoteDialog, setShowQuoteDialog] = useState(false)
  const [quoteFormState, setQuoteFormState] = useState<"idle" | "loading" | "success" | "error">("idle")

  const { addItem } = useCart()

  useEffect(() => {
    if (!slug) return

    setLoading(true)
    setError(null)

    fetchProductBySlug(slug)
      .then((data) => {
        if (data === null) {
          setError('Product not found')
          setProduct(null)
        } else {
          setProduct(data)
          // Always default to the first (main) image for a newly loaded product.
          setSelectedImage(0)
          // Auto-select variant from URL or default variant
          if (data.variants && data.variants.length > 0) {
            const variantId = searchParams.get('variant')
            let variantToSelect: ProductVariant | null = null

            if (variantId) {
              // Try to find variant by ID from URL
              variantToSelect = data.variants.find(v => v.id.toString() === variantId) || null
            }

            // Fallback to default variant or first variant
            if (!variantToSelect) {
              variantToSelect = data.variants.find(v => v.defaultVariant) || data.variants[0]
            }

            setSelectedVariant(variantToSelect)
          }
        }
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message || 'Failed to load product')
        setProduct(null)
        setLoading(false)
      })
  }, [slug])

  // Handle variant selection from URL when product changes
  useEffect(() => {
    if (!product || !product.variants || product.variants.length === 0) return

    const variantId = searchParams.get('variant')
    if (variantId) {
      const variant = product.variants.find(v => v.id.toString() === variantId)
      if (variant && variant.id !== selectedVariant?.id) {
        setSelectedVariant(variant)
      }
    }
  }, [searchParams, product])

  const handleVariantSelect = (variant: ProductVariant) => {
    setSelectedVariant(variant)
    // Update URL with variant query parameter
    const params = new URLSearchParams(searchParams.toString())
    params.set('variant', variant.id.toString())
    router.push(`/shop/${slug}?${params.toString()}`, { scroll: false })
  }

  const handleAddToCart = () => {
    if (product) {
      if (product.quoteOnly) {
        return
      }
      addItem(product, quantity)
    }
  }

  const handleRequestQuote = () => {
    setShowQuoteDialog(true)
    setQuoteFormState("idle")
  }

  const handleQuoteSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setQuoteFormState("loading")

    const formData = new FormData(e.currentTarget)
    const data = {
      name: formData.get("name"),
      email: formData.get("email"),
      phone: formData.get("phone"),
      message: formData.get("message"),
      product: quoteProductName,
      // Send SKU (variant SKU if selected, otherwise product SKU) for lead follow-up.
      partNumber: selectedVariant?.sku || product?.sku || '',
      quantity: quantity.toString(),
      source: "product_quote",
    }

    try {
      openLeadInWhatsApp({
        source: "product_quote",
        name: String(data.name || ""),
        email: String(data.email || ""),
        phone: data.phone ? String(data.phone) : null,
        message: data.message ? String(data.message) : null,
        product: data.product ? String(data.product) : null,
        partNumber: data.partNumber ? String(data.partNumber) : null,
        quantity: data.quantity ? String(data.quantity) : null,
      })

      setQuoteFormState("success")
    } catch (error) {
      console.error("Quote form submission error:", error)
      setQuoteFormState("error")
    }
  }

  if (loading) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <div className="grid gap-12 lg:grid-cols-2">
            <div className="space-y-4">
              <Skeleton className="aspect-square w-full" />
              <div className="grid grid-cols-4 gap-4">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="aspect-square" />
                ))}
              </div>
            </div>
            <div className="space-y-6">
              <Skeleton className="h-12 w-3/4" />
              <Skeleton className="h-6 w-1/2" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          </div>
        </Container>
      </section>
    )
  }

  if (error || !product) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card className="border-destructive">
            <CardContent className="p-12 text-center">
              <h2 className="mb-4 text-2xl font-bold">Product Not Found</h2>
              <p className="mb-6 text-muted-foreground">
                {error || "The product you're looking for doesn't exist or has been removed."}
              </p>
              <Button asChild>
                <Link href="/shop">
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Shop Home
                </Link>
              </Button>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  const availabilityBadge = getAvailabilityBadge(product.availability)
  const displayName = selectedVariant ? `${product.name} — ${selectedVariant.name}` : product.name
  const displaySku = selectedVariant?.sku || product.sku
  const quoteProductName = selectedVariant ? `${product.name} - ${selectedVariant.name}` : product.name
  const infoSections = product.infoSections || []
  const isQuoteOnly = product.quoteOnly === true
  const normalizedProductType = product.productType?.toLowerCase()
  const isCarProduct = normalizedProductType === "car"

  const topicBackTarget = (() => {
    switch (normalizedProductType) {
      case "car":
        return { href: "/shop/cars", label: "Back to Cars" }
      case "part":
        return { href: "/shop/parts", label: "Back to Parts" }
      case "tool":
        return { href: "/shop/tools", label: "Back to Tools" }
      case "custom":
        return { href: "/shop/custom", label: "Back to Custom" }
      default:
        return { href: "/shop", label: "Shop Home" }
    }
  })()

  const formatEnumValue = (value?: string) => {
    if (!value) return undefined
    return value
      .split("_")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ")
  }

  const hasValue = (value: unknown) => {
    if (Array.isArray(value)) return value.length > 0
    return value !== undefined && value !== null && value !== ""
  }

  const hasVehicleTab =
    hasValue(product.compatibilityMode) ||
    hasValue(product.compatibleMakes) ||
    hasValue(product.compatibleModels) ||
    hasValue(product.compatibleYearStart) ||
    hasValue(product.compatibleYearEnd) ||
    hasValue(product.vinCompatible)

  const hasPartsTab =
    normalizedProductType === "part" ||
    hasValue(product.partCategory) ||
    hasValue(product.partNumberRaw) ||
    hasValue(product.partPosition) ||
    hasValue(product.material) ||
    hasValue(product.reconditioned)

  const hasToolsTab =
    normalizedProductType === "tool" ||
    hasValue(product.toolCategory) ||
    hasValue(product.powerSource) ||
    hasValue(product.voltage) ||
    hasValue(product.torqueMinNm) ||
    hasValue(product.torqueMaxNm) ||
    hasValue(product.driveSize) ||
    hasValue(product.professionalGrade) ||
    hasValue(product.isKit)

  const hasCustomTab =
    normalizedProductType === "custom" ||
    hasValue(product.customCategory) ||
    hasValue(product.styleTags) ||
    hasValue(product.finish) ||
    hasValue(product.streetLegal) ||
    hasValue(product.installationDifficulty)

  const carDetailsText = (() => {
    if (!isCarProduct) return ""
    const lines: string[] = []
    if (product.year !== undefined) lines.push(`Year: ${product.year}`)
    if (product.make) lines.push(`Make: ${product.make}`)
    if (product.model) lines.push(`Model: ${product.model}`)
    if (product.mileage !== undefined) {
      lines.push(`Mileage: ${new Intl.NumberFormat("en-US").format(product.mileage)} km`)
    }
    if (product.powerKw !== undefined) lines.push(`Power: ${product.powerKw} kW`)
    if (product.fuelType) lines.push(`Fuel Type: ${formatEnumValue(product.fuelType)}`)
    if (product.transmission) lines.push(`Transmission: ${formatEnumValue(product.transmission)}`)
    if (product.driveType) lines.push(`Drive Type: ${formatEnumValue(product.driveType)}`)
    if (product.color) lines.push(`Color: ${product.color}`)
    if (product.warrantyIncluded !== undefined) lines.push(`Warranty: ${product.warrantyIncluded ? "Included" : "Not included"}`)
    return lines.join("\n")
  })()

  const compatibilityYearRange =
    hasValue(product.compatibleYearStart) || hasValue(product.compatibleYearEnd)
      ? `${product.compatibleYearStart ?? "Any"} - ${product.compatibleYearEnd ?? "Any"}`
      : undefined

  const toolTorqueRange =
    hasValue(product.torqueMinNm) || hasValue(product.torqueMaxNm)
      ? `${product.torqueMinNm ?? "Any"} - ${product.torqueMaxNm ?? "Any"} Nm`
      : undefined

  return (
    <section className="py-24 lg:py-32">
      <Container>
        {/* Breadcrumb */}
        <div className="mb-8">
          <Link
            href={topicBackTarget.href}
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            {topicBackTarget.label}
          </Link>
        </div>

        <div className="grid gap-12 lg:grid-cols-2">
          {/* Images */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-4"
          >
            {/* Main Image */}
            <div className="relative aspect-square overflow-hidden rounded-lg bg-muted">
              {product.images && product.images.length > 0 ? (
                <Image
                  src={getImageUrl(product.images[selectedImage] || product.images[0])}
                  alt={product.name}
                  fill
                  className="object-cover"
                  priority
                />
              ) : (
                <div className="flex h-full items-center justify-center">
                  <Package className="h-24 w-24 text-muted-foreground" />
                </div>
              )}
            </div>

            {/* Thumbnail Gallery */}
            {product.images && product.images.length > 1 && (
              <div className="grid grid-cols-4 gap-4">
                {product.images.map((image, index) => (
                  <button
                    key={index}
                    onClick={() => setSelectedImage(index)}
                    className={`relative aspect-square overflow-hidden rounded-lg border-2 transition-all ${selectedImage === index
                      ? 'border-primary ring-2 ring-primary/20'
                      : 'border-transparent hover:border-primary/50'
                      }`}
                  >
                    <Image
                      src={getImageUrl(image)}
                      alt={`${product.name} view ${index + 1}`}
                      fill
                      className="object-cover"
                    />
                  </button>
                ))}
              </div>
            )}

          </motion.div>

          {/* Product Info */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-6"
          >
            <div>
              <Badge variant={availabilityBadge.variant} className="mb-3">
                {availabilityBadge.label}
              </Badge>
              <h1 className="mb-3 text-3xl font-bold lg:text-4xl">{displayName}</h1>
              {displaySku && (
                <p className="text-sm text-muted-foreground">
                  SKU: <span className="font-mono font-medium">{displaySku}</span>
                </p>
              )}
              {product.partNumber && (
                <p className="text-sm text-muted-foreground">
                  Part Number: <span className="font-mono font-medium">{product.partNumber}</span>
                </p>
              )}
              {product.manufacturer && (
                <p className="text-sm text-muted-foreground">
                  Manufacturer: {product.manufacturer}
                </p>
              )}
            </div>

            {product.shortDescription && (
              <p className="text-lg text-muted-foreground leading-relaxed">
                {product.shortDescription}
              </p>
            )}

            <Separator />

            {/* Price and Lead Time */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-3xl font-bold">
                  {formatCurrency(selectedVariant?.price || product.price, product.currency)}
                </p>
                {product.minQuantity && product.minQuantity > 1 && (
                  <p className="text-sm text-muted-foreground">
                    Min. quantity: {product.minQuantity}
                  </p>
                )}
              </div>
              {product.leadTime && (
                <div className="text-right">
                  <p className="text-sm font-medium">Lead Time</p>
                  <p className="text-sm text-muted-foreground">{product.leadTime}</p>
                </div>
              )}
            </div>

            <Separator />

            {/* Variant Selector */}
            {product.variants && product.variants.length > 0 && (
              <>
                <div>
                  <label className="mb-3 block text-sm font-medium">Select Variant</label>
                  <div className="grid gap-2">
                    {product.variants
                      .filter(v => v.active)
                      .sort((a, b) => a.position - b.position)
                      .map((variant) => (
                        <Button
                          key={variant.id}
                          variant={selectedVariant?.id === variant.id ? "default" : "outline"}
                          className="justify-start h-auto py-3 px-4 cursor-pointer"
                          onClick={() => handleVariantSelect(variant)}
                        >
                          <div className="flex flex-col items-start w-full">
                            <span className="font-medium">{variant.name}</span>
                            <div className="flex items-center gap-2 text-xs text-muted-foreground mt-1">
                              {variant.price && (
                                <span className="font-semibold text-foreground">
                                  {formatCurrency(variant.price, product.currency)}
                                </span>
                              )}
                              <span>•</span>
                              <span>Stock: {variant.stockQuantity}</span>
                              {Object.keys(variant.options).length > 0 && (
                                <>
                                  <span>•</span>
                                  <span>
                                    {Object.entries(variant.options).map(([key, value]) => `${key}: ${value}`).join(', ')}
                                  </span>
                                </>
                              )}
                            </div>
                          </div>
                        </Button>
                      ))}
                  </div>
                </div>

                <Separator />
              </>
            )}

            {/* Quantity Selector */}
            <div>
              <label className="mb-2 block text-sm font-medium">Quantity</label>
              <div className="flex items-center gap-4">
                <div className="flex items-center">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => setQuantity((q) => Math.max(product.minQuantity || 1, q - 1))}
                    disabled={quantity <= (product.minQuantity || 1)}
                  >
                    <Minus className="h-4 w-4" />
                  </Button>
                  <span className="w-16 text-center font-semibold">{quantity}</span>
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => setQuantity((q) => q + 1)}
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                <p className="text-sm text-muted-foreground">
                  Total: {formatCurrency((selectedVariant?.price || product.price) * quantity, product.currency)}
                </p>
              </div>
            </div>

            {/* Actions */}
            <div className="space-y-3">
              {!isQuoteOnly ? (
                <>
                  <Button
                    onClick={handleAddToCart}
                    disabled={product.availability === 'out_of_stock'}
                    size="lg"
                    className="w-full"
                  >
                    <ShoppingCart className="mr-2 h-5 w-5" />
                    Add to Cart
                  </Button>
                  <Button
                    onClick={handleRequestQuote}
                    variant="outline"
                    size="lg"
                    className="w-full"
                  >
                    <FileText className="mr-2 h-5 w-5" />
                    Request Quote for This Item
                  </Button>
                </>
              ) : (
                <Button
                  onClick={handleRequestQuote}
                  size="lg"
                  className="w-full"
                >
                  <FileText className="mr-2 h-5 w-5" />
                  Request Quote for This Item
                </Button>
              )}
            </div>

            {/* Trust Badges */}
            <Card>
              <CardContent className="grid gap-4 p-6 sm:grid-cols-3">
                <div className="flex items-start gap-3">
                  <Shield className="h-5 w-5 text-primary" />
                  <div>
                    <p className="text-sm font-medium">Quality Guaranteed</p>
                    <p className="text-xs text-muted-foreground">Verified suppliers only</p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <Truck className="h-5 w-5 text-primary" />
                  <div>
                    <p className="text-sm font-medium">Global Shipping</p>
                    <p className="text-xs text-muted-foreground">Tracked delivery</p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <Package className="h-5 w-5 text-primary" />
                  <div>
                    <p className="text-sm font-medium">Secure Packaging</p>
                    <p className="text-xs text-muted-foreground">Safe arrival</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>

        {/* Product Details Tabs */}
        <div className="mt-16">
          <Tabs defaultValue="description" className="w-full">
            <TabsList className="inline-flex w-fit max-w-full flex-wrap gap-2">
              <TabsTrigger className="!flex-none min-w-[120px] px-4" value="description">Description</TabsTrigger>
              {hasVehicleTab && <TabsTrigger className="!flex-none min-w-[120px] px-4" value="vehicle">Vehicle</TabsTrigger>}
              {hasPartsTab && <TabsTrigger className="!flex-none min-w-[120px] px-4" value="parts">Parts</TabsTrigger>}
              {hasToolsTab && <TabsTrigger className="!flex-none min-w-[120px] px-4" value="tools">Tools</TabsTrigger>}
              {hasCustomTab && <TabsTrigger className="!flex-none min-w-[120px] px-4" value="custom">Custom</TabsTrigger>}
              {infoSections.map((section, index) => (
                <TabsTrigger className="!flex-none min-w-[120px] px-4" key={`${section.title}-${index}`} value={`info-${index}`}>
                  {section.title}
                </TabsTrigger>
              ))}
            </TabsList>

            <TabsContent value="description" className="mt-6">
              <Card>
                <CardContent className="p-6">
                  {(product.brand || product.weight !== undefined || product.productType || product.condition || product.oemType) && (
                    <div className="mb-4 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                      {product.brand && (
                        <div>
                          <span className="font-medium text-foreground">Brand:</span> {product.brand}
                        </div>
                      )}
                      {product.productType && (
                        <div>
                          <span className="font-medium text-foreground">Product Type:</span> {formatEnumValue(product.productType)}
                        </div>
                      )}
                      {product.condition && (
                        <div>
                          <span className="font-medium text-foreground">Condition:</span> {formatEnumValue(product.condition)}
                        </div>
                      )}
                      {product.oemType && (
                        <div>
                          <span className="font-medium text-foreground">OEM Type:</span> {formatEnumValue(product.oemType)}
                        </div>
                      )}
                      {product.weight !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">Weight:</span> {product.weight} kg
                        </div>
                      )}
                    </div>
                  )}

                  <div className="prose prose-sm max-w-none dark:prose-invert">
                    {carDetailsText && <p className="leading-relaxed whitespace-pre-line">{carDetailsText}</p>}
                    {product.description ? (
                      <p className="leading-relaxed whitespace-pre-line">{product.description}</p>
                    ) : !carDetailsText ? (
                      <p className="text-muted-foreground">No description available.</p>
                    ) : null}
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {hasVehicleTab && (
              <TabsContent value="vehicle" className="mt-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                      {product.compatibilityMode && (
                        <div>
                          <span className="font-medium text-foreground">Compatibility Mode:</span> {formatEnumValue(product.compatibilityMode)}
                        </div>
                      )}
                      {product.compatibleMakes && product.compatibleMakes.length > 0 && (
                        <div>
                          <span className="font-medium text-foreground">Compatible Makes:</span> {product.compatibleMakes.join(", ")}
                        </div>
                      )}
                      {product.compatibleModels && product.compatibleModels.length > 0 && (
                        <div>
                          <span className="font-medium text-foreground">Compatible Models:</span> {product.compatibleModels.join(", ")}
                        </div>
                      )}
                      {compatibilityYearRange && (
                        <div>
                          <span className="font-medium text-foreground">Compatible Years:</span> {compatibilityYearRange}
                        </div>
                      )}
                      {product.vinCompatible !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">VIN Compatible:</span> {product.vinCompatible ? "Yes" : "No"}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {hasPartsTab && (
              <TabsContent value="parts" className="mt-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                      {product.partCategory && (
                        <div>
                          <span className="font-medium text-foreground">Part Category:</span> {product.partCategory}
                        </div>
                      )}
                      {(product.partNumberRaw || product.partNumber) && (
                        <div>
                          <span className="font-medium text-foreground">Part Number:</span> {product.partNumberRaw || product.partNumber}
                        </div>
                      )}
                      {product.partPosition && product.partPosition.length > 0 && (
                        <div>
                          <span className="font-medium text-foreground">Position:</span> {product.partPosition.join(", ")}
                        </div>
                      )}
                      {product.material && (
                        <div>
                          <span className="font-medium text-foreground">Material:</span> {product.material}
                        </div>
                      )}
                      {product.reconditioned !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">Reconditioned:</span> {product.reconditioned ? "Yes" : "No"}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {hasToolsTab && (
              <TabsContent value="tools" className="mt-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                      {product.toolCategory && (
                        <div>
                          <span className="font-medium text-foreground">Tool Category:</span> {product.toolCategory}
                        </div>
                      )}
                      {product.powerSource && (
                        <div>
                          <span className="font-medium text-foreground">Power Source:</span> {formatEnumValue(product.powerSource)}
                        </div>
                      )}
                      {product.voltage !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">Voltage:</span> {product.voltage} V
                        </div>
                      )}
                      {toolTorqueRange && (
                        <div>
                          <span className="font-medium text-foreground">Torque Range:</span> {toolTorqueRange}
                        </div>
                      )}
                      {product.driveSize && (
                        <div>
                          <span className="font-medium text-foreground">Drive Size:</span> {product.driveSize}
                        </div>
                      )}
                      {product.professionalGrade !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">Professional Grade:</span> {product.professionalGrade ? "Yes" : "No"}
                        </div>
                      )}
                      {product.isKit !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">Kit:</span> {product.isKit ? "Yes" : "No"}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {hasCustomTab && (
              <TabsContent value="custom" className="mt-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                      {product.customCategory && (
                        <div>
                          <span className="font-medium text-foreground">Custom Category:</span> {product.customCategory}
                        </div>
                      )}
                      {product.styleTags && product.styleTags.length > 0 && (
                        <div>
                          <span className="font-medium text-foreground">Style Tags:</span> {product.styleTags.join(", ")}
                        </div>
                      )}
                      {product.finish && (
                        <div>
                          <span className="font-medium text-foreground">Finish:</span> {product.finish}
                        </div>
                      )}
                      {product.streetLegal !== undefined && (
                        <div>
                          <span className="font-medium text-foreground">Street Legal:</span> {product.streetLegal ? "Yes" : "No"}
                        </div>
                      )}
                      {product.installationDifficulty && (
                        <div>
                          <span className="font-medium text-foreground">Installation Difficulty:</span> {formatEnumValue(product.installationDifficulty)}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {infoSections.map((section, index) => (
              <TabsContent key={`${section.title}-${index}`} value={`info-${index}`} className="mt-6">
                <Card>
                  <CardContent className="p-6">
                    <div className="prose prose-sm max-w-none dark:prose-invert">
                      <p className="leading-relaxed whitespace-pre-line">{section.content}</p>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            ))}
          </Tabs>
        </div>

        {/* CTA Section */}
        <Card className="mt-16 border-primary/20 bg-primary/5">
          <CardContent className="p-8 text-center">
            <h3 className="mb-2 text-2xl font-bold">Need Help Choosing?</h3>
            <p className="mb-6 text-muted-foreground">
              Our team can verify compatibility, check specifications, and provide technical guidance
            </p>
            <Button asChild size="lg">
              <Link href="/#contact">Contact Our Team</Link>
            </Button>
          </CardContent>
        </Card>
      </Container>

      {/* Quote Request Dialog */}
      <Dialog open={showQuoteDialog} onOpenChange={setShowQuoteDialog}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Request a Quote</DialogTitle>
            <DialogDescription>
              {product && `Get a custom quote for ${quoteProductName}`}
            </DialogDescription>
          </DialogHeader>

          {quoteFormState === "success" ? (
            <div className="text-center py-6">
              <CheckCircle2 className="mx-auto mb-4 h-16 w-16 text-primary" />
              <h3 className="mb-3 text-xl font-bold">Quote Request Received</h3>
              <p className="mb-6 text-muted-foreground">
                Your quote details were prepared for WhatsApp and sent to our team channel.
              </p>
              <Button onClick={() => setShowQuoteDialog(false)}>Close</Button>
            </div>
          ) : (
            <form onSubmit={handleQuoteSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="quote-name">
                  Full Name <span className="text-destructive">*</span>
                </Label>
                <Input id="quote-name" name="name" required placeholder="John Smith" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="quote-email">
                  Email Address <span className="text-destructive">*</span>
                </Label>
                <Input id="quote-email" name="email" type="email" required placeholder="john@example.com" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="quote-phone">Phone Number</Label>
                <Input id="quote-phone" name="phone" type="tel" placeholder="+1 (555) 123-4567" />
              </div>

              <div className="space-y-2">
                <Label htmlFor="quote-message">Additional Details</Label>
                <Textarea
                  id="quote-message"
                  name="message"
                  placeholder="Any specific requirements or questions..."
                  rows={4}
                />
              </div>

              <div className="rounded-lg border border-white/15 bg-gradient-to-br from-slate-900/85 via-slate-800/75 to-slate-900/85 p-3 text-sm shadow-sm backdrop-blur-sm">
                <p className="font-medium">Quote Details:</p>
                <p className="text-slate-200">Product: {quoteProductName}</p>
                <p className="text-slate-200">
                  Price: {formatCurrency(selectedVariant?.price || product.price, product.currency)}
                </p>
                {displaySku && (
                  <p className="text-slate-200">SKU: {displaySku}</p>
                )}
                <p className="text-slate-200">Quantity: {quantity}</p>
              </div>

              {quoteFormState === "error" && (
                <p className="text-sm text-destructive">
                  There was an error submitting your request. Please try again.
                </p>
              )}

              <div className="flex gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setShowQuoteDialog(false)}
                  className="flex-1"
                  disabled={quoteFormState === "loading"}
                >
                  Cancel
                </Button>
                <Button type="submit" className="flex-1" disabled={quoteFormState === "loading"}>
                  {quoteFormState === "loading" ? "Submitting..." : "Submit Request"}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </section>
  )
}
