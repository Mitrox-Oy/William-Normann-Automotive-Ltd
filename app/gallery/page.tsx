import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { siteConfig } from "@/content/site"
import { Instagram, Linkedin, Twitter } from "lucide-react"
import { Button } from "@/components/ui/button"
import Image from "next/image"
import Link from "next/link"
import type { Metadata } from 'next'
import { canonicalUrl, SITE_NAME, LOGO_URL } from '@/lib/seo/config'

export const metadata: Metadata = {
    title: "Gallery",
    description: "Explore our automotive work and follow us on social media for more.",
    alternates: { canonical: canonicalUrl('/gallery') },
    openGraph: {
        type: 'website',
        url: canonicalUrl('/gallery'),
        title: `Gallery | ${SITE_NAME}`,
        description: 'Explore our automotive work and follow us on social media for more.',
        siteName: SITE_NAME,
        images: [LOGO_URL],
    },
    twitter: {
        card: 'summary_large_image',
        title: `Gallery | ${SITE_NAME}`,
        description: 'Explore our automotive work and follow us on social media for more.',
    },
}

// Gallery images - you can replace these with actual image paths
const galleryImages = [
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
    "/placeholder.svg",
]

export default function GalleryPage() {
    return (
        <>
            {/* Header */}
            <section className="py-16 lg:py-20">
                <Container>
                    <SectionHeading
                        title="Gallery"
                        subtitle="See our work and connect with us on social media"
                        centered
                    />
                </Container>
            </section>

            {/* Social Media Links */}
            <section className="py-8">
                <Container>
                    <div className="flex justify-center gap-4 flex-wrap">
                        <Button asChild variant="outline" size="lg" className="gap-2">
                            <Link href={siteConfig.social.instagram} target="_blank" rel="noopener noreferrer">
                                <Instagram className="h-5 w-5" />
                                Follow on Instagram
                            </Link>
                        </Button>
                        <Button asChild variant="outline" size="lg" className="gap-2">
                            <Link href={siteConfig.social.linkedin} target="_blank" rel="noopener noreferrer">
                                <Linkedin className="h-5 w-5" />
                                Connect on LinkedIn
                            </Link>
                        </Button>
                        <Button asChild variant="outline" size="lg" className="gap-2">
                            <Link href={siteConfig.social.twitter} target="_blank" rel="noopener noreferrer">
                                <Twitter className="h-5 w-5" />
                                Follow on Twitter
                            </Link>
                        </Button>
                    </div>
                </Container>
            </section>

            {/* Gallery Grid - 3 columns on desktop, Instagram-style */}
            <section className="py-12 lg:py-20">
                <Container>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                        {galleryImages.map((image, index) => (
                            <div
                                key={index}
                                className="relative aspect-square overflow-hidden rounded-lg bg-muted hover:opacity-90 transition-opacity cursor-pointer group"
                            >
                                <Image
                                    src={image}
                                    alt={`Gallery image ${index + 1}`}
                                    fill
                                    className="object-cover transition-transform group-hover:scale-105"
                                />
                            </div>
                        ))}
                    </div>

                    {/* Call to action to see more on social media */}
                    <div className="mt-12 text-center">
                        <p className="text-lg text-muted-foreground mb-6">
                            Want to see more? Follow us on Instagram for daily updates!
                        </p>
                        <Button asChild size="lg" className="gap-2">
                            <Link href={siteConfig.social.instagram} target="_blank" rel="noopener noreferrer">
                                <Instagram className="h-5 w-5" />
                                See More on Instagram
                            </Link>
                        </Button>
                    </div>
                </Container>
            </section>
        </>
    )
}
