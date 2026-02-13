"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { TopicProductListing } from "@/components/topic-product-listing"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  fetchCategoryBySlug,
  fetchCategoryChildren,
  TOPIC_INFO,
  type Category,
  type ShopTopic
} from "@/lib/shopApi"
import { getImageUrl } from "@/lib/utils"
import Image from "next/image"
import Link from "next/link"
import { motion } from "framer-motion"
import { ArrowLeft } from "lucide-react"

interface TopicPageProps {
  topic: ShopTopic
}

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

function getCategoryDisplayName(topic: string, category: Category): string {
  if (topic !== "cars") {
    return category.name
  }

  return CARS_CATEGORY_DISPLAY_NAMES[category.slug] ?? category.name
}

export default function TopicPage({ topic }: TopicPageProps) {
  const [rootCategory, setRootCategory] = useState<Category | null>(null)
  const [subcategories, setSubcategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const topicInfo = TOPIC_INFO[topic]

  // Fetch root category and its children
  useEffect(() => {
    async function loadData() {
      setLoading(true)
      setError(null)
      try {
        const category = await fetchCategoryBySlug(topic)
        if (!category) {
          setError(`Topic "${topic}" not found. Please run the database migration.`)
          setLoading(false)
          return
        }
        setRootCategory(category)

        const children = await fetchCategoryChildren(category.id)
        setSubcategories(children)
        setLoading(false)
      } catch (err: any) {
        setError(err.message || 'Failed to load topic')
        setLoading(false)
      }
    }
    loadData()
  }, [topic])

  if (loading) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <div className="space-y-8">
            <div className="flex items-center gap-4">
              <Skeleton className="h-8 w-32" />
            </div>
            <Skeleton className="h-12 w-64" />
            <div className="flex gap-4 overflow-hidden">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-32 w-48 flex-shrink-0 rounded-xl" />
              ))}
            </div>
            <div className="grid gap-6 lg:grid-cols-[256px_1fr]">
              <Skeleton className="h-96 w-full rounded-lg" />
              <div className="space-y-4">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-64 w-full rounded-lg" />
                ))}
              </div>
            </div>
          </div>
        </Container>
      </section>
    )
  }

  if (error) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card className="border-destructive">
            <CardContent className="p-8 text-center">
              <p className="mb-4 text-destructive">{error}</p>
              <Link href="/shop" className="text-sm text-muted-foreground hover:text-primary">
                ← Shop Home
              </Link>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  if (!rootCategory) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card>
            <CardContent className="p-8 text-center">
              <p className="mb-4">Topic not found</p>
              <Link href="/shop" className="text-sm text-muted-foreground hover:text-primary">
                ← Shop Home
              </Link>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        {/* Back Link */}
        <Link
          href="/shop"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-primary mb-6"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Shop Home
        </Link>

        {/* Page Title */}
        <SectionHeading
          title={topicInfo.label}
          subtitle={topicInfo.description}
          className="mb-8"
        />

        {/* Subcategories Row (Image Row) */}
        {subcategories.length > 0 && (
          <div className="mb-12">
            <h3 className="text-lg font-semibold mb-4">Browse by Category</h3>
            <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-muted scrollbar-track-transparent">
              {subcategories.map((subcat, i) => (
                <SubcategoryTile
                  key={subcat.id}
                  category={subcat}
                  topic={topic}
                  index={i}
                />
              ))}
            </div>
          </div>
        )}

        {/* Product Listing */}
        <TopicProductListing
          rootCategoryId={rootCategory.id}
          topicSlug={topic}
          topicName={topicInfo.label}
          topicDescription={topicInfo.description}
          showBackLink={false}
        />
      </Container>
    </section>
  )
}

interface SubcategoryTileProps {
  category: Category
  topic: string
  index: number
}

function SubcategoryTile({ category, topic, index }: SubcategoryTileProps) {
  // Default image if category doesn't have one
  const imageUrl = category.imageUrl || `/images/categories/${category.slug}.jpg`
  const displayName = getCategoryDisplayName(topic, category)

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.05, duration: 0.3 }}
      className="flex-shrink-0"
    >
      <Link
        href={`/shop/${topic}/${category.slug}`}
        className="group block relative w-48 h-32 overflow-hidden rounded-xl bg-muted"
      >
        {/* Background Image */}
        <div className="absolute inset-0">
          <Image
            src={imageUrl}
            alt={displayName}
            fill
            className="object-cover transition-transform duration-300 group-hover:scale-110"
            sizes="192px"
            onError={(e) => {
              // Fallback if image doesn't exist
              e.currentTarget.src = '/images/placeholder-category.jpg'
            }}
          />
          <div className="absolute inset-0 bg-black/30 transition-colors duration-300 group-hover:bg-black/40" />
        </div>

        {/* Label */}
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-white font-semibold text-center px-2 drop-shadow-md">
            {displayName}
          </span>
        </div>

        {/* Product count badge */}
        {category.productCount !== undefined && category.productCount > 0 && (
          <div className="absolute bottom-2 right-2 bg-primary/80 text-primary-foreground text-xs px-2 py-0.5 rounded-full">
            {category.productCount}
          </div>
        )}
      </Link>
    </motion.div>
  )
}
