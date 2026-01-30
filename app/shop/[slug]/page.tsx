"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useCart } from "@/components/CartContext"
import { 
  fetchProductBySlug, 
  formatCurrency, 
  getAvailabilityBadge,
  type Product 
} from "@/lib/shopApi"
import { ShoppingCart, Minus, Plus, ArrowLeft, Package, Truck, Shield, FileText } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { useParams, useRouter } from "next/navigation"
import { motion } from "framer-motion"

export default function ProductDetailPage() {
  const params = useParams()
  const router = useRouter()
  const slug = params.slug as string
  
  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedImage, setSelectedImage] = useState(0)
  const [quantity, setQuantity] = useState(1)
  
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
        }
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message || 'Failed to load product')
        setProduct(null)
        setLoading(false)
      })
  }, [slug])

  const handleAddToCart = () => {
    if (product) {
      addItem(product, quantity)
    }
  }

  const handleRequestQuote = () => {
    if (product) {
      const queryParams = new URLSearchParams({
        product: product.name,
        part_number: product.partNumber || '',
        quantity: quantity.toString(),
      })
      router.push(`/#contact?${queryParams.toString()}`)
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
                  Back to Shop
                </Link>
              </Button>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  const availabilityBadge = getAvailabilityBadge(product.availability)

  return (
    <section className="py-24 lg:py-32">
      <Container>
        {/* Breadcrumb */}
        <div className="mb-8">
          <Link 
            href="/shop" 
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Shop
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
                  src={product.images[selectedImage] || product.images[0]}
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
                    className={`relative aspect-square overflow-hidden rounded-lg border-2 transition-all ${
                      selectedImage === index 
                        ? 'border-primary ring-2 ring-primary/20' 
                        : 'border-transparent hover:border-primary/50'
                    }`}
                  >
                    <Image
                      src={image}
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
              <h1 className="mb-3 text-3xl font-bold lg:text-4xl">{product.name}</h1>
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
                  {formatCurrency(product.price, product.currency)}
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
                  Total: {formatCurrency(product.price * quantity, product.currency)}
                </p>
              </div>
            </div>

            {/* Actions */}
            <div className="space-y-3">
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
            <TabsList className="grid w-full grid-cols-3 lg:w-[400px]">
              <TabsTrigger value="description">Description</TabsTrigger>
              <TabsTrigger value="specifications">Specifications</TabsTrigger>
              <TabsTrigger value="compatibility">Compatibility</TabsTrigger>
            </TabsList>

            <TabsContent value="description" className="mt-6">
              <Card>
                <CardContent className="p-6">
                  <div className="prose prose-sm max-w-none dark:prose-invert">
                    {product.description ? (
                      <p className="leading-relaxed whitespace-pre-line">{product.description}</p>
                    ) : (
                      <p className="text-muted-foreground">No description available.</p>
                    )}
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="specifications" className="mt-6">
              <Card>
                <CardContent className="p-6">
                  {product.specifications && Object.keys(product.specifications).length > 0 ? (
                    <dl className="grid gap-4 sm:grid-cols-2">
                      {Object.entries(product.specifications).map(([key, value]) => (
                        <div key={key} className="border-b pb-3">
                          <dt className="text-sm font-medium text-muted-foreground">{key}</dt>
                          <dd className="mt-1 text-sm font-semibold">{value}</dd>
                        </div>
                      ))}
                    </dl>
                  ) : (
                    <p className="text-muted-foreground">No specifications available.</p>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="compatibility" className="mt-6">
              <Card>
                <CardContent className="p-6">
                  {product.compatibleVehicles && product.compatibleVehicles.length > 0 ? (
                    <div>
                      <p className="mb-4 text-sm text-muted-foreground">
                        This part is compatible with the following vehicles:
                      </p>
                      <ul className="grid gap-2 sm:grid-cols-2">
                        {product.compatibleVehicles.map((vehicle, index) => (
                          <li key={index} className="flex items-center gap-2 text-sm">
                            <div className="h-1.5 w-1.5 rounded-full bg-primary" />
                            {vehicle}
                          </li>
                        ))}
                      </ul>
                    </div>
                  ) : (
                    <div>
                      <p className="mb-4 text-muted-foreground">
                        Compatibility information not available. 
                      </p>
                      <p className="text-sm text-muted-foreground">
                        Please use the "Request Quote" button to verify fitment for your specific vehicle.
                      </p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
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
    </section>
  )
}

