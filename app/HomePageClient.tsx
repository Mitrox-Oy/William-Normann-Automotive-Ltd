"use client"

import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { CTAButton } from "@/components/cta-button"
import { siteConfig } from "@/content/site"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { ArrowRight, Globe, Package, CheckCircle2 } from "lucide-react"
// <CHANGE> Import new premium components
import { GlassPanel } from "@/components/glass-panel"
import { HowItWorksTimeline } from "@/components/how-it-works-timeline"
import { QuoteForm } from "@/components/quote-form"
import { motion } from "framer-motion"

export default function HomePageClient() {
  return (
    <>
      {/* <CHANGE> Premium split hero with glass CTA panel */}
      <section className="relative overflow-hidden py-24 lg:py-32">
        <Container>
          <div className="grid gap-12 lg:grid-cols-2 lg:gap-16">
            {/* Left: Content */}
            <div className="flex flex-col justify-center">
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
              <GlassPanel className="mt-10" hover>
                <div className="space-y-6">
                  <div className="flex flex-col gap-4 sm:flex-row">
                    <CTAButton href="/#contact" size="lg" className="shadow-lg">
                      {siteConfig.hero.primaryCTA}
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </CTAButton>
                    <CTAButton href="/services" variant="outline" size="lg">
                      {siteConfig.hero.secondaryCTA}
                    </CTAButton>
                  </div>

                  {/* Credibility badges */}
                  <div className="space-y-2 pt-4">
                    {siteConfig.hero.badges.map((badge, i) => (
                      <div key={i} className="flex items-center gap-2 text-sm text-muted-foreground">
                        <CheckCircle2 className="h-4 w-4 text-primary" />
                        <span>{badge}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </GlassPanel>
            </div>

            {/* Right: Abstract illustration */}
            <div className="relative hidden lg:block">
              <div className="absolute inset-0 flex items-center justify-center">
                {/* <CHANGE> Abstract routes illustration with gradient */}
                <div className="relative h-full w-full">
                  <svg viewBox="0 0 400 400" className="h-full w-full" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                      <linearGradient id="routeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="white" stopOpacity="1" />
                        <stop offset="100%" stopColor="white" stopOpacity="0.9" />
                      </linearGradient>
                      <filter id="glow">
                        <feGaussianBlur stdDeviation="4" result="coloredBlur" />
                        <feMerge>
                          <feMergeNode in="coloredBlur" />
                          <feMergeNode in="SourceGraphic" />
                        </feMerge>
                      </filter>
                    </defs>
                    {/* Network nodes representing global connections */}
                    <circle cx="80" cy="100" r="12" fill="url(#routeGrad)" filter="url(#glow)" />
                    <circle cx="320" cy="120" r="12" fill="url(#routeGrad)" filter="url(#glow)" />
                    <circle cx="200" cy="200" r="16" fill="url(#routeGrad)" filter="url(#glow)" />
                    <circle cx="100" cy="300" r="10" fill="url(#routeGrad)" filter="url(#glow)" />
                    <circle cx="300" cy="280" r="10" fill="url(#routeGrad)" filter="url(#glow)" />
                    {/* Connecting routes */}
                    <path
                      d="M 80 100 Q 140 150 200 200"
                      stroke="url(#routeGrad)"
                      strokeWidth="2"
                      fill="none"
                      strokeDasharray="5,5"
                    />
                    <path
                      d="M 200 200 Q 260 160 320 120"
                      stroke="url(#routeGrad)"
                      strokeWidth="2"
                      fill="none"
                      strokeDasharray="5,5"
                    />
                    <path
                      d="M 200 200 Q 150 250 100 300"
                      stroke="url(#routeGrad)"
                      strokeWidth="2"
                      fill="none"
                      strokeDasharray="5,5"
                    />
                    <path
                      d="M 200 200 Q 250 240 300 280"
                      stroke="url(#routeGrad)"
                      strokeWidth="2"
                      fill="none"
                      strokeDasharray="5,5"
                    />
                  </svg>
                  <div className="absolute inset-0 bg-gradient-to-br from-white/10 via-transparent to-white/5 blur-3xl" />
                </div>
              </div>
            </div>
          </div>
        </Container>
      </section>

      {/* <CHANGE> Trust row with better spacing */}
      <section className="py-20 lg:py-24">
        <Container>
          <div className="relative">
            {/* Blurred gradient overlay behind text - extremely subtle */}
            <div className="absolute inset-0 -inset-x-4 -inset-y-2 bg-gradient-to-b from-black/[0.001] via-black/[0.001] to-black/[0.001] backdrop-blur-[1px] rounded-2xl pointer-events-none" />
            <div className="relative grid gap-6 sm:grid-cols-2 lg:grid-cols-5 py-4">
              {siteConfig.trust.map((item, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="text-center"
                >
                  <h3 className="mb-2 font-semibold">{item.title}</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">{item.description}</p>
                </motion.div>
              ))}
            </div>
          </div>
        </Container>
      </section>

      {/* <CHANGE> Featured service with visual distinction */}
      <section className="py-28 lg:py-36">
        <Container>
          <SectionHeading
            title="What We Do Best"
            subtitle="Specialized automotive sourcing across five core service areas"
            centered
            className="mb-16"
          />

          {/* Featured service */}
          {siteConfig.services
            .filter((s) => s.featured)
            .map((service) => (
              <GlassPanel key={service.slug} className="mb-8" hover>
                <div className="grid gap-8 lg:grid-cols-3">
                  <div className="lg:col-span-2">
                    <Badge variant="secondary" className="mb-4">
                      Featured Service
                    </Badge>
                    <h3 className="mb-3 text-3xl font-bold">{service.title}</h3>
                    <p className="mb-6 text-muted-foreground leading-relaxed">{service.description}</p>
                    <div className="grid gap-3 sm:grid-cols-2">
                      {service.features.map((feature, i) => (
                        <div key={i} className="flex items-start gap-2">
                          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                          <span className="text-sm">{feature}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div className="flex flex-col justify-between gap-4">
                    <div>
                      <p className="mb-2 text-sm font-medium text-muted-foreground">Best for</p>
                      <p className="font-medium">{service.forWho}</p>
                    </div>
                    <div>
                      <p className="mb-2 text-sm font-medium text-muted-foreground">Turnaround</p>
                      <p className="font-medium">{service.turnaround}</p>
                    </div>
                    <CTAButton href="/services" className="w-full">
                      View All Services
                    </CTAButton>
                  </div>
                </div>
              </GlassPanel>
            ))}

          {/* Other services grid */}
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {siteConfig.services
              .filter((s) => !s.featured)
              .map((service, i) => (
                <motion.div
                  key={service.slug}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                >
                  <Card className="h-full transition-all">
                    <CardContent className="p-6">
                      <Package className="mb-4 h-8 w-8 text-primary" />
                      <h3 className="mb-2 font-semibold">{service.title}</h3>
                      <p className="mb-4 text-sm text-muted-foreground leading-relaxed">{service.description}</p>
                      <Badge variant="outline" className="text-xs">
                        {service.turnaround}
                      </Badge>
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

      {/* <CHANGE> Glass panel for differentiator */}
      <section className="py-28 lg:py-36">
        <Container>
          <GlassPanel>
            <div className="grid gap-12 lg:grid-cols-2 lg:gap-16">
              <div>
                <Globe className="mb-6 h-12 w-12 text-primary" />
                <h2 className="mb-4 text-balance text-3xl font-bold tracking-tight sm:text-4xl">
                  {siteConfig.differentiator.headline}
                </h2>
                <p className="text-pretty text-muted-foreground leading-relaxed">
                  {siteConfig.differentiator.description}
                </p>
              </div>
              <div className="space-y-4">
                {siteConfig.differentiator.benefits.map((benefit, index) => (
                  <div
                    key={index}
                    className="flex items-start gap-3 rounded-lg p-4"
                  >
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10">
                      <CheckCircle2 className="h-4 w-4 text-primary" />
                    </div>
                    <p className="font-medium leading-relaxed">{benefit}</p>
                  </div>
                ))}
              </div>
            </div>
          </GlassPanel>
        </Container>
      </section>

      {/* Stats Section */}
      {siteConfig.stats && (
        <section className="py-20 lg:py-24">
          <Container>
            <SectionHeading title={siteConfig.stats.headline} centered className="mb-16" />
            <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
              {siteConfig.stats.items.map((stat, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, scale: 0.9 }}
                  whileInView={{ opacity: 1, scale: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="text-center"
                >
                  <p className="mb-2 text-4xl font-bold text-primary lg:text-5xl">{stat.value}</p>
                  <p className="text-sm text-muted-foreground">{stat.label}</p>
                </motion.div>
              ))}
            </div>
          </Container>
        </section>
      )}

      {/* <CHANGE> Improved testimonials layout */}
      <section className="py-28 lg:py-36">
        <Container>
          <SectionHeading
            title="Trusted by Workshops and Distributors"
            subtitle="Real feedback from clients who rely on our sourcing"
            centered
            className="mb-16"
          />
          <div className="grid gap-6 md:grid-cols-3">
            {siteConfig.testimonials.map((testimonial, i) => (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className={i === 0 ? "md:col-span-2" : ""}
              >
                <Card className="h-full">
                  <CardContent className="p-6">
                    <p className="mb-6 text-pretty italic text-muted-foreground leading-relaxed">
                      "{testimonial.quote}"
                    </p>
                    <div>
                      <p className="font-semibold">{testimonial.name}</p>
                      <p className="text-sm text-muted-foreground">{testimonial.role}</p>
                      {testimonial.company && <p className="text-xs text-muted-foreground">{testimonial.company}</p>}
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>

          {/* Logo row placeholder */}
          <div className="mt-16 pt-16">
            <p className="mb-8 text-center text-sm font-medium text-muted-foreground">
              Trusted by workshops and distributors across
            </p>
            <div className="flex flex-wrap items-center justify-center gap-8">
              {["UK", "Germany", "UAE", "Italy", "Spain", "France"].map((country) => (
                <div key={country} className="flex h-12 items-center rounded px-6 font-medium text-white">
                  {country}
                </div>
              ))}
            </div>
          </div>
        </Container>
      </section>

      {/* <CHANGE> Quote form with premium layout */}
      <section id="contact" className="py-28 lg:py-36">
        <Container>
          <div className="relative">
            {/* Blurred gradient overlay behind text - extremely subtle */}
            <div className="absolute inset-0 -inset-x-4 -inset-y-2 bg-gradient-to-b from-black/[0.03] via-black/[0.02] to-black/[0.03] backdrop-blur-[1px] rounded-2xl pointer-events-none" />
            <div className="relative">
              <SectionHeading
                title="Request Your Quote"
                subtitle="Tell us what you need. We'll respond with pricing, lead time, and shipping options within 4 business hours."
                centered
                className="mb-12"
              />
              <QuoteForm source="home-page" />
            </div>
          </div>
        </Container>
      </section>
    </>
  )
}
