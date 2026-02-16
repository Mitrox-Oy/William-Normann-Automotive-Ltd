/**
 * JSON-LD structured-data components — rendered as <script type="application/ld+json">.
 * Server components only; produce zero visible output.
 */

import { siteConfig } from '@/content/site'
import { SITE_URL, SITE_NAME, LOGO_URL, canonicalUrl } from './config'
import { getImageUrl } from '@/lib/utils'
import type { Product } from '@/lib/shopApi'

// ---------------------------------------------------------------------------
// Generic wrapper
// ---------------------------------------------------------------------------

function JsonLd({ data }: { data: Record<string, unknown> }) {
    return (
        <script
            type="application/ld+json"
            dangerouslySetInnerHTML={{ __html: JSON.stringify(data, null, 0) }}
        />
    )
}

// ---------------------------------------------------------------------------
// schema.org/Product + Offer
// ---------------------------------------------------------------------------

const AVAILABILITY_MAP: Record<string, string> = {
    in_stock: 'https://schema.org/InStock',
    low_stock: 'https://schema.org/LimitedAvailability',
    out_of_stock: 'https://schema.org/OutOfStock',
    pre_order: 'https://schema.org/PreOrder',
}

const CONDITION_MAP: Record<string, string> = {
    new: 'https://schema.org/NewCondition',
    used: 'https://schema.org/UsedCondition',
    refurbished: 'https://schema.org/RefurbishedCondition',
    reconditioned: 'https://schema.org/RefurbishedCondition',
}

function absoluteImg(img: string | undefined): string {
    if (!img) return LOGO_URL
    const resolved = getImageUrl(img)
    return resolved.startsWith('http') ? resolved : `${SITE_URL}${resolved}`
}

export function ProductJsonLd({ product }: { product: Product }) {
    const images = product.images.filter(Boolean).map(absoluteImg)
    if (images.length === 0) images.push(LOGO_URL)

    const offer: Record<string, unknown> = {
        '@type': 'Offer',
        url: canonicalUrl(`/shop/${product.slug}`),
        priceCurrency: product.currency || 'USD',
        availability: AVAILABILITY_MAP[product.availability] || 'https://schema.org/InStock',
    }

    // Only include price when it's a priced product (not quote-only)
    if (!product.quoteOnly && product.price > 0) {
        offer.price = product.price.toFixed(2)
    }

    if (product.condition) {
        const mapped = CONDITION_MAP[product.condition.toLowerCase()]
        if (mapped) offer.itemCondition = mapped
    }

    const data: Record<string, unknown> = {
        '@context': 'https://schema.org',
        '@type': 'Product',
        name: product.name,
        description: product.description || product.shortDescription || product.name,
        image: images,
        sku: product.sku,
        url: canonicalUrl(`/shop/${product.slug}`),
        offers: offer,
    }

    if (product.brand || product.manufacturer) {
        data.brand = { '@type': 'Brand', name: product.brand || product.manufacturer }
    }
    if (product.partNumber) {
        data.mpn = product.partNumber
    }
    if (product.categoryName) {
        data.category = product.categoryName
    }

    return <JsonLd data={data} />
}

// ---------------------------------------------------------------------------
// schema.org/BreadcrumbList
// ---------------------------------------------------------------------------

export interface BreadcrumbItem {
    name: string
    url: string
}

export function BreadcrumbJsonLd({ items }: { items: BreadcrumbItem[] }) {
    const data = {
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: items.map((item, i) => ({
            '@type': 'ListItem',
            position: i + 1,
            name: item.name,
            item: item.url,
        })),
    }
    return <JsonLd data={data} />
}

// ---------------------------------------------------------------------------
// schema.org/Organization  (render once in root layout)
// ---------------------------------------------------------------------------

export function OrganizationJsonLd() {
    const sameAs: string[] = []
    if (siteConfig.social.linkedin) sameAs.push(siteConfig.social.linkedin)
    if (siteConfig.social.twitter) sameAs.push(siteConfig.social.twitter)
    if (siteConfig.social.instagram) sameAs.push(siteConfig.social.instagram)

    const data: Record<string, unknown> = {
        '@context': 'https://schema.org',
        '@type': 'Organization',
        name: SITE_NAME,
        url: SITE_URL,
        logo: LOGO_URL,
        contactPoint: {
            '@type': 'ContactPoint',
            telephone: siteConfig.contact.phone,
            email: siteConfig.contact.email,
            contactType: 'customer service',
        },
        address: {
            '@type': 'PostalAddress',
            streetAddress: siteConfig.contact.address.line1,
            addressCountry: 'AE',
        },
    }
    if (sameAs.length > 0) data.sameAs = sameAs

    return <JsonLd data={data} />
}

// ---------------------------------------------------------------------------
// schema.org/WebSite with SearchAction  (render once in root layout)
// ---------------------------------------------------------------------------

export function WebSiteJsonLd() {
    const data = {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: SITE_NAME,
        url: SITE_URL,
        potentialAction: {
            '@type': 'SearchAction',
            target: {
                '@type': 'EntryPoint',
                urlTemplate: `${SITE_URL}/shop/parts?q={search_term_string}`,
            },
            'query-input': 'required name=search_term_string',
        },
    }
    return <JsonLd data={data} />
}
