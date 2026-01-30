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
import { CheckCircle2, FileText, Clock, Mail, CheckCheck } from "lucide-react"
import { GlassPanel } from "./glass-panel"

interface QuoteFormProps {
  source?: string
}

export function QuoteForm({ source = "general" }: QuoteFormProps) {
  const [formState, setFormState] = useState<"idle" | "loading" | "success" | "error">("idle")
  const [consent, setConsent] = useState(false)
  const [fileName, setFileName] = useState<string | null>(null)

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    setFileName(file ? file.name : null)
  }

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
        e.currentTarget.reset()
        setConsent(false)
        setFileName(null)
      } else {
        setFormState("error")
      }
    } catch (error) {
      console.error("[v0] Quote form submission error:", error)
      setFormState("error")
    }
  }

  if (formState === "success") {
    return (
      <GlassPanel className="text-center">
        <CheckCircle2 className="mx-auto mb-4 h-16 w-16 text-primary" />
        <h3 className="mb-3 text-2xl font-bold">Quote Request Received</h3>
        <p className="mb-6 text-muted-foreground leading-relaxed">
          Thank you for your inquiry. Our team is reviewing your requirements and will respond with a detailed quote
          within 24 hours.
        </p>
        <div className="rounded-lg p-4 text-sm text-muted-foreground">
          Check your email for confirmation and next steps.
        </div>
      </GlassPanel>
    )
  }

  return (
    <div className="grid gap-8 lg:grid-cols-3">
      {/* Form */}
      <div className="lg:col-span-2">
        <GlassPanel>
          <form onSubmit={handleSubmit} className="space-y-6">
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
                    <SelectValue placeholder="Select service" />
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
                Your Requirements <span className="text-destructive">*</span>
              </Label>
              <Textarea
                id="message"
                name="message"
                required
                placeholder="Part numbers, vehicle details, quantities, target prices, delivery location, timeline..."
                rows={5}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="file">Upload Part List / Screenshot (optional)</Label>
              <div className="relative">
                <Input
                  id="file"
                  name="file"
                  type="file"
                  accept=".pdf,.jpg,.jpeg,.png,.xlsx,.csv"
                  onChange={handleFileChange}
                  className="cursor-pointer file:mr-4 file:cursor-pointer file:rounded-md file:border-0 file:bg-primary file:px-4 file:py-2 file:text-sm file:font-medium file:text-primary-foreground"
                />
              </div>
              {fileName && (
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                  <FileText className="h-4 w-4" />
                  {fileName}
                </p>
              )}
              <p className="text-xs text-muted-foreground">Accepted: PDF, images, Excel (max 5MB)</p>
            </div>

            <div className="flex items-start gap-3 rounded-lg p-4">
              <Checkbox
                id="consent"
                name="consent"
                required
                checked={consent}
                onCheckedChange={(checked) => setConsent(checked === true)}
                className="mt-1"
              />
              <Label htmlFor="consent" className="text-sm leading-relaxed">
                I agree to be contacted by William Automotive regarding my quote request.{" "}
                <span className="text-destructive">*</span>
              </Label>
            </div>

            <Button type="submit" size="lg" className="w-full" disabled={formState === "loading"}>
              {formState === "loading" ? "Submitting..." : "Request Quote"}
            </Button>

            <p className="text-center text-xs text-muted-foreground">
              We respect your privacy. Your data is processed securely and never shared.
            </p>

            {formState === "error" && (
              <p className="text-center text-sm text-destructive">
                Something went wrong. Please try again or email us directly at {siteConfig.contact.email}
              </p>
            )}
          </form>
        </GlassPanel>
      </div>

      {/* What Happens Next */}
      <div className="space-y-6">
        <div>
          <h3 className="mb-4 text-lg font-semibold">What Happens Next</h3>
          <div className="space-y-4">
            <div className="flex gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10">
                <Mail className="h-5 w-5 text-white" strokeWidth={2} />
              </div>
              <div>
                <p className="mb-1 font-medium">1. Instant Confirmation</p>
                <p className="text-sm text-muted-foreground">You'll receive an email confirming we got your request.</p>
              </div>
            </div>

            <div className="flex gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10">
                <Clock className="h-5 w-5 text-white" strokeWidth={2} />
              </div>
              <div>
                <p className="mb-1 font-medium">2. Expert Review</p>
                <p className="text-sm text-muted-foreground">
                  Our team sources from our global network within 24 hours.
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10">
                <CheckCheck className="h-5 w-5 text-white" strokeWidth={2} />
              </div>
              <div>
                <p className="mb-1 font-medium">3. Detailed Quote</p>
                <p className="text-sm text-muted-foreground">
                  Receive part numbers, pricing, origin, lead time, and shipping options.
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="rounded-lg p-4">
          <p className="mb-1 text-sm font-medium text-white">Typical Response Time</p>
          <p className="text-2xl font-bold text-white">24 Hours</p>
          <p className="text-xs text-white">On business days</p>
        </div>
      </div>
    </div>
  )
}
