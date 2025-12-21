import { NextResponse } from "next/server"
import { z } from "zod"

// Validation schema using Zod
const leadSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters"),
  email: z.string().email("Invalid email address"),
  phone: z.string().optional(),
  interest: z.string().min(1, "Please select an interest"),
  message: z.string().min(10, "Message must be at least 10 characters"),
  consent: z.literal("on", {
    errorMap: () => ({ message: "You must consent to be contacted" }),
  }),
  source: z.string().optional(),
  website: z.string().optional(), // Honeypot field
})

// In-memory storage for development (replace with database in production)
const leads: any[] = []

export async function POST(request: Request) {
  try {
    const body = await request.json()

    // Check honeypot field
    if (body.website) {
      console.log("[v0] Spam detected via honeypot field")
      // Return success to not alert spam bots
      return NextResponse.json({ success: true })
    }

    // Validate input
    const validatedData = leadSchema.parse(body)

    // Create lead object
    const lead = {
      id: Date.now().toString(),
      ...validatedData,
      createdAt: new Date().toISOString(),
      ip: request.headers.get("x-forwarded-for") || "unknown",
    }

    // Store lead
    leads.push(lead)

    console.log("[v0] New lead received:", {
      name: lead.name,
      email: lead.email,
      interest: lead.interest,
      source: lead.source,
    })

    // TODO: In production, you would:
    // 1. Store in a database (Supabase, Neon, etc.)
    // 2. Send notification email to sales team
    // 3. Add to CRM (via API)
    // 4. Send confirmation email to customer
    // 5. Add spam protection (Turnstile, reCAPTCHA)

    return NextResponse.json(
      {
        success: true,
        message: "Thank you! We'll get back to you within 24 hours.",
      },
      { status: 200 },
    )
  } catch (error) {
    console.error("[v0] Lead submission error:", error)

    if (error instanceof z.ZodError) {
      return NextResponse.json(
        {
          success: false,
          error: "Validation error",
          details: error.errors,
        },
        { status: 400 },
      )
    }

    return NextResponse.json(
      {
        success: false,
        error: "Failed to submit form. Please try again.",
      },
      { status: 500 },
    )
  }
}

// Optional: GET endpoint to view leads in development
export async function GET() {
  return NextResponse.json({
    total: leads.length,
    leads: leads.map((lead) => ({
      id: lead.id,
      name: lead.name,
      email: lead.email,
      interest: lead.interest,
      source: lead.source,
      createdAt: lead.createdAt,
    })),
  })
}
