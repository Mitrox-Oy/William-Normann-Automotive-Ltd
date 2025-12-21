"use client"

import { motion } from "framer-motion"
import { siteConfig } from "@/content/site"

export function HowItWorksTimeline() {
  return (
    <div className="relative">
      {/* Connecting line */}
      <div className="absolute left-6 top-6 hidden h-[calc(100%-48px)] w-0.5 bg-gradient-to-b from-primary via-primary/50 to-primary md:block" />

      <div className="space-y-8">
        {siteConfig.howItWorks.map((step, index) => (
          <motion.div
            key={step.step}
            initial={{ opacity: 0, x: -20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ delay: index * 0.1, duration: 0.5 }}
            className="relative flex gap-6"
          >
            {/* Step number node */}
            <div className="relative z-10 flex h-12 w-12 shrink-0 items-center justify-center rounded-full border-4 border-background bg-primary font-bold text-primary-foreground shadow-lg">
              {step.step}
            </div>

            {/* Content */}
            <div className="flex-1 rounded-xl p-6 transition-all">
              <h3 className="mb-2 text-lg font-semibold">{step.title}</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">{step.description}</p>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  )
}
