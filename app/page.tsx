import type { Metadata } from 'next'
import { canonicalUrl, SITE_NAME, LOGO_URL } from '@/lib/seo/config'
import { siteConfig } from '@/content/site'
import HomePageClient from "./HomePageClient"

export const metadata: Metadata = {
  title: 'Home',
  description: siteConfig.company.description,
  alternates: { canonical: canonicalUrl('/') },
  openGraph: {
    type: 'website',
    url: canonicalUrl('/'),
    title: SITE_NAME,
    description: siteConfig.company.description,
    siteName: SITE_NAME,
    images: [LOGO_URL],
  },
  twitter: {
    card: 'summary_large_image',
    title: SITE_NAME,
    description: siteConfig.company.description,
  },
}

export default function HomePage() {
  return <HomePageClient />
}
