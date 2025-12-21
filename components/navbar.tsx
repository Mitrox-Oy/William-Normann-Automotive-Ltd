"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Menu, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Container } from "@/components/container"
import { siteConfig } from "@/content/site"
import { cn } from "@/lib/utils"

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const pathname = usePathname()

  useEffect(() => {
    function handleScroll() {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  return (
    <header className="sticky top-0 z-50 transition-all duration-300 pt-4">
      <Container>
        <nav
          className={cn(
            "mx-auto flex h-16 items-center justify-between rounded-2xl px-6 transition-all duration-300",
            "bg-gray-900/40 backdrop-blur-2xl border border-white/10 shadow-lg",
            scrolled && "bg-gray-900/50",
          )}
        >
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary font-bold text-white shadow-sm">
              WN
            </div>
            <span className="hidden font-bold text-white sm:inline-block">{siteConfig.company.shortName}</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden items-center gap-8 md:flex">
            {siteConfig.nav.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "text-sm font-medium text-white transition-colors hover:text-primary",
                  pathname === item.href && "text-primary",
                )}
              >
                {item.label}
              </Link>
            ))}
          </div>

          <div className="flex items-center gap-4">
            <Button asChild size="sm" className="hidden shadow-sm sm:inline-flex">
              <Link href="/#contact">Get Quote</Link>
            </Button>

            {/* Mobile Menu Button */}
            <Button variant="ghost" size="sm" className="md:hidden text-white" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </Button>
          </div>
        </nav>

        {/* Mobile Navigation */}
        {mobileMenuOpen && (
          <div className="mt-2 rounded-2xl bg-gray-900/40 backdrop-blur-2xl border border-white/10 pb-4 md:hidden">
            <div className="space-y-1 px-6 pt-4">
              {siteConfig.nav.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "block rounded-md px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-white/10",
                    pathname === item.href && "bg-white/10",
                  )}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  {item.label}
                </Link>
              ))}
              <div className="px-3 pt-2">
                <Button asChild size="sm" className="w-full">
                  <Link href="/#contact" onClick={() => setMobileMenuOpen(false)}>
                    Get Quote
                  </Link>
                </Button>
              </div>
            </div>
          </div>
        )}
      </Container>
    </header>
  )
}
