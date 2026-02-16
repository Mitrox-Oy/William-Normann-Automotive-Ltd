import { notFound } from "next/navigation"
import Image from "next/image"
import Link from "next/link"
import { Container } from "@/components/container"
import { CTAButton } from "@/components/cta-button"
import { siteConfig } from "@/content/site"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Calendar, Clock, ArrowLeft } from "lucide-react"
import type { Metadata } from "next"
import { canonicalUrl, SITE_NAME, LOGO_URL } from "@/lib/seo/config"
import { BreadcrumbJsonLd, type BreadcrumbItem } from "@/lib/seo/json-ld"

interface BlogPostPageProps {
  params: Promise<{ slug: string }>
}

export async function generateMetadata({ params }: BlogPostPageProps): Promise<Metadata> {
  const { slug } = await params
  const post = siteConfig.blog.posts.find((p) => p.slug === slug)

  if (!post) {
    return {
      title: "Post Not Found",
    }
  }

  const url = canonicalUrl(`/blog/${post.slug}`)
  const image = post.image ? `${canonicalUrl('')}${post.image}` : LOGO_URL

  return {
    title: post.title,
    description: post.excerpt,
    alternates: { canonical: url },
    openGraph: {
      type: 'article',
      url,
      title: `${post.title} | ${SITE_NAME}`,
      description: post.excerpt,
      siteName: SITE_NAME,
      images: [image],
      publishedTime: post.date,
    },
    twitter: {
      card: 'summary_large_image',
      title: `${post.title} | ${SITE_NAME}`,
      description: post.excerpt,
      images: [image],
    },
  }
}

export async function generateStaticParams() {
  return siteConfig.blog.posts.map((post) => ({
    slug: post.slug,
  }))
}

export default async function BlogPostPage({ params }: BlogPostPageProps) {
  const { slug } = await params
  const post = siteConfig.blog.posts.find((p) => p.slug === slug)

  if (!post) {
    notFound()
  }

  const crumbs: BreadcrumbItem[] = [
    { name: 'Home', url: canonicalUrl('/') },
    { name: 'Blog', url: canonicalUrl('/blog') },
    { name: post.title, url: canonicalUrl(`/blog/${post.slug}`) },
  ]

  return (
    <>
      <BreadcrumbJsonLd items={crumbs} />
      <article>
        {/* Header */}
        <section className="border-b bg-muted/30 py-16 lg:py-20">
          <Container size="narrow">
            <Link
              href="/blog"
              className="mb-8 inline-flex items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to blog
            </Link>
            <Badge variant="secondary" className="mb-4">
              {post.category}
            </Badge>
            <h1 className="text-balance text-4xl font-bold tracking-tight sm:text-5xl">{post.title}</h1>
            <div className="mt-6 flex items-center gap-4 text-sm text-muted-foreground">
              <div className="flex items-center gap-1">
                <Calendar className="h-4 w-4" />
                <time dateTime={post.date}>
                  {new Date(post.date).toLocaleDateString("en-GB", {
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                  })}
                </time>
              </div>
              <div className="flex items-center gap-1">
                <Clock className="h-4 w-4" />
                <span>{post.readTime} min read</span>
              </div>
            </div>
          </Container>
        </section>

        {/* Featured Image */}
        <section className="border-b py-12">
          <Container size="narrow">
            <div className="relative aspect-video w-full overflow-hidden rounded-lg">
              <Image src={post.image || "/placeholder.svg"} alt={post.title} fill className="object-cover" />
            </div>
          </Container>
        </section>

        {/* Content */}
        <section className="py-16 lg:py-20">
          <Container size="narrow">
            <div className="prose prose-lg mx-auto">
              <p className="lead text-pretty text-xl text-muted-foreground">{post.excerpt}</p>

              <h2>Introduction</h2>
              <p>
                This is where the full blog post content would appear. In a production environment, you would integrate a
                CMS like Sanity, Contentful, or use MDX files for rich content management.
              </p>

              <h2>Key Takeaways</h2>
              <ul>
                <li>Important point about automotive sourcing</li>
                <li>Expert insights on international trade</li>
                <li>Best practices for workshops and distributors</li>
                <li>Tips for finding reliable suppliers</li>
              </ul>

              <h2>Conclusion</h2>
              <p>
                Contact our team to learn more about how we can help with your automotive sourcing needs. Our experts are
                ready to assist you with parts sourcing, market connections, and technical consulting.
              </p>
            </div>
          </Container>
        </section>

        {/* CTA Banner */}
        <section className="border-t bg-muted/30 py-16">
          <Container size="narrow">
            <Card>
              <CardContent className="p-8 text-center">
                <h2 className="mb-2 text-2xl font-bold">Need Automotive Parts or Consulting?</h2>
                <p className="mb-6 text-muted-foreground">
                  Get in touch with our team for expert sourcing and competitive quotes.
                </p>
                <CTAButton href="/#contact" size="lg">
                  Request a Quote
                </CTAButton>
              </CardContent>
            </Card>
          </Container>
        </section>
      </article>
    </>
  )
}
