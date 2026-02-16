import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import type { Metadata } from 'next'
import { canonicalUrl, SITE_NAME } from '@/lib/seo/config'

export const metadata: Metadata = {
    title: "Terms of Service",
    description: "Terms of service for William Automotive.",
    alternates: { canonical: canonicalUrl('/terms') },
    robots: { index: true, follow: true },
}

export default function TermsPage() {
    return (
        <section className="py-24 lg:py-32">
            <Container>
                <SectionHeading title="Terms of Service" subtitle="Conditions for using our services" />

                <div className="mt-10 space-y-8 text-sm leading-relaxed text-muted-foreground">
                    <div>
                        <h2 className="text-base font-semibold text-foreground">Agreement</h2>
                        <p className="mt-2">
                            By using our website or placing an order, you agree to these terms. If you do not agree,
                            please do not use the service.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Quotes & Orders</h2>
                        <p className="mt-2">
                            Quotes are valid for a limited time and are subject to supplier availability. Orders are
                            confirmed once payment is received and verified.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Pricing & Payment</h2>
                        <p className="mt-2">
                            Prices may change based on supplier costs, exchange rates, or shipping fees. Taxes, duties,
                            and customs fees are the customer’s responsibility unless explicitly included.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Returns & Warranty</h2>
                        <p className="mt-2">
                            Return eligibility and warranty terms vary by product type and supplier. We will provide
                            clear guidance during the order process.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Account Responsibilities</h2>
                        <p className="mt-2">
                            You are responsible for maintaining account security and ensuring the accuracy of your
                            information. Notify us immediately of unauthorized access.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Limitation of Liability</h2>
                        <p className="mt-2">
                            To the fullest extent permitted by law, we are not liable for indirect or consequential
                            damages arising from the use of our services.
                        </p>
                    </div>
                </div>
            </Container>
        </section>
    )
}
