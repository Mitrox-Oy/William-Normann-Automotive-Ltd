import Link from "next/link"
import Image from "next/image"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { siteConfig } from "@/content/site"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Calendar, Clock, ArrowRight } from "lucide-react"
import type { Metadata } from 'next'
import { canonicalUrl, SITE_NAME, LOGO_URL } from '@/lib/seo/config'

export const metadata: Metadata = {
  title: "Blog",
  description: "Latest insights on automotive parts sourcing, international trade, and industry trends.",
  alternates: { canonical: canonicalUrl('/blog') },
  openGraph: {
    type: 'website',
    url: canonicalUrl('/blog'),
    title: `Blog | ${SITE_NAME}`,
    description: 'Latest insights on automotive parts sourcing, international trade, and industry trends.',
    siteName: SITE_NAME,
    images: [LOGO_URL],
  },
  twitter: {
    card: 'summary_large_image',
    title: `Blog | ${SITE_NAME}`,
    description: 'Latest insights on automotive parts sourcing, international trade, and industry trends.',
  },
}

export default function BlogPage() {
  return (
    <>
      {/* Header */}
      <section className="py-16 lg:py-20">
        <Container>
          <SectionHeading
            title="Blog"
            subtitle="Insights, guides, and industry news from the automotive world"
            centered
          />
        </Container>
      </section>

      {/* Blog Posts */}
      <section className="py-20 lg:py-28">
        <Container>
          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
            {siteConfig.blog.posts.map((post) => (
              <Card key={post.slug} className="flex flex-col overflow-hidden">
                <div className="relative aspect-video w-full overflow-hidden">
                  <Image
                    src={post.image || "/placeholder.svg"}
                    alt={post.title}
                    fill
                    className="object-cover transition-transform hover:scale-105"
                  />
                </div>
                <CardHeader>
                  <div className="mb-2 flex items-center gap-2">
                    <Badge variant="secondary">{post.category}</Badge>
                  </div>
                  <CardTitle className="text-xl">
                    <Link href={`/blog/${post.slug}`} className="hover:text-primary">
                      {post.title}
                    </Link>
                  </CardTitle>
                  <CardDescription>{post.excerpt}</CardDescription>
                </CardHeader>
                <CardContent className="flex-1">
                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-4 w-4" />
                      <time dateTime={post.date}>
                        {new Date(post.date).toLocaleDateString("en-GB", {
                          day: "numeric",
                          month: "short",
                          year: "numeric",
                        })}
                      </time>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock className="h-4 w-4" />
                      <span>{post.readTime} min read</span>
                    </div>
                  </div>
                </CardContent>
                <CardFooter>
                  <Button asChild variant="ghost" className="group w-full bg-white text-black hover:bg-white/90">
                    <Link href={`/blog/${post.slug}`}>
                      Read article
                      <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
                    </Link>
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        </Container>
      </section>
    </>
  )
}
