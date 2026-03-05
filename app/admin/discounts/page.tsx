"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { ChevronLeft, Pencil, Plus, Trash2 } from "lucide-react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { RequireAuth } from "@/components/AuthProvider"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  createAdminDiscountCode,
  deleteAdminDiscountCode,
  getAdminCategories,
  getAdminDiscountCodes,
  updateAdminDiscountCode,
  type AdminCategory,
  type AdminDiscountCode,
  type DiscountCodeInput,
} from "@/lib/adminApi"

function AdminDiscountsContent() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [discounts, setDiscounts] = useState<AdminDiscountCode[]>([])
  const [categories, setCategories] = useState<AdminCategory[]>([])

  const [editingId, setEditingId] = useState<string | null>(null)
  const [code, setCode] = useState("")
  const [description, setDescription] = useState("")
  const [percentage, setPercentage] = useState("")
  const [active, setActive] = useState(true)
  const [appliesToAllProducts, setAppliesToAllProducts] = useState(true)
  const [categoryIds, setCategoryIds] = useState<number[]>([])

  useEffect(() => {
    async function loadData() {
      try {
        const [discountData, categoryData] = await Promise.all([
          getAdminDiscountCodes(),
          getAdminCategories(),
        ])
        setDiscounts(discountData)
        setCategories(categoryData.filter((category) => category.status !== "inactive"))
      } catch (loadError: any) {
        setError(loadError?.message || "Failed to load discounts")
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [])

  const sortedCategories = useMemo(
    () => [...categories].sort((left, right) => left.name.localeCompare(right.name)),
    [categories],
  )

  function resetForm() {
    setEditingId(null)
    setCode("")
    setDescription("")
    setPercentage("")
    setActive(true)
    setAppliesToAllProducts(true)
    setCategoryIds([])
  }

  function startEditing(discount: AdminDiscountCode) {
    setEditingId(discount.id)
    setCode(discount.code)
    setDescription(discount.description || "")
    setPercentage(discount.percentage.toString())
    setActive(discount.active)
    setAppliesToAllProducts(discount.appliesToAllProducts)
    setCategoryIds(discount.categoryIds || [])
    setError(null)
    setSuccess(null)
  }

  function toggleCategory(categoryId: number) {
    setCategoryIds((prev) =>
      prev.includes(categoryId) ? prev.filter((id) => id !== categoryId) : [...prev, categoryId],
    )
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)

    const normalizedCode = code.trim().toUpperCase()
    const parsedPercentage = Number.parseFloat(percentage)
    if (!normalizedCode) {
      setError("Discount code is required")
      return
    }
    if (!Number.isFinite(parsedPercentage) || parsedPercentage <= 0 || parsedPercentage > 100) {
      setError("Discount percentage must be between 0.01 and 100")
      return
    }
    if (!appliesToAllProducts && categoryIds.length === 0) {
      setError("Select at least one category or enable 'all products'")
      return
    }

    const payload: DiscountCodeInput = {
      code: normalizedCode,
      description: description.trim() || undefined,
      percentage: parsedPercentage,
      active,
      appliesToAllProducts,
      categoryIds: appliesToAllProducts ? [] : categoryIds,
    }

    try {
      setSaving(true)
      if (editingId) {
        const updated = await updateAdminDiscountCode(editingId, payload)
        setDiscounts((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
        setSuccess(`Updated ${updated.code}`)
      } else {
        const created = await createAdminDiscountCode(payload)
        setDiscounts((prev) => [created, ...prev])
        setSuccess(`Created ${created.code}`)
      }
      resetForm()
    } catch (saveError: any) {
      setError(saveError?.message || "Failed to save discount")
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string, codeValue: string) {
    if (!confirm(`Delete discount code ${codeValue}?`)) return

    setError(null)
    setSuccess(null)
    try {
      await deleteAdminDiscountCode(id)
      setDiscounts((prev) => prev.filter((item) => item.id !== id))
      if (editingId === id) resetForm()
      setSuccess(`Deleted ${codeValue}`)
    } catch (deleteError: any) {
      setError(deleteError?.message || "Failed to delete discount")
    }
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/admin" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Dashboard
          </Link>
        </div>

        <SectionHeading title="Discounts" subtitle="Manage cart discount codes and category scopes" className="mb-8" />

        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>{editingId ? "Edit Discount Code" : "Create Discount Code"}</CardTitle>
              <CardDescription>Percentage-based codes with optional category restrictions</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSave} className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="discount-code">Code *</Label>
                    <Input
                      id="discount-code"
                      value={code}
                      onChange={(e) => setCode(e.target.value.toUpperCase())}
                      placeholder="SAVE10"
                      maxLength={64}
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="discount-percentage">Discount % *</Label>
                    <Input
                      id="discount-percentage"
                      type="number"
                      step="0.01"
                      min="0.01"
                      max="100"
                      value={percentage}
                      onChange={(e) => setPercentage(e.target.value)}
                      placeholder="10"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="discount-description">Description</Label>
                  <Input
                    id="discount-description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Optional internal description"
                  />
                </div>

                <div className="flex flex-wrap items-center gap-6">
                  <div className="flex items-center gap-2">
                    <Switch id="discount-active" checked={active} onCheckedChange={setActive} />
                    <Label htmlFor="discount-active">Active</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <Switch
                      id="discount-all-products"
                      checked={appliesToAllProducts}
                      onCheckedChange={setAppliesToAllProducts}
                    />
                    <Label htmlFor="discount-all-products">Apply to all products</Label>
                  </div>
                </div>

                {!appliesToAllProducts && (
                  <div className="space-y-2 rounded-lg border p-3">
                    <Label>Allowed Categories</Label>
                    <div className="grid gap-2 sm:grid-cols-2">
                      {sortedCategories.map((category) => (
                        <label key={category.id} className="flex items-center gap-2 text-sm">
                          <Checkbox
                            checked={categoryIds.includes(category.id)}
                            onCheckedChange={() => toggleCategory(category.id)}
                          />
                          <span>{category.name}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex gap-2">
                  <Button type="submit" disabled={saving}>
                    <Plus className="mr-2 h-4 w-4" />
                    {saving ? "Saving..." : editingId ? "Update Code" : "Create Code"}
                  </Button>
                  {editingId && (
                    <Button type="button" variant="outline" onClick={resetForm} disabled={saving}>
                      Cancel Edit
                    </Button>
                  )}
                </div>
              </form>

              {error && <p className="mt-4 text-sm text-destructive">{error}</p>}
              {success && <p className="mt-4 text-sm text-green-600">{success}</p>}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Existing Codes</CardTitle>
              <CardDescription>Applied at cart and checkout</CardDescription>
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Code</TableHead>
                    <TableHead>Scope</TableHead>
                    <TableHead>Discount</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={5} className="py-8 text-center">
                        Loading...
                      </TableCell>
                    </TableRow>
                  ) : discounts.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} className="py-8 text-center text-muted-foreground">
                        No discount codes yet
                      </TableCell>
                    </TableRow>
                  ) : (
                    discounts.map((discount) => (
                      <TableRow key={discount.id}>
                        <TableCell className="font-medium">{discount.code}</TableCell>
                        <TableCell className="max-w-[200px]">
                          {discount.appliesToAllProducts ? (
                            <span>All products</span>
                          ) : (
                            <span className="line-clamp-2">{discount.categoryNames.join(", ") || "Specific categories"}</span>
                          )}
                        </TableCell>
                        <TableCell>{discount.percentage}%</TableCell>
                        <TableCell>
                          <Badge variant={discount.active ? "default" : "secondary"}>
                            {discount.active ? "Active" : "Inactive"}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="inline-flex items-center gap-2">
                            <Button type="button" variant="outline" size="icon" onClick={() => startEditing(discount)}>
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button
                              type="button"
                              variant="outline"
                              size="icon"
                              onClick={() => handleDelete(discount.id, discount.code)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </Container>
    </section>
  )
}

export default function AdminDiscountsPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <AdminDiscountsContent />
    </RequireAuth>
  )
}
