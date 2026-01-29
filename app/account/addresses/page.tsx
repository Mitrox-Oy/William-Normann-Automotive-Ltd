"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RequireAuth } from "@/components/AuthProvider"
import { getAddresses, createAddress, updateAddress, deleteAddress, setDefaultAddress, type Address } from "@/lib/accountApi"
import { MapPin, Plus, Edit, Trash2, Check, ChevronLeft } from "lucide-react"
import Link from "next/link"

function AddressesPageContent() {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingAddress, setEditingAddress] = useState<Address | null>(null)

  useEffect(() => {
    loadAddresses()
  }, [])

  async function loadAddresses() {
    try {
      setLoading(true)
      const data = await getAddresses()
      setAddresses(data)
    } catch (error) {
      console.error("Failed to load addresses:", error)
      setAddresses([])
    } finally {
      setLoading(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("Are you sure you want to delete this address?")) return
    
    try {
      await deleteAddress(id)
      setAddresses((prev) => prev.filter((addr) => addr.id !== id))
    } catch (error) {
      console.error("Failed to delete address:", error)
    }
  }

  async function handleSetDefault(id: string) {
    try {
      await setDefaultAddress(id)
      await loadAddresses()
    } catch (error) {
      console.error("Failed to set default address:", error)
    }
  }

  function handleEdit(address: Address) {
    setEditingAddress(address)
    setDialogOpen(true)
  }

  function handleNew() {
    setEditingAddress(null)
    setDialogOpen(true)
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/account" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Account
          </Link>
        </div>

        <div className="mb-8 flex items-center justify-between">
          <SectionHeading title="My Addresses" subtitle="Manage your delivery addresses" />
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button onClick={handleNew}>
                <Plus className="mr-2 h-4 w-4" />
                Add Address
              </Button>
            </DialogTrigger>
            <AddressDialog
              address={editingAddress}
              onSuccess={() => {
                loadAddresses()
                setDialogOpen(false)
              }}
            />
          </Dialog>
        </div>

        {loading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <Card key={i}>
                <CardContent className="p-6">
                  <Skeleton className="h-32 w-full" />
                </CardContent>
              </Card>
            ))}
          </div>
        ) : addresses.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {addresses.map((address) => (
              <Card key={address.id} className={address.isDefault ? "border-primary" : ""}>
                <CardContent className="p-6">
                  <div className="mb-4 flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold">{address.label}</h3>
                      {address.isDefault && (
                        <Badge variant="default" className="mt-1">
                          Default
                        </Badge>
                      )}
                    </div>
                    <div className="flex gap-1">
                      <Button variant="ghost" size="sm" onClick={() => handleEdit(address)}>
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDelete(address.id)}
                        disabled={address.isDefault}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>

                  <div className="space-y-1 text-sm text-muted-foreground">
                    <p className="font-medium text-foreground">{address.recipientName}</p>
                    <p>{address.street}</p>
                    <p>{address.city}, {address.state}</p>
                    <p>{address.postalCode}</p>
                    <p>{address.country}</p>
                    <p className="mt-2">{address.phone}</p>
                  </div>

                  {!address.isDefault && (
                    <Button
                      variant="outline"
                      size="sm"
                      className="mt-4 w-full"
                      onClick={() => handleSetDefault(address.id)}
                    >
                      <Check className="mr-2 h-4 w-4" />
                      Set as Default
                    </Button>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-16 text-center">
              <MapPin className="mb-4 h-16 w-16 text-muted-foreground" />
              <h3 className="mb-2 text-lg font-semibold">No addresses saved</h3>
              <p className="mb-6 text-sm text-muted-foreground">
                Add your delivery addresses for faster checkout
              </p>
              <Button onClick={handleNew}>
                <Plus className="mr-2 h-4 w-4" />
                Add Address
              </Button>
            </CardContent>
          </Card>
        )}
      </Container>
    </section>
  )
}

// Address Dialog Component
function AddressDialog({ address, onSuccess }: { address: Address | null; onSuccess: () => void }) {
  const [formData, setFormData] = useState({
    label: address?.label || "",
    recipientName: address?.recipientName || "",
    phone: address?.phone || "",
    street: address?.street || "",
    city: address?.city || "",
    state: address?.state || "",
    country: address?.country || "",
    postalCode: address?.postalCode || "",
    isDefault: address?.isDefault || false,
  })
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)

    try {
      if (address) {
        await updateAddress(address.id, formData)
      } else {
        await createAddress(formData)
      }
      onSuccess()
    } catch (error) {
      console.error("Failed to save address:", error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <DialogContent>
      <DialogHeader>
        <DialogTitle>{address ? "Edit Address" : "Add New Address"}</DialogTitle>
        <DialogDescription>
          {address ? "Update your delivery address details" : "Add a new delivery address"}
        </DialogDescription>
      </DialogHeader>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="label">Label</Label>
            <Input id="label" value={formData.label} onChange={(e) => setFormData({ ...formData, label: e.target.value })} required />
          </div>
          <div>
            <Label htmlFor="recipientName">Recipient Name</Label>
            <Input id="recipientName" value={formData.recipientName} onChange={(e) => setFormData({ ...formData, recipientName: e.target.value })} required />
          </div>
        </div>
        <div>
          <Label htmlFor="phone">Phone</Label>
          <Input id="phone" type="tel" value={formData.phone} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} required />
        </div>
        <div>
          <Label htmlFor="street">Street Address</Label>
          <Input id="street" value={formData.street} onChange={(e) => setFormData({ ...formData, street: e.target.value })} required />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="city">City</Label>
            <Input id="city" value={formData.city} onChange={(e) => setFormData({ ...formData, city: e.target.value })} required />
          </div>
          <div>
            <Label htmlFor="state">State/Province</Label>
            <Input id="state" value={formData.state} onChange={(e) => setFormData({ ...formData, state: e.target.value })} required />
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="postalCode">Postal Code</Label>
            <Input id="postalCode" value={formData.postalCode} onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })} required />
          </div>
          <div>
            <Label htmlFor="country">Country</Label>
            <Input id="country" value={formData.country} onChange={(e) => setFormData({ ...formData, country: e.target.value })} required />
          </div>
        </div>
        <DialogFooter>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Saving..." : address ? "Update" : "Add"} Address
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  )
}

export default function AddressesPage() {
  return (
    <RequireAuth allowedRoles={["customer", "owner"]}>
      <AddressesPageContent />
    </RequireAuth>
  )
}

