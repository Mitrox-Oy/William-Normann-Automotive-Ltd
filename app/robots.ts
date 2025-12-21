import type { MetadataRoute } from "next"

export const dynamic = "force-static"

export default function robots(): MetadataRoute.Robots {
  const baseUrl = process.env.NEXT_PUBLIC_SITE_URL || "https://williamnormann.com"
  const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ""
  
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/api/"],
    },
    sitemap: `${baseUrl}${basePath}/sitemap.xml`,
  }
}
