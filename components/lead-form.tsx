"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { siteConfig } from "@/content/site"
import { CheckCircle2 } from "lucide-react"

interface LeadFormProps {
  source?: string
}

export function LeadForm({ source = "general" }: LeadFormProps) {
  const [formState, setFormState] = useState<"idle" | "loading" | "success" | "error">("idle")
  const [consent, setConsent] = useState(false)

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setFormState("loading")

    const formData = new FormData(e.currentTarget)
    const data = {
      name: formData.get("name"),
      email: formData.get("email"),
      phone: formData.get("phone"),
      interest: formData.get("interest"),
      message: formData.get("message"),
      consent: formData.get("consent"),
      source,
      // Honeypot field for spam protection
      website: formData.get("website"),
    }

    try {
      const response = await fetch("/api/lead", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      })

      if (response.ok) {
        setFormState("success")
        // Reset form
        e.currentTarget.reset()
        setConsent(false)
      } else {
        setFormState("error")
      }
    } catch (error) {
      console.error("[v0] Lead form submission error:", error)
      setFormState("error")
    }
  }

  if (formState === "success") {
    return (
      <div className="rounded-lg border border-primary/20 bg-primary/5 p-8 text-center">
        <CheckCircle2 className="mx-auto mb-4 h-12 w-12 text-primary" />
        <h3 className="mb-2 text-xl font-semibold">Thank you for your inquiry!</h3>
        <p className="text-muted-foreground">
          We've received your message and will get back to you within 24 hours on business days.
        </p>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Honeypot field - hidden from users */}
      <input type="text" name="website" className="hidden" tabIndex={-1} autoComplete="off" />

      <div className="grid gap-6 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="name">
            Full Name <span className="text-destructive">*</span>
          </Label>
          <Input id="name" name="name" required placeholder="John Smith" />
        </div>

        <div className="space-y-2">
          <Label htmlFor="email">
            Email Address <span className="text-destructive">*</span>
          </Label>
          <Input id="email" name="email" type="email" required placeholder="john@example.com" />
        </div>
      </div>

      <div className="grid gap-6 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="phone">Phone Number (optional)</Label>
          <Input id="phone" name="phone" type="tel" placeholder="+44 20 1234 5678" />
        </div>

        <div className="space-y-2">
          <Label htmlFor="interest">
            I'm interested in <span className="text-destructive">*</span>
          </Label>
          <Select name="interest" required>
            <SelectTrigger id="interest">
              <SelectValue placeholder="Select an option" />
            </SelectTrigger>
            <SelectContent>
              {siteConfig.leadForm.interests.map((interest) => (
                <SelectItem key={interest} value={interest.toLowerCase().replace(/\s+/g, "-")}>
                  {interest}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="message">
          Tell us what you need <span className="text-destructive">*</span>
        </Label>
        <Textarea
          id="message"
          name="message"
          required
          placeholder="Please describe your requirements, including part numbers, quantities, or any specific details..."
          rows={5}
        />
      </div>

      <div className="flex items-start gap-3">
        <Checkbox
          id="consent"
          name="consent"
          required
          checked={consent}
          onCheckedChange={(checked) => setConsent(checked === true)}
        />
        <Label htmlFor="consent" className="text-sm leading-relaxed">
          I agree to be contacted by William Automotive regarding my inquiry and understand my data will be
          processed according to the privacy policy. <span className="text-destructive">*</span>
        </Label>
      </div>

      <div className="space-y-4">
        <Button type="submit" size="lg" className="w-full" disabled={formState === "loading"}>
          {formState === "loading" ? "Sending..." : "Send Inquiry"}
        </Button>
        <p className="text-center text-sm text-muted-foreground">{siteConfig.leadForm.responseTime}</p>
      </div>

      {formState === "error" && (
        <p className="text-center text-sm text-destructive">
          Something went wrong. Please try again or contact us directly.
        </p>
      )}
    </form>
  )
}
