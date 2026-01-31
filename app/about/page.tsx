import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { LeadForm } from "@/components/lead-form"
import { siteConfig } from "@/content/site"
import { Card, CardContent } from "@/components/ui/card"
import { Mail, Phone, MapPin, Package, Shield, Star, Zap, Globe, Instagram, MessageCircle, User } from "lucide-react"

export const metadata = {
  title: "About & Contact",
  description:
    "Learn about William Automotive and get in touch with our team for automotive parts sourcing and global market connections.",
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
          <h2 className="text-3xl font-bold mb-8 text-center">Our Story: From Passion for Cars to International Trade</h2>
          <div className="prose prose-lg mx-auto mb-16">
            <p className="text-muted-foreground leading-relaxed mb-6">
              Our company was born from a genuine passion for cars combined with the excitement of international trade. As car industry experts and enthusiasts ourselves, we quickly realized that Dubai offers an exceptionally high-quality selection of vehicles and parts – but exporting them to Europe is challenging without the right connections and know-how.
            </p>
            <p className="text-muted-foreground leading-relaxed">
              We've built direct relationships with trusted sellers and service providers in Dubai, ensuring every product's origin and condition are thoroughly verified. We work only with reliable shipping partners and maintain close cooperation with customs authorities to make the process smooth.
            </p>
          </div>

          <h3 className="text-2xl font-bold mb-8 text-center">We operate flexibly – our customers include:</h3>
          <div className="grid gap-6 sm:grid-cols-3 mb-12">
            <Card>
              <CardContent className="p-6">
                <User className="mb-4 h-6 w-6 text-primary" />
                <p className="text-sm text-muted-foreground">Car enthusiasts looking for something special and unique</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <Star className="mb-4 h-6 w-6 text-primary" />
                <p className="text-sm text-muted-foreground">Car dealerships wanting to stand out with a distinctive selection</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-6">
                <Package className="mb-4 h-6 w-6 text-primary" />
                <p className="text-sm text-muted-foreground">Businesses seeking cost-effective spare parts solutions</p>
              </CardContent>
            </Card>
          </div>

          <p className="text-center text-lg text-muted-foreground leading-relaxed">
            Customer satisfaction is our most important measure of success. The idea was born in Finland – but our focus is on the global car market.
          </p>
        </Container>
      </section>

      {/* Values */}
      <section className="py-20 lg:py-28">
        <Container>
          <SectionHeading
            title="Our Values"
            centered
            className="mb-16"
          />
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-5">
            {siteConfig.about.values.map((value, index) => {
              const icons = [Shield, Star, Zap, Globe]
              const Icon = icons[index]
              return (
                <Card key={index}>
                  <CardContent className="p-6">
                    <Icon className="mb-4 h-6 w-6 text-primary" />
                    <h3 className="mb-2 font-semibold">{value.title}</h3>
                    <p className="text-sm text-muted-foreground">{value.description}</p>
                  </CardContent>
                </Card>
              )
            })}
            {siteConfig.services
              .filter((s) => s.slug === "warranty")
              .map((service) => (
                <Card key={service.slug}>
                  <CardContent className="p-6">
                    <Package className="mb-4 h-6 w-6 text-primary" />
                    <h3 className="mb-2 font-semibold">{service.title}</h3>
                    <p className="text-sm text-muted-foreground">{service.description}</p>
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

          <div className="mx-auto grid max-w-5xl gap-8 lg:grid-cols-5">
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
                <h3 className="mb-2 font-semibold">UAE / Arthur</h3>
                <a
                  href="tel:+971585347970"
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  +971585347970
                </a>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex flex-col items-center p-6 text-center">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Phone className="h-6 w-6 text-primary" />
                </div>
                <h3 className="mb-2 font-semibold">FIN / Arthur</h3>
                <a
                  href="tel:+358440127970"
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  +358440127970
                </a>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex flex-col items-center p-6 text-center">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <MessageCircle className="h-6 w-6 text-primary" />
                </div>
                <h3 className="mb-2 font-semibold">WhatsApp</h3>
                <a
                  href="https://wa.me/971585347970"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  Chat with us
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

          {/* Map */}
          <div className="mx-auto mt-12 max-w-3xl">
            <div className="w-full overflow-hidden rounded-lg" style={{ height: '450px' }}>
              <iframe
                width="100%"
                height="100%"
                frameBorder="0"
                style={{ border: 0 }}
                src="https://www.google.com/maps/embed/v1/place?key=AIzaSyB2NIWI3Tv9iDPrlnowr_0ZqZWoAQydKJU&q=Dubai%20Investment%20Park%20-%20Dubai%20-%20United%20Arab%20Emirates&maptype=roadmap"
                allowFullScreen
              ></iframe>
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
