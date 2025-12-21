"use client"

import type React from "react"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface CTAButtonProps {
  href?: string
  onClick?: () => void
  children: React.ReactNode
  variant?: "default" | "outline" | "ghost"
  size?: "default" | "sm" | "lg"
  className?: string
}

export function CTAButton({
  href,
  onClick,
  children,
  variant = "default",
  size = "default",
  className,
}: CTAButtonProps) {
  if (href) {
    return (
      <Button asChild variant={variant} size={size} className={cn(className)}>
        <Link href={href}>{children}</Link>
      </Button>
    )
  }

  return (
    <Button onClick={onClick} variant={variant} size={size} className={cn(className)}>
      {children}
    </Button>
  )
}
