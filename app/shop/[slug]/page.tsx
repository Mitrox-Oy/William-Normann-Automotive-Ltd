"use client"

import { isValidTopic, type ShopTopic } from "@/lib/shopApi"
import { useParams } from "next/navigation"
import TopicPage from "./topic-page"
import ProductDetailPage from "./product-detail-page"

/**
 * Unified handler for /shop/[slug]
 * - If slug is a valid topic (cars, parts, tools, custom), render TopicPage
 * - Otherwise, treat as a product slug and render ProductDetailPage
 */
export default function ShopSlugPage() {
  const params = useParams()
  const slug = params.slug as string

  // Check if this is a topic
  if (isValidTopic(slug)) {
    return <TopicPage topic={slug as ShopTopic} />
  }

  // Otherwise treat as product slug
  return <ProductDetailPage slug={slug} />
}