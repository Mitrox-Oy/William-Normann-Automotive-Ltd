import { siteConfig } from "@/content/site"
import type { MetadataRoute } from "next"

export const dynamic = "force-static"

export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = process.env.NEXT_PUBLIC_SITE_URL || "https://williamnormann.com"
  const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ""

  // Static pages
  const routes = ["", "/services", "/about", "/blog"].map((route) => ({
    url: `${baseUrl}${basePath}${route}`,
    lastModified: new Date(),
    changeFrequency: "weekly" as const,
    priority: route === "" ? 1 : 0.8,
  }))

  // Blog posts
  const blogPosts = siteConfig.blog.posts.map((post) => ({
    url: `${baseUrl}${basePath}/blog/${post.slug}`,
    lastModified: new Date(post.date),
    changeFrequency: "monthly" as const,
    priority: 0.6,
  }))

  return [...routes, ...blogPosts]
}
