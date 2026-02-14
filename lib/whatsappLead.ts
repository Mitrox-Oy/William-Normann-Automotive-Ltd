import { siteConfig } from "@/content/site"

export interface WhatsAppLeadPayload {
    source: string
    name: string
    email: string
    phone?: string | null
    interest?: string | null
    message?: string | null
    product?: string | null
    partNumber?: string | null
    quantity?: string | null
}

function normalizePhoneForWa(phone: string) {
    return phone.replace(/[^\d]/g, "")
}

function safeLine(value?: string | null) {
    return value && value.trim().length > 0 ? value.trim() : "-"
}

function formatLeadText(payload: WhatsAppLeadPayload) {
    const lines = [
        "🚗 New Quote Request",
        `Source: ${safeLine(payload.source)}`,
        `Name: ${safeLine(payload.name)}`,
        `Email: ${safeLine(payload.email)}`,
        `Phone: ${safeLine(payload.phone)}`,
    ]

    if (payload.interest && payload.interest.trim()) {
        lines.push(`Interest: ${payload.interest.trim()}`)
    }

    if (payload.product && payload.product.trim()) {
        lines.push(`Product: ${payload.product.trim()}`)
    }

    if (payload.partNumber && payload.partNumber.trim()) {
        lines.push(`Part Number: ${payload.partNumber.trim()}`)
    }

    if (payload.quantity && payload.quantity.trim()) {
        lines.push(`Quantity: ${payload.quantity.trim()}`)
    }

    lines.push("Message:")
    lines.push(safeLine(payload.message))

    return lines.join("\n")
}

export function openLeadInWhatsApp(payload: WhatsAppLeadPayload) {
    const recipient = normalizePhoneForWa(siteConfig.contact.phone)
    const text = formatLeadText(payload)
    const url = `https://wa.me/${recipient}?text=${encodeURIComponent(text)}`

    const popup = window.open(url, "_blank", "noopener,noreferrer")
    if (!popup) {
        window.location.href = url
    }
}