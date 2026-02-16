/**
 * SEO Configuration — single source of truth for site-wide SEO values.
 * Only affects <head> metadata and structured data; never renders visible text.
 */

import { siteConfig } from '@/content/site'

/** Canonical base URL (no trailing slash). */
export const SITE_URL =
    (process.env.NEXT_PUBLIC_SITE_URL || 'https://williamnormann.com').replace(/\/+$/, '')

export const SITE_NAME = siteConfig.company.name

/** Return an absolute canonical URL for the given path. */
export function canonicalUrl(path: string): string {
    const clean = path.startsWith('/') ? path : `/${path}`
    return `${SITE_URL}${clean}`
}

/** Absolute URL for the site logo (used in OG / JSON-LD). */
export const LOGO_URL = `${SITE_URL}/william-automotive-logo-black-background.png`
