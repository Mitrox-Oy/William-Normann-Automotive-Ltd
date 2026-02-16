import { siteConfig } from "@/content/site"
import type { MetadataRoute } from "next"
import { fetchProducts, fetchCategories, SHOP_TOPICS } from "@/lib/shopApi"
import { SITE_URL } from "@/lib/seo/config"

/**
 * Dynamic sitemap — fetches all products and categories from the backend.
 * Revalidated on each deploy / on-demand; never force-static.
 */
export const dynamic = "force-dynamic"
export const revalidate = 3600 // regenerate at most once per hour

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = SITE_URL

  // ── Static pages ──────────────────────────────────────────────────────
  const staticRoutes: MetadataRoute.Sitemap = [
    "", "/shop", "/about", "/gallery", "/services", "/blog", "/privacy", "/terms",
  ].map((route) => ({
    url: `${baseUrl}${route}`,
    lastModified: new Date(),
    changeFrequency: "weekly" as const,
    priority: route === "" ? 1 : 0.8,
  }))

  // ── Blog posts (hardcoded in siteConfig) ──────────────────────────────
  const blogPosts: MetadataRoute.Sitemap = siteConfig.blog.posts.map((post) => ({
    url: `${baseUrl}/blog/${post.slug}`,
    lastModified: new Date(post.date),
    changeFrequency: "monthly" as const,
    priority: 0.6,
  }))

  // ── Topic (root category) pages ───────────────────────────────────────
  const topicRoutes: MetadataRoute.Sitemap = SHOP_TOPICS.map((topic) => ({
    url: `${baseUrl}/shop/${topic}`,
    lastModified: new Date(),
    changeFrequency: "daily" as const,
    priority: 0.9,
  }))

  // ── Category pages ────────────────────────────────────────────────────
  let categoryRoutes: MetadataRoute.Sitemap = []
  try {
    const categories = await fetchCategories()
    // Build a lookup of parent slug by id
    const idToSlug = new Map<number, string>()
    for (const cat of categories) {
      idToSlug.set(cat.id, cat.slug)
    }

    categoryRoutes = categories
      .filter((cat) => cat.parentId) // only child categories
      .map((cat) => {
        const parentSlug = cat.parentId ? idToSlug.get(cat.parentId) : undefined
        const path = parentSlug
          ? `/shop/${parentSlug}/${cat.slug}`
          : `/shop/${cat.slug}`
        return {
          url: `${baseUrl}${path}`,
          lastModified: new Date(),
          changeFrequency: "daily" as const,
          priority: 0.7,
        }
      })
  } catch {
    // Backend unreachable — emit only static routes
  }

  // ── Product pages ─────────────────────────────────────────────────────
  let productRoutes: MetadataRoute.Sitemap = []
  try {
    // Fetch in pages of 200 to avoid timeout
    let page = 1
    const limit = 200
    let hasMore = true

    while (hasMore) {
      const res = await fetchProducts({ page, limit })
      for (const p of res.products) {
        productRoutes.push({
          url: `${baseUrl}/shop/${p.slug}`,
          lastModified: p.updatedAt ? new Date(p.updatedAt) : new Date(),
          changeFrequency: "weekly" as const,
          priority: 0.8,
        })
      }
      hasMore = page < res.totalPages
      page++
    }
  } catch {
    // Backend unreachable — emit only static routes
  }

  return [...staticRoutes, ...topicRoutes, ...categoryRoutes, ...productRoutes, ...blogPosts]
}
