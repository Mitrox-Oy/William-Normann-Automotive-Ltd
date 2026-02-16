import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { QuoteForm } from "@/components/quote-form"
import { siteConfig } from "@/content/site"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { Check } from "lucide-react"
import type { Metadata } from 'next'
import { canonicalUrl, SITE_NAME, LOGO_URL } from '@/lib/seo/config'

export const metadata: Metadata = {
  title: "Services",
  description:
    "Comprehensive automotive solutions including parts sourcing, wholesale distribution, market connections, and technical consulting.",
  alternates: { canonical: canonicalUrl('/services') },
  openGraph: {
    type: 'website',
    url: canonicalUrl('/services'),
    title: `Services | ${SITE_NAME}`,
    description: 'Comprehensive automotive solutions including parts sourcing, wholesale distribution, market connections, and technical consulting.',
    siteName: SITE_NAME,
    images: [LOGO_URL],
  },
  twitter: {
    card: 'summary_large_image',
    title: `Services | ${SITE_NAME}`,
    description: 'Comprehensive automotive solutions including parts sourcing, wholesale distribution, market connections, and technical consulting.',
  },
}

export default function ServicesPage() {
  return (
    <>
      {/* Header */}
      <section className="py-16 lg:py-20">
        <Container>
          <SectionHeading
            title="Our Services"
            subtitle="Comprehensive automotive solutions for enthusiasts, workshops, and distributors worldwide"
            centered
          />
        </Container>
      </section>

      {/* Services Detail */}
      <section className="py-20 lg:py-28">
        <Container>
          <div className="space-y-6">
            {siteConfig.services.map((service) => (
              <Card key={service.slug} id={service.slug}>
                <CardHeader>
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="flex-1">
                      <CardTitle className="text-2xl">{service.title}</CardTitle>
                      <CardDescription className="mt-2">{service.description}</CardDescription>
                    </div>
                    <div className="flex flex-col gap-2">
                      <Badge variant="secondary">{service.forWho}</Badge>
                      <Badge variant="outline">⏱ {service.turnaround}</Badge>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <Accordion type="single" collapsible className="w-full">
                    <AccordionItem value="features">
                      <AccordionTrigger>What's included</AccordionTrigger>
                      <AccordionContent>
                        <ul className="grid gap-3 sm:grid-cols-2">
                          {service.features.map((feature, index) => (
                            <li key={index} className="flex items-start gap-2">
                              <Check className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                              <span className="text-sm">{feature}</span>
                            </li>
                          ))}
                        </ul>
                      </AccordionContent>
                    </AccordionItem>
                  </Accordion>
                </CardContent>
              </Card>
            ))}
          </div>
        </Container>
      </section>

      {/* CTA Section */}
      <section className="py-28 lg:py-36">
        <Container>
          <SectionHeading
            title="Need Something Specific?"
            subtitle="Tell us your requirements and we'll source the best solution"
            centered
            className="mb-12"
          />
          <QuoteForm source="services-page" />
        </Container>
      </section>
    </>
  )
}
