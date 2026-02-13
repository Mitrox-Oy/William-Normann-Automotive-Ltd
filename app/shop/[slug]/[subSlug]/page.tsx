"use client"

import { useState, useEffect } from "react"
import { useParams } from "next/navigation"
import { Container } from "@/components/container"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ArrowLeft } from "lucide-react"
import Link from "next/link"
import {
  isValidTopic,
  fetchCategoryBySlug,
  TOPIC_INFO,
  type ShopTopic,
  type Category
} from "@/lib/shopApi"
import { TopicProductListing } from "@/components/topic-product-listing"

const CARS_CATEGORY_DISPLAY_NAMES: Record<string, string> = {
  "cars-bmw": "JDM",
  "cars-jdm": "JDM",
  "cars-audi": "Euro",
  "cars-euro": "Euro",
  "cars-mercedes": "Luxury",
  "cars-luxury": "Luxury",
  "cars-porsche": "Super Cars",
  "cars-super-cars": "Super Cars",
}

function getSubcategoryDisplayName(topicSlug: string, subcategory: Category): string {
  if (topicSlug !== "cars") {
    return subcategory.name
  }

  return CARS_CATEGORY_DISPLAY_NAMES[subcategory.slug] ?? subcategory.name
}

/**
 * Subcategory page for /shop/[slug]/[subSlug]
 * - First validates that [slug] is a valid topic
 * - Then fetches the subcategory by [subSlug]
 * - Renders a filtered product listing for that subcategory
 */
export default function TopicSubcategoryPage() {
  const params = useParams()
  const topicSlug = params.slug as string
  const subSlug = params.subSlug as string

  const [subcategory, setSubcategory] = useState<Category | null>(null)
  const [rootCategory, setRootCategory] = useState<Category | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Check if this is a valid topic
  const isValidTopicRoute = isValidTopic(topicSlug)
  const topic = topicSlug as ShopTopic
  const topicInfo = isValidTopicRoute ? TOPIC_INFO[topic] : null

  useEffect(() => {
    if (!isValidTopicRoute) {
      // Not a valid topic - this will show a 404-like page
      setLoading(false)
      setError("Invalid topic")
      return
    }

    const loadData = async () => {
      setLoading(true)
      setError(null)

      try {
        // First fetch the root category for this topic
        const rootCat = await fetchCategoryBySlug(topicSlug)
        if (!rootCat) {
          setError(`Topic "${topicSlug}" not found`)
          setLoading(false)
          return
        }
        setRootCategory(rootCat)

        // Then fetch the subcategory
        const subCat = await fetchCategoryBySlug(subSlug)
        if (!subCat) {
          setError(`Category "${subSlug}" not found`)
          setLoading(false)
          return
        }

        // Validate that this subcategory belongs to this topic
        // For simplicity we check if it has the root as ancestor (or we trust the routing)
        setSubcategory(subCat)
        setLoading(false)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load category")
        setLoading(false)
      }
    }

    loadData()
  }, [topicSlug, subSlug, isValidTopicRoute])

  // Loading state
  if (loading) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <div className="space-y-8">
            <div className="space-y-4">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-12 w-96" />
            </div>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="aspect-[4/5] rounded-lg" />
              ))}
            </div>
          </div>
        </Container>
      </section>
    )
  }

  // Error / not found state
  if (error || !subcategory || !rootCategory) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card className="border-destructive">
            <CardContent className="p-12 text-center">
              <h2 className="mb-4 text-2xl font-bold">Not Found</h2>
              <p className="mb-6 text-muted-foreground">
                {error || "The page you're looking for doesn't exist."}
              </p>
              <div className="flex flex-col gap-4 sm:flex-row sm:justify-center">
                <Button asChild>
                  <Link href="/shop">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Shop Home
                  </Link>
                </Button>
                {isValidTopicRoute && topicInfo && (
                  <Button asChild variant="outline">
                    <Link href={`/shop/${topicSlug}`}>
                      Back to {topicInfo.label}
                    </Link>
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  const subcategoryDisplayName = getSubcategoryDisplayName(topicSlug, subcategory)

  return (
    <section className="py-16 lg:py-24">
      <Container>
        {/* Breadcrumbs */}
        <nav className="mb-8 text-sm text-muted-foreground">
          <ol className="flex items-center gap-2">
            <li>
              <Link href="/shop" className="hover:text-foreground transition-colors">
                SHOP
              </Link>
            </li>
            <li>/</li>
            <li>
              <Link href={`/shop/${topicSlug}`} className="hover:text-foreground transition-colors">
                {topicInfo?.label || topicSlug}
              </Link>
            </li>
            <li>/</li>
            <li className="text-foreground font-medium">
              {subcategoryDisplayName}
            </li>
          </ol>
        </nav>

        {/* Category Header */}
        <div className="mb-12 space-y-4">
          <h1 className="text-4xl font-bold lg:text-5xl">
            {subcategoryDisplayName}
          </h1>
          {subcategory.description && (
            <p className="text-lg text-muted-foreground max-w-2xl">
              {subcategory.description}
            </p>
          )}
        </div>

        {/* Product Listing scoped to this subcategory */}
        <TopicProductListing
          rootCategoryId={rootCategory.id}
          topicSlug={topicSlug}
          defaultCategoryId={subcategory.id}
          defaultCategorySlug={subcategory.slug}
          showBackLink={false}
        />
      </Container>
    </section>
  )
}
