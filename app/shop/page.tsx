import type { Metadata } from 'next'
import { SITE_NAME, canonicalUrl, LOGO_URL } from '@/lib/seo/config'
import ShopOverview from './shop-overview'

export const metadata: Metadata = {
  title: 'Shop',
  description:
    'Browse cars, parts, and custom automotive products at ' + SITE_NAME + '.',
  alternates: { canonical: canonicalUrl('/shop') },
  robots: { index: true, follow: true },
  openGraph: {
    type: 'website',
    url: canonicalUrl('/shop'),
    title: `Shop | ${SITE_NAME}`,
    description:
      'Browse cars, parts, and custom automotive products at ' + SITE_NAME + '.',
    siteName: SITE_NAME,
    images: [LOGO_URL],
  },
  twitter: {
    card: 'summary_large_image',
    title: `Shop | ${SITE_NAME}`,
    description:
      'Browse cars, parts, and custom automotive products at ' + SITE_NAME + '.',
  },
}

export default function ShopPage() {
  return <ShopOverview />
}

