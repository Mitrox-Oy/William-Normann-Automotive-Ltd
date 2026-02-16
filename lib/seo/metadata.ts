/**
 * Metadata helpers — generate Next.js Metadata objects for every route type.
 * These are document-level <head> changes only; zero visible UI impact.
 */

import type { Metadata } from 'next'
import { SITE_URL, SITE_NAME, LOGO_URL, canonicalUrl } from './config'
import { getImageUrl } from '@/lib/utils'
import type { Product, Category, ShopTopic } from '@/lib/shopApi'
import { TOPIC_INFO } from '@/lib/shopApi'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Build an absolute OG-safe image URL from a product image path. */
function absoluteImageUrl(img: string | undefined): string {
    if (!img) return LOGO_URL
    const resolved = getImageUrl(img)
    if (resolved.startsWith('http')) return resolved
    return `${SITE_URL}${resolved}`
}

/** Truncate text to a maximum length, adding ellipsis if trimmed. */
function truncate(text: string, max: number): string {
    if (text.length <= max) return text
    return text.slice(0, max - 1).trimEnd() + '…'
}

/** True when searchParams contain any filter / sort / pagination key. */
export function hasFilterParams(sp: Record<string, string | string[] | undefined>): boolean {
    // We treat *any* query parameter as a filterable variant except utm_* tracking params
    return Object.keys(sp).some((k) => !k.startsWith('utm'))
}

// ---------------------------------------------------------------------------
// Product metadata
// ---------------------------------------------------------------------------

export function productMetadata(product: Product): Metadata {
    const title = [product.name, product.brand, product.categoryName]
        .filter(Boolean)
        .join(' — ')

    // Prefer existing description; fall back to factual summary from attributes
    const descParts: string[] = []
    if (product.description) {
        descParts.push(product.description)
    } else {
        if (product.brand) descParts.push(product.brand)
        if (product.condition) descParts.push(product.condition)
        if (product.categoryName) descParts.push(product.categoryName)
        if (product.make && product.model) descParts.push(`${product.make} ${product.model}`)
        if (product.price && !product.quoteOnly) descParts.push(`$${product.price.toFixed(2)}`)
    }
    const description = truncate(descParts.join(' · '), 160)

    const url = canonicalUrl(`/shop/${product.slug}`)
    const images = product.images
        .filter(Boolean)
        .map((img) => absoluteImageUrl(img))

    return {
        title,
        description,
        alternates: { canonical: url },
        robots: { index: true, follow: true },
        openGraph: {
            type: 'website',
            url,
            title,
            description,
            siteName: SITE_NAME,
            images: images.length > 0 ? images : [LOGO_URL],
        },
        twitter: {
            card: 'summary_large_image',
            title,
            description,
            images: images.length > 0 ? [images[0]] : [LOGO_URL],
        },
    }
}

// ---------------------------------------------------------------------------
// Topic (root category) metadata
// ---------------------------------------------------------------------------

export function topicMetadata(
    topicSlug: ShopTopic,
    searchParams: Record<string, string | string[] | undefined> = {},
): Metadata {
    const info = TOPIC_INFO[topicSlug]
    const title = `${info.label} — Shop`
    const description = info.description
    const url = canonicalUrl(`/shop/${topicSlug}`)
    const filtered = hasFilterParams(searchParams)

    return {
        title,
        description,
        alternates: { canonical: url }, // always points to unfiltered base
        robots: filtered ? { index: false, follow: true } : { index: true, follow: true },
        openGraph: {
            type: 'website',
            url,
            title: `${title} | ${SITE_NAME}`,
            description,
            siteName: SITE_NAME,
            images: [LOGO_URL],
        },
        twitter: {
            card: 'summary_large_image',
            title: `${title} | ${SITE_NAME}`,
            description,
        },
    }
}

// ---------------------------------------------------------------------------
// Sub-category metadata
// ---------------------------------------------------------------------------

export function categoryMetadata(
    category: Category | null,
    topicSlug: string,
    searchParams: Record<string, string | string[] | undefined> = {},
): Metadata {
    if (!category) return { title: 'Category Not Found' }

    const title = `${category.name} — Shop`
    const description = category.description
        ? truncate(category.description, 160)
        : `Browse ${category.name} products at ${SITE_NAME}.`
    const url = canonicalUrl(`/shop/${topicSlug}/${category.slug}`)
    const filtered = hasFilterParams(searchParams)

    return {
        title,
        description,
        alternates: { canonical: url },
        robots: filtered ? { index: false, follow: true } : { index: true, follow: true },
        openGraph: {
            type: 'website',
            url,
            title: `${title} | ${SITE_NAME}`,
            description,
            siteName: SITE_NAME,
            images: [LOGO_URL],
        },
        twitter: {
            card: 'summary_large_image',
            title: `${title} | ${SITE_NAME}`,
            description,
        },
    }
}
