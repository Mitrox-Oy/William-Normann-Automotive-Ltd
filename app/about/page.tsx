import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { LeadForm } from "@/components/lead-form"
import { siteConfig } from "@/content/site"
import { Card, CardContent } from "@/components/ui/card"
import { Mail, Phone, MapPin } from "lucide-react"

export const metadata = {
  title: "About & Contact",
  description:
    "Learn about William Normann Automotive Ltd and get in touch with our team for automotive parts sourcing and global market connections.",
}

export default function AboutPage() {
  return (
    <>
      {/* Header */}
      <section className="py-16 lg:py-20">
        <Container>
          <SectionHeading
            title="About Us"
            subtitle="Connecting automotive markets with expertise and integrity"
            centered
          />
        </Container>
      </section>

      {/* Story */}
      <section className="py-20 lg:py-28">
        <Container size="narrow">
          <div className="prose prose-lg mx-auto">
            {siteConfig.about.story.split("\n\n").map((paragraph, index) => (
              <p key={index} className="text-muted-foreground leading-relaxed">
                {paragraph}
              </p>
            ))}
          </div>
        </Container>
      </section>

      {/* Values */}
      <section className="py-20 lg:py-28">
        <Container>
          <SectionHeading
            title="Our Values"
            subtitle="The principles that guide everything we do"
            centered
            className="mb-16"
          />
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {siteConfig.about.values.map((value, index) => (
              <Card key={index}>
                <CardContent className="p-6">
                  <h3 className="mb-2 font-semibold">{value.title}</h3>
                  <p className="text-sm text-muted-foreground">{value.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </Container>
      </section>

      {/* Contact Information */}
      <section className="py-20 lg:py-28">
        <Container>
          <SectionHeading
            title="Get in Touch"
            subtitle="We're here to help with your automotive sourcing needs"
            centered
            className="mb-16"
          />

          <div className="mx-auto grid max-w-4xl gap-8 lg:grid-cols-3">
            <Card>
              <CardContent className="flex flex-col items-center p-6 text-center">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Mail className="h-6 w-6 text-primary" />
                </div>
                <h3 className="mb-2 font-semibold">Email</h3>
                <a
                  href={`mailto:${siteConfig.contact.email}`}
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  {siteConfig.contact.email}
                </a>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex flex-col items-center p-6 text-center">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Phone className="h-6 w-6 text-primary" />
                </div>
                <h3 className="mb-2 font-semibold">Phone</h3>
                <a
                  href={`tel:${siteConfig.contact.phone}`}
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  {siteConfig.contact.phone}
                </a>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex flex-col items-center p-6 text-center">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <MapPin className="h-6 w-6 text-primary" />
                </div>
                <h3 className="mb-2 font-semibold">Office</h3>
                <p className="text-sm text-muted-foreground">
                  {siteConfig.contact.address.line1}
                  <br />
                  {siteConfig.contact.address.line2}
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Map Placeholder */}
          <div className="mx-auto mt-12 max-w-4xl">
            <div className="aspect-video w-full overflow-hidden rounded-lg bg-muted">
              <div className="flex h-full items-center justify-center text-muted-foreground">
                Google Maps embed placeholder
              </div>
            </div>
          </div>
        </Container>
      </section>

      {/* Contact Form */}
      <section className="py-20 lg:py-28">
        <Container size="narrow">
          <SectionHeading
            title="Send us a Message"
            subtitle="Fill out the form below and we'll get back to you within 24 hours"
            centered
            className="mb-12"
          />
          <LeadForm source="about-page" />
        </Container>
      </section>
    </>
  )
}
