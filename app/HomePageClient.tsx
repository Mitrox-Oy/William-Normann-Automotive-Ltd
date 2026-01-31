"use client"

import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { CTAButton } from "@/components/cta-button"
import { siteConfig } from "@/content/site"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { ArrowRight, Globe, Package, CheckCircle2, Check } from "lucide-react"
// <CHANGE> Import new premium components
import { GlassPanel } from "@/components/glass-panel"
import { HowItWorksTimeline } from "@/components/how-it-works-timeline"
import { QuoteForm } from "@/components/quote-form"
import { motion } from "framer-motion"

export default function HomePageClient() {
  return (
    <>
      {/* <CHANGE> Premium hero with glass CTA panel */}
      <section className="relative overflow-hidden py-28 lg:py-40">
        <Container>
          <div className="max-w-4xl mx-auto">
            {/* Content */}
            <div className="flex flex-col items-center text-center">
              <Badge variant="secondary" className="mb-6 w-fit">
                {siteConfig.company.tagline}
              </Badge>
              <h1 className="text-balance text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
                {siteConfig.hero.headline}
              </h1>
              <p className="mt-6 text-pretty text-lg text-muted-foreground leading-relaxed sm:text-xl">
                {siteConfig.hero.subheadline}
              </p>

              {/* <CHANGE> CTA panel in glass container */}
              <GlassPanel className="mt-10 w-full" hover>
                <div className="space-y-6">
                  <div className="flex flex-col gap-4 sm:flex-row sm:justify-center">
                    <CTAButton href="/#contact" size="lg" className="shadow-lg">
                      {siteConfig.hero.primaryCTA}
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </CTAButton>
                    <CTAButton href="/shop" variant="outline" size="lg">
                      {siteConfig.hero.secondaryCTA}
                    </CTAButton>
                  </div>

                  {/* Credibility badges */}
                  <div className="space-y-2 pt-4">
                    {siteConfig.hero.badges.map((badge, i) => (
                      <div key={i} className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
                        <CheckCircle2 className="h-4 w-4 text-primary" />
                        <span>{badge}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </GlassPanel>
            </div>
          </div>
        </Container>
      </section>

      {/* Services grid - all equal size */}
      <section className="py-28 lg:py-36">
        <Container>
          <SectionHeading
            title="What We Do Best"
            subtitle="Specialized automotive sourcing from Dubai to Europe - with global reach to USA, MENA and Asia"
            centered
            className="mb-16"
          />

          {/* All services in equal grid */}
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 auto-rows-max">
            {siteConfig.services
              .filter((s) => s.slug !== "warranty")
              .map((service, i) => (
                <motion.div
                  key={service.slug}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="w-full"
                >
                  <Card className="h-full transition-all w-full">
                    <CardContent className="flex flex-col p-6">
                      <Package className="mb-4 h-8 w-8 text-primary" />
                      <h3 className="mb-2 font-semibold">{service.title}</h3>
                      <p className="mb-4 text-sm text-muted-foreground leading-relaxed">{service.description}</p>
                      <div className="mt-auto w-full space-y-4">
                        <Accordion type="single" collapsible className="w-full">
                          <AccordionItem value="features" className="border-none">
                            <AccordionTrigger className="text-sm py-2 hover:no-underline">What's included</AccordionTrigger>
                            <AccordionContent className="pb-0">
                              <ul className="space-y-2 pt-4 pb-4">
                                {service.features.map((feature, idx) => (
                                  <li key={idx} className="flex items-start gap-2">
                                    <Check className="mt-0.5 h-3 w-3 shrink-0 text-primary" />
                                    <span className="text-xs text-muted-foreground">{feature}</span>
                                  </li>
                                ))}
                              </ul>
                            </AccordionContent>
                          </AccordionItem>
                        </Accordion>
                        <Badge variant="outline" className="text-xs w-fit">
                          {service.turnaround}
                        </Badge>
                      </div>
                    </CardContent>
                  </Card>
                </motion.div>
              ))}
          </div>
        </Container>
      </section>

      {/* <CHANGE> Timeline component for how it works */}
      <section className="py-28 lg:py-36">
        <Container>
          <SectionHeading
            title="How It Works"
            subtitle="From inquiry to delivery: our transparent 4-step process"
            centered
            className="mb-20"
          />
          <div className="mx-auto max-w-3xl">
            <HowItWorksTimeline />
          </div>
        </Container>
      </section>



      {/* <CHANGE> Quote form with premium layout */}
      <section id="contact" className="py-28 lg:py-36">
        <Container>
          <div className="relative">
            <div className="relative">
              <SectionHeading
                title="Request Your Quote"
                subtitle="Tell us what you need. We'll respond with pricing, lead time, and shipping options within 24 hours."
                centered
                className="mb-12"
              />

              {/* What You Get in Every Quote */}
              <div className="mb-16">
                <h3 className="text-center text-2xl font-bold mb-12">What You Get in Every Quote</h3>
                <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4 mb-12">
                  <div className="text-center">
                    <CheckCircle2 className="mx-auto mb-4 h-8 w-8 text-primary" />
                    <h4 className="mb-2 font-semibold">Part Numbers & Alternatives</h4>
                    <p className="text-sm text-muted-foreground">OEM codes, cross-references, and verified aftermarket options</p>
                  </div>
                  <div className="text-center">
                    <CheckCircle2 className="mx-auto mb-4 h-8 w-8 text-primary" />
                    <h4 className="mb-2 font-semibold">Origin & Lead Time</h4>
                    <p className="text-sm text-muted-foreground">Source location, availability status, and realistic delivery estimates</p>
                  </div>
                  <div className="text-center">
                    <CheckCircle2 className="mx-auto mb-4 h-8 w-8 text-primary" />
                    <h4 className="mb-2 font-semibold">Shipping Options & Costs</h4>
                    <p className="text-sm text-muted-foreground">Multiple freight options with transparent pricing and tracking</p>
                  </div>
                  <div className="text-center">
                    <CheckCircle2 className="mx-auto mb-4 h-8 w-8 text-primary" />
                    <h4 className="mb-2 font-semibold">Duties & HS Codes Guidance</h4>
                    <p className="text-sm text-muted-foreground">Import classification, estimated duties, and compliance documentation</p>
                  </div>
                </div>
              </div>

              <QuoteForm source="home-page" />
            </div>
          </div>
        </Container>
      </section>
    </>
  )
}
