import type { Metadata } from 'next'
import { isValidTopic, type ShopTopic } from '@/lib/shopApi'
import { getCachedProduct, getCachedCategory } from '@/lib/seo/data'
import { productMetadata, topicMetadata } from '@/lib/seo/metadata'
import { ProductJsonLd, BreadcrumbJsonLd, type BreadcrumbItem } from '@/lib/seo/json-ld'
import { canonicalUrl } from '@/lib/seo/config'
import TopicPage from './topic-page'
import ProductDetailPage from './product-detail-page'

// ---------------------------------------------------------------------------
// Metadata  (runs server-side, invisible to users)
// ---------------------------------------------------------------------------

interface PageProps {
  params: Promise<{ slug: string }>
  searchParams: Promise<Record<string, string | string[] | undefined>>
}

export async function generateMetadata({ params, searchParams }: PageProps): Promise<Metadata> {
  const { slug } = await params
  const sp = await searchParams

  if (isValidTopic(slug)) {
    return topicMetadata(slug, sp)
  }

  const product = await getCachedProduct(slug)
  if (!product) return { title: 'Product Not Found' }
  return productMetadata(product)
}

// ---------------------------------------------------------------------------
// Page  (server component wrapper — existing client components render below)
// ---------------------------------------------------------------------------

export default async function ShopSlugPage({ params, searchParams }: PageProps) {
  const { slug } = await params

  // Topic route (cars / parts / custom)
  if (isValidTopic(slug)) {
    // Breadcrumb: Home → Shop → Topic
    const topicBreadcrumb: BreadcrumbItem[] = [
      { name: 'Home', url: canonicalUrl('/') },
      { name: 'Shop', url: canonicalUrl('/shop') },
      { name: slug.charAt(0).toUpperCase() + slug.slice(1), url: canonicalUrl(`/shop/${slug}`) },
    ]
    return (
      <>
        <BreadcrumbJsonLd items={topicBreadcrumb} />
        <TopicPage topic={slug as ShopTopic} />
      </>
    )
  }

  // Product route
  const product = await getCachedProduct(slug)

  if (product) {
    // Breadcrumb: Home → Shop → [Category] → Product
    const crumbs: BreadcrumbItem[] = [
      { name: 'Home', url: canonicalUrl('/') },
      { name: 'Shop', url: canonicalUrl('/shop') },
    ]
    if (product.categoryName) {
      crumbs.push({ name: product.categoryName, url: canonicalUrl('/shop') })
    }
    crumbs.push({ name: product.name, url: canonicalUrl(`/shop/${product.slug}`) })

    return (
      <>
        <ProductJsonLd product={product} />
        <BreadcrumbJsonLd items={crumbs} />
        <ProductDetailPage slug={slug} />
      </>
    )
  }

  // Fallback — product not found; let the client component show its own 404 UI
  return <ProductDetailPage slug={slug} />
}