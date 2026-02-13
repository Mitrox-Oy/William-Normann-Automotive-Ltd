export const PRODUCT_TYPES = ["car", "part", "tool", "custom"] as const
export type ProductType = typeof PRODUCT_TYPES[number]

export const PRODUCT_CONDITIONS = ["new", "used", "refurbished"] as const
export type ProductCondition = typeof PRODUCT_CONDITIONS[number]

export const OEM_TYPES = ["oem", "aftermarket"] as const
export type OemType = typeof OEM_TYPES[number]

export const COMPATIBILITY_MODES = ["universal", "vehicle_specific"] as const
export type CompatibilityMode = typeof COMPATIBILITY_MODES[number]

export const FUEL_TYPES = [
  "petrol",
  "diesel",
  "hybrid",
  "electric",
  "plug_in_hybrid",
  "other",
] as const
export type FuelType = typeof FUEL_TYPES[number]

export const TRANSMISSION_TYPES = [
  "manual",
  "automatic",
  "semi_automatic",
  "cvt",
  "dual_clutch",
  "other",
] as const
export type TransmissionType = typeof TRANSMISSION_TYPES[number]

export const BODY_TYPES = [
  "sedan",
  "coupe",
  "hatchback",
  "wagon",
  "suv",
  "pickup",
  "van",
  "convertible",
  "other",
] as const
export type BodyType = typeof BODY_TYPES[number]

export const DRIVE_TYPES = ["fwd", "rwd", "awd", "4wd", "other"] as const
export type DriveType = typeof DRIVE_TYPES[number]

export const PART_POSITIONS = [
  "front",
  "rear",
  "left",
  "right",
  "upper",
  "lower",
  "inner",
  "outer",
] as const
export type PartPosition = typeof PART_POSITIONS[number]

export const POWER_SOURCES = [
  "manual",
  "electric_corded",
  "battery",
  "pneumatic",
  "hydraulic",
  "other",
] as const
export type PowerSource = typeof POWER_SOURCES[number]

export const DRIVE_SIZES = ['1/4"', '3/8"', '1/2"', '3/4"', '1"'] as const
export type DriveSize = typeof DRIVE_SIZES[number]

export const STYLE_TAGS = [
  "sport",
  "luxury",
  "off_road",
  "classic",
  "street",
  "track",
] as const
export type StyleTag = typeof STYLE_TAGS[number]

export const FINISH_TYPES = [
  "matte",
  "gloss",
  "satin",
  "chrome",
  "carbon",
  "raw",
  "painted",
] as const
export type FinishType = typeof FINISH_TYPES[number]

export const INSTALLATION_DIFFICULTIES = ["easy", "medium", "hard", "professional_only"] as const
export type InstallationDifficulty = typeof INSTALLATION_DIFFICULTIES[number]

export function csvToList(value?: string | null): string[] {
  if (!value) return []
  return value
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean)
}

export function listToCsv(values?: string[] | null): string | undefined {
  if (!values || values.length === 0) return undefined
  const normalized = values.map((entry) => entry.trim()).filter(Boolean)
  return normalized.length > 0 ? normalized.join(",") : undefined
}

export function normalizeCsvToken(value?: string | null): string {
  return (value || "").trim().toLowerCase()
}

export interface CategoryLike {
  id: number
  slug: string
  parentId?: number | string | null
  parent_id?: number | string | null
  parent?: { id?: number | string | null } | null
}

export function resolveProductTypeFromCategory(categoryId: number | string | undefined | null, categories: CategoryLike[]): ProductType | undefined {
  if (!categoryId || categories.length === 0) return undefined
  const id = typeof categoryId === "string" ? Number.parseInt(categoryId, 10) : categoryId
  if (!id || Number.isNaN(id)) return undefined

  const parseParentId = (category?: CategoryLike): number | undefined => {
    if (!category) return undefined
    const rawParentId = category.parentId ?? category.parent_id ?? category.parent?.id
    if (rawParentId === null || rawParentId === undefined || rawParentId === "") return undefined
    const parsed = typeof rawParentId === "number" ? rawParentId : Number.parseInt(String(rawParentId), 10)
    return Number.isFinite(parsed) ? parsed : undefined
  }

  const inferTypeFromSlug = (slug?: string): ProductType | undefined => {
    const normalizedSlug = (slug || "").toLowerCase()
    if (!normalizedSlug) return undefined
    if (normalizedSlug === "cars" || normalizedSlug.startsWith("cars-")) return "car"
    if (normalizedSlug === "parts" || normalizedSlug.startsWith("parts-")) return "part"
    if (normalizedSlug === "tools" || normalizedSlug.startsWith("tools-")) return "tool"
    if (normalizedSlug === "custom" || normalizedSlug.startsWith("custom-")) return "custom"
    return undefined
  }

  const byId = new Map<number, CategoryLike>()
  categories.forEach((category) => byId.set(category.id, category))

  let current = byId.get(id)
  while (current) {
    const inferredType = inferTypeFromSlug(current.slug)
    if (inferredType) return inferredType

    const parentId = parseParentId(current)
    if (!parentId) break

    const parent = byId.get(parentId)
    if (!parent) break
    current = parent
  }
  return undefined
}
