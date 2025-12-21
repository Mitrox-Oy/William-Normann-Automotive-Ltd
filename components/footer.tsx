"use client"

import Link from "next/link"
import { Container } from "@/components/container"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { siteConfig } from "@/content/site"
import { Linkedin, Twitter, Instagram, Mail, Phone, MapPin } from "lucide-react"

export function Footer() {
  return (
    <footer className="py-12 lg:py-16">
      <Container>
        <div className="mx-auto rounded-2xl bg-gray-900/40 backdrop-blur-2xl border border-white/10 shadow-lg p-8 lg:p-12">
        <div className="grid gap-12 lg:grid-cols-4">
          {/* Company Info */}
          <div className="lg:col-span-2">
            <Link href="/" className="mb-4 flex items-center gap-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary font-bold text-primary-foreground">
                WN
              </div>
              <span className="font-bold">{siteConfig.company.shortName}</span>
            </Link>
            <p className="mb-6 text-pretty text-sm text-muted-foreground">{siteConfig.footer.tagline}</p>

            {/* Newsletter */}
            <div className="space-y-3">
              <p className="text-sm font-medium">Stay updated with our newsletter</p>
              <form className="flex gap-2" onSubmit={(e) => e.preventDefault()}>
                <Input type="email" placeholder="Your email" className="max-w-xs" />
                <Button type="submit" size="sm">
                  Subscribe
                </Button>
              </form>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="mb-4 font-semibold">Quick Links</h3>
            <ul className="space-y-3 text-sm">
              {siteConfig.nav.map((item) => (
                <li key={item.href}>
                  <Link href={item.href} className="text-muted-foreground transition-colors hover:text-foreground">
                    {item.label}
                  </Link>
                </li>
              ))}
              {siteConfig.footer.quickLinks.map((item) => (
                <li key={item.href}>
                  <Link href={item.href} className="text-muted-foreground transition-colors hover:text-foreground">
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h3 className="mb-4 font-semibold">Contact</h3>
            <ul className="space-y-3 text-sm">
              <li className="flex items-start gap-2 text-muted-foreground">
                <Mail className="mt-0.5 h-4 w-4 shrink-0" />
                <a href={`mailto:${siteConfig.contact.email}`} className="transition-colors hover:text-foreground">
                  {siteConfig.contact.email}
                </a>
              </li>
              <li className="flex items-start gap-2 text-muted-foreground">
                <Phone className="mt-0.5 h-4 w-4 shrink-0" />
                <a href={`tel:${siteConfig.contact.phone}`} className="transition-colors hover:text-foreground">
                  {siteConfig.contact.phone}
                </a>
              </li>
              <li className="flex items-start gap-2 text-muted-foreground">
                <MapPin className="mt-0.5 h-4 w-4 shrink-0" />
                <span>
                  {siteConfig.contact.address.line1}
                  <br />
                  {siteConfig.contact.address.line2}
                </span>
              </li>
            </ul>

            {/* Social Links */}
            <div className="mt-6 flex gap-3">
              <a
                href={siteConfig.social.linkedin}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-8 w-8 items-center justify-center rounded-md border transition-colors hover:bg-muted"
              >
                <Linkedin className="h-4 w-4" />
              </a>
              <a
                href={siteConfig.social.twitter}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-8 w-8 items-center justify-center rounded-md border transition-colors hover:bg-muted"
              >
                <Twitter className="h-4 w-4" />
              </a>
              <a
                href={siteConfig.social.instagram}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-8 w-8 items-center justify-center rounded-md border transition-colors hover:bg-muted"
              >
                <Instagram className="h-4 w-4" />
              </a>
            </div>
          </div>
        </div>

        <div className="mt-12 pt-8 text-center text-sm text-muted-foreground">
          {siteConfig.footer.copyright}
        </div>
        </div>
      </Container>
    </footer>
  )
}
