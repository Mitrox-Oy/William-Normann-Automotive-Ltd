import { cn } from "@/lib/utils"
import type { ReactNode } from "react"

interface GlassPanelProps {
  children: ReactNode
  className?: string
  hover?: boolean
}

export function GlassPanel({ children, className, hover = false }: GlassPanelProps) {
  return (
    <div
      className={cn(
        "rounded-2xl p-8 transition-all",
        className,
      )}
    >
      {children}
    </div>
  )
}
