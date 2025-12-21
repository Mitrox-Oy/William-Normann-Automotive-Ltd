"use client"

import { useEffect, useState } from "react"
import { CTAButton } from "@/components/cta-button"
import { useMobile } from "@/hooks/use-mobile"
import { cn } from "@/lib/utils"

export function StickyCTA() {
  const [isVisible, setIsVisible] = useState(false)
  const isMobile = useMobile()

  useEffect(() => {
    const handleScroll = () => {
      // Show CTA after scrolling down 300px
      setIsVisible(window.scrollY > 300)
    }

    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  if (!isVisible) return null

  return (
    <>
      {/* Desktop Sticky CTA - right side */}
      {!isMobile && (
        <div className="fixed right-6 top-1/2 z-40 hidden -translate-y-1/2 lg:block">
          <CTAButton
            href="/#contact"
            size="lg"
            className={cn(
              "shadow-lg transition-all duration-300",
              isVisible ? "translate-x-0 opacity-100" : "translate-x-full opacity-0",
            )}
          >
            Request a Quote
          </CTAButton>
        </div>
      )}

      {/* Mobile Sticky CTA - bottom */}
      {isMobile && (
        <div className="fixed bottom-0 left-0 right-0 z-40 border-t bg-background/95 p-4 backdrop-blur supports-[backdrop-filter]:bg-background/60 lg:hidden">
          <CTAButton href="/#contact" size="lg" className="w-full shadow-lg">
            Request a Quote
          </CTAButton>
        </div>
      )}
    </>
  )
}
