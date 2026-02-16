import type { Metadata } from 'next'
import { isValidTopic } from '@/lib/shopApi'
import { TOPIC_INFO } from '@/lib/shopApi'
import { getCachedCategory } from '@/lib/seo/data'
import { categoryMetadata } from '@/lib/seo/metadata'
import { BreadcrumbJsonLd, type BreadcrumbItem } from '@/lib/seo/json-ld'
import { canonicalUrl } from '@/lib/seo/config'
import SubcategoryPage from './subcategory-page'

// ---------------------------------------------------------------------------
// Metadata  (runs server-side, invisible to users)
// ---------------------------------------------------------------------------

interface PageProps {
  params: Promise<{ slug: string; subSlug: string }>
  searchParams: Promise<Record<string, string | string[] | undefined>>
}

export async function generateMetadata({ params, searchParams }: PageProps): Promise<Metadata> {
  const { slug, subSlug } = await params
  const sp = await searchParams

  if (!isValidTopic(slug)) return { title: 'Not Found' }

  const category = await getCachedCategory(subSlug)
  return categoryMetadata(category, slug, sp)
}

// ---------------------------------------------------------------------------
// Page  (server wrapper — client component renders below)
// ---------------------------------------------------------------------------

export default async function TopicSubcategoryPageEntry({ params }: PageProps) {
  const { slug, subSlug } = await params

  // Build breadcrumb JSON-LD: Home → Shop → Topic → Subcategory
  const topicLabel = isValidTopic(slug)
    ? TOPIC_INFO[slug].label
    : slug.charAt(0).toUpperCase() + slug.slice(1)

  const category = await getCachedCategory(subSlug)

  const crumbs: BreadcrumbItem[] = [
    { name: 'Home', url: canonicalUrl('/') },
    { name: 'Shop', url: canonicalUrl('/shop') },
    { name: topicLabel, url: canonicalUrl(`/shop/${slug}`) },
  ]
  if (category) {
    crumbs.push({ name: category.name, url: canonicalUrl(`/shop/${slug}/${category.slug}`) })
  }

  return (
    <>
      <BreadcrumbJsonLd items={crumbs} />
      <SubcategoryPage />
    </>
  )
}
