import type React from "react"
import type { Metadata } from "next"

import { Analytics } from "@vercel/analytics/next"
import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"
import { MobileCTA } from "@/components/mobile-cta"
import { AnnouncementBar } from "@/components/announcement-bar"
import { CartProvider } from "@/components/CartContext"
import { AuthProvider } from "@/components/AuthProvider"
import { siteConfig } from "@/content/site"
import { withBasePath } from "@/lib/base-path"
import "./globals.css"

import { Inter, Geist_Mono, Source_Serif_4, Inter as V0_Font_Inter, Geist_Mono as V0_Font_Geist_Mono, Source_Serif_4 as V0_Font_Source_Serif_4 } from 'next/font/google'

// Initialize fonts
const _inter = V0_Font_Inter({ subsets: ['latin'], weight: ["100", "200", "300", "400", "500", "600", "700", "800", "900"] })
const _geistMono = V0_Font_Geist_Mono({ subsets: ['latin'], weight: ["100", "200", "300", "400", "500", "600", "700", "800", "900"] })
const _sourceSerif_4 = V0_Font_Source_Serif_4({ subsets: ['latin'], weight: ["200", "300", "400", "500", "600", "700", "800", "900"] })

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
})

const geistMono = Geist_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
})

const sourceSerif4 = Source_Serif_4({
  subsets: ["latin"],
  variable: "--font-serif",
})

export const metadata: Metadata = {
  title: {
    default: siteConfig.company.name,
    template: `%s | ${siteConfig.company.name}`,
  },
  description: siteConfig.company.description,
  keywords: [
    "automotive parts sourcing",
    "hard to find car parts",
    "OEM parts international",
    "wholesale automotive distributor",
    "cross-border automotive trade",
    "discontinued car parts",
    "JDM parts Europe",
    "automotive parts import export",
  ],
  authors: [{ name: siteConfig.company.name }],
  creator: siteConfig.company.name,
  openGraph: {
    type: "website",
    locale: "en_GB",
    url: "https://williamnormann.com",
    siteName: siteConfig.company.name,
    title: siteConfig.company.name,
    description: siteConfig.company.description,
  },
  twitter: {
    card: "summary_large_image",
    title: siteConfig.company.name,
    description: siteConfig.company.description,
  },
  generator: "v0.app",
  icons: {
    icon: [
      {
        url: withBasePath("/william-automotive-logo-white-background.png"),
        media: "(prefers-color-scheme: light)",
      },
      {
        url: withBasePath("/william-automotive-logo-black-background.png"),
        media: "(prefers-color-scheme: dark)",
      },
    ],
    apple: withBasePath("/william-automotive-logo-black-background.png"),
  },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`${inter.variable} ${geistMono.variable} ${sourceSerif4.variable} font-sans antialiased`}>
        <AuthProvider>
          <CartProvider>
            <div className="fixed inset-0 z-0">
              <video
                autoPlay
                loop
                muted
                playsInline
                className="absolute inset-0 h-full w-full object-cover"
              >
                <source src={withBasePath("/william-automotive-background-video.mov")} type="video/quicktime" />
                <source src={withBasePath("/william-automotive-background-video.mov")} type="video/mp4" />
              </video>
              {/* Dim overlay to improve readability */}
              <div className="absolute inset-0 bg-black/60 pointer-events-none" />
            </div>
            <div className="relative z-10">
              <AnnouncementBar />
              <Navbar />
              <main className="min-h-screen">{children}</main>
              <Footer />
              <MobileCTA />
            </div>
            <Analytics />
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  )
}
