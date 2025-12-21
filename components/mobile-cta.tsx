"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { motion, AnimatePresence } from "framer-motion"
import { MessageSquare } from "lucide-react"

export function MobileCTA() {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    function handleScroll() {
      setVisible(window.scrollY > 600)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ y: 100, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 100, opacity: 0 }}
          className="fixed bottom-0 left-0 right-0 z-40 border-t bg-background/95 p-4 backdrop-blur-lg md:hidden"
        >
          <Button asChild size="lg" className="w-full shadow-lg">
            <Link href="/#contact">
              <MessageSquare className="mr-2 h-5 w-5" />
              Request a Quote
            </Link>
          </Button>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
