import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import type { Metadata } from 'next'
import { canonicalUrl, SITE_NAME } from '@/lib/seo/config'

export const metadata: Metadata = {
    title: "Privacy Policy",
    description: "Privacy policy for William Automotive.",
    alternates: { canonical: canonicalUrl('/privacy') },
    robots: { index: true, follow: true },
}

export default function PrivacyPage() {
    return (
        <section className="py-24 lg:py-32">
            <Container>
                <SectionHeading title="Privacy Policy" subtitle="How we collect, use, and protect your information" />

                <div className="mt-10 space-y-8 text-sm leading-relaxed text-muted-foreground">
                    <div>
                        <h2 className="text-base font-semibold text-foreground">Overview</h2>
                        <p className="mt-2">
                            We collect only the information needed to provide quotes, fulfill orders, manage accounts,
                            and improve our services. We do not sell personal data.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Information We Collect</h2>
                        <ul className="mt-2 list-disc space-y-2 pl-5">
                            <li>Contact details (name, email, phone) for quotes and account support.</li>
                            <li>Shipping and billing details for order fulfillment.</li>
                            <li>Order history and preferences to improve service quality.</li>
                            <li>Technical data (device, browser, IP) for security and analytics.</li>
                        </ul>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">How We Use Data</h2>
                        <ul className="mt-2 list-disc space-y-2 pl-5">
                            <li>Process inquiries, quotes, and orders.</li>
                            <li>Provide customer support and service updates.</li>
                            <li>Comply with legal and regulatory requirements.</li>
                            <li>Prevent fraud and maintain platform security.</li>
                        </ul>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Data Sharing</h2>
                        <p className="mt-2">
                            We share data only with service providers necessary to deliver our services (e.g., payment,
                            logistics, and hosting). These providers are contractually required to safeguard your data.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Your Choices</h2>
                        <p className="mt-2">
                            You may request access, updates, or deletion of your personal data. Contact us and we will
                            respond promptly, subject to legal obligations.
                        </p>
                    </div>

                    <div>
                        <h2 className="text-base font-semibold text-foreground">Contact</h2>
                        <p className="mt-2">
                            For privacy inquiries, please contact our support team via the contact form or your account
                            dashboard.
                        </p>
                    </div>
                </div>
            </Container>
        </section>
    )
}
