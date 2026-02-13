"use client"

import { useState, useEffect, useRef } from "react"
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { useCart } from "@/components/CartContext"
import {
  fetchProducts,
  fetchBrands,
  fetchCategoryChildren,
  formatCurrency,
  getAvailabilityBadge,
  type Product,
  type Category,
  type SearchParams
} from "@/lib/shopApi"
import {
  BODY_TYPES,
  COMPATIBILITY_MODES,
  DRIVE_TYPES,
  FUEL_TYPES,
  POWER_SOURCES,
  PRODUCT_CONDITIONS,
  TRANSMISSION_TYPES,
} from "@/lib/filterAttributes"
import { getPartsDeepCategories, getPartsSubcategories, resolvePartsBranch } from "@/lib/partsTaxonomy"
import { getImageUrl } from "@/lib/utils"
import { Search, ShoppingCart, Filter, X, ArrowLeft, ChevronDown } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { motion } from "framer-motion"

interface TopicProductListingProps {
  rootCategoryId: number
  topicSlug: string
  topicName?: string
  topicDescription?: string
  defaultCategoryId?: number
  defaultCategorySlug?: string
  showBackLink?: boolean
  backLinkHref?: string
  backLinkLabel?: string
}

interface CategoryOption {
  id: number
  label: string
}

function isQuoteOnlyProduct(product: Product): boolean {
  const flag = (product as any).quoteOnly ?? (product as any).quote_only
  if (typeof flag === "boolean") return flag
  if (typeof flag === "number") return flag === 1
  if (typeof flag === "string") {
    const normalized = flag.trim().toLowerCase()
    return normalized === "true" || normalized === "1" || normalized === "yes"
  }
  return false
}

function formatEnumLabel(value?: string): string {
  if (!value) return ""
  return value
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ")
}

function formatMileage(value?: number): string | undefined {
  if (value === undefined || value === null) return undefined
  return `${new Intl.NumberFormat("en-US").format(value)} km`
}

function formatHorsepowerFromKw(value?: number): string | undefined {
  if (value === undefined || value === null) return undefined
  const hp = Math.round(value * 1.34102209)
  return `${new Intl.NumberFormat("en-US").format(hp)} hp`
}

/**
 * Card excerpt policy:
 * - Use the first paragraph/line only.
 * - Collapse whitespace.
 * - Truncate to keep cards compact without fragile regex "metadata stripping".
 */
function getCardExcerpt(description?: string, maxChars: number = 150): string | undefined {
  if (!description) return undefined
  const raw = String(description)
  const firstParagraph = raw.split(/\n\s*\n/)[0] ?? raw
  const collapsed = firstParagraph.replace(/\s+/g, " ").trim()
  if (!collapsed) return undefined
  if (collapsed.length <= maxChars) return collapsed
  return `${collapsed.slice(0, Math.max(0, maxChars - 3)).trim()}...`
}

function resolvePartsMainFromCategorySlug(categorySlug?: string): string {
  if (!categorySlug) return ""
  const normalizedSlug = categorySlug.toLowerCase()
  if (!normalizedSlug.startsWith("parts-")) return ""

  const mainCandidates = [
    "engine-drivetrain",
    "suspension-steering",
    "brakes",
    "wheels-tires",
    "electrical-lighting",
    "exterior-body",
    "interior",
    "cooling-hvac",
    "maintenance-service",
  ]

  const matchedMain = mainCandidates.find(
    (candidate) => normalizedSlug === `parts-${candidate}` || normalizedSlug.startsWith(`parts-${candidate}-`),
  )

  return matchedMain || ""
}

export function TopicProductListing({
  rootCategoryId,
  topicSlug,
  topicName,
  topicDescription,
  defaultCategoryId,
  defaultCategorySlug,
  showBackLink = true,
  backLinkHref = "/shop",
  backLinkLabel = "Shop Home"
}: TopicProductListingProps) {
  const DISPLAY_PAGE_SIZE = 12
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const defaultPartsMainFromPage = topicSlug === "parts" ? resolvePartsMainFromCategorySlug(defaultCategorySlug) : ""

  const [products, setProducts] = useState<Product[]>([])
  const [categoryOptions, setCategoryOptions] = useState<CategoryOption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedCategory, setSelectedCategory] = useState<string>(defaultCategoryId?.toString() || "all")
  const [selectedAvailability, setSelectedAvailability] = useState<string>("all")
  const [selectedBrand, setSelectedBrand] = useState<string>("all")
  const [selectedCondition, setSelectedCondition] = useState<string>("all")
  const [minPrice, setMinPrice] = useState<string>("")
  const [maxPrice, setMaxPrice] = useState<string>("")

  const [vehicleMake, setVehicleMake] = useState("")
  const [vehicleModel, setVehicleModel] = useState("")
  const [yearMin, setYearMin] = useState("")
  const [yearMax, setYearMax] = useState("")
  const [mileageMin, setMileageMin] = useState("")
  const [mileageMax, setMileageMax] = useState("")
  const [fuelType, setFuelType] = useState("all")
  const [transmission, setTransmission] = useState("all")
  const [bodyType, setBodyType] = useState("all")
  const [driveType, setDriveType] = useState("all")
  const [compatibilityMode, setCompatibilityMode] = useState("all")
  const [compatibleMake, setCompatibleMake] = useState("")
  const [compatibleModel, setCompatibleModel] = useState("")
  const [compatibleYear, setCompatibleYear] = useState("")
  const [oemType, setOemType] = useState("all")
  const [partCategory, setPartCategory] = useState("")
  const [partsMain, setPartsMain] = useState(defaultPartsMainFromPage)
  const [partsSub, setPartsSub] = useState("")
  const [partsDeep, setPartsDeep] = useState("")
  const [wheelDiameterMin, setWheelDiameterMin] = useState("")
  const [wheelDiameterMax, setWheelDiameterMax] = useState("")
  const [wheelWidthMin, setWheelWidthMin] = useState("")
  const [wheelWidthMax, setWheelWidthMax] = useState("")
  const [wheelOffsetMin, setWheelOffsetMin] = useState("")
  const [wheelOffsetMax, setWheelOffsetMax] = useState("")
  const [centerBoreMin, setCenterBoreMin] = useState("")
  const [centerBoreMax, setCenterBoreMax] = useState("")
  const [wheelBoltPattern, setWheelBoltPattern] = useState("")
  const [wheelMaterial, setWheelMaterial] = useState("")
  const [wheelColor, setWheelColor] = useState("")
  const [hubCentricRingsNeeded, setHubCentricRingsNeeded] = useState("all")
  const [engineType, setEngineType] = useState("")
  const [engineDisplacementMin, setEngineDisplacementMin] = useState("")
  const [engineDisplacementMax, setEngineDisplacementMax] = useState("")
  const [engineCylinders, setEngineCylinders] = useState("")
  const [enginePowerMin, setEnginePowerMin] = useState("")
  const [enginePowerMax, setEnginePowerMax] = useState("")
  const [turboType, setTurboType] = useState("")
  const [flangeType, setFlangeType] = useState("")
  const [wastegateType, setWastegateType] = useState("")
  const [rotorDiameterMin, setRotorDiameterMin] = useState("")
  const [rotorDiameterMax, setRotorDiameterMax] = useState("")
  const [padCompound, setPadCompound] = useState("")
  const [adjustableHeight, setAdjustableHeight] = useState("all")
  const [adjustableDamping, setAdjustableDamping] = useState("all")
  const [lightingVoltage, setLightingVoltage] = useState("")
  const [bulbType, setBulbType] = useState("")
  const [toolCategory, setToolCategory] = useState("")
  const [powerSource, setPowerSource] = useState("all")
  const [finish, setFinish] = useState("all")
  const [streetLegal, setStreetLegal] = useState("all")
  const [styleTag, setStyleTag] = useState("")

  const [sortBy, setSortBy] = useState<NonNullable<SearchParams['sortBy']>>("newest")
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [showMobileFilters, setShowMobileFilters] = useState(false)
  const [filtersCollapsed, setFiltersCollapsed] = useState(true)
  const [brands, setBrands] = useState<string[]>([])
  const skipInitialPageReset = useRef(true)

  const { addItem } = useCart()

  // Load categories for this topic (full descendant tree under root category)
  useEffect(() => {
    const loadCategoryOptions = async () => {
      try {
        const options: CategoryOption[] = []

        const collect = async (parentId: number, depth: number) => {
          const children = await fetchCategoryChildren(parentId)
          const sortedChildren = [...children].sort((left, right) =>
            left.name.localeCompare(right.name, undefined, { sensitivity: "base" }),
          )

          for (const child of sortedChildren) {
            const prefix = depth === 0 ? "" : `${"— ".repeat(depth)} `
            options.push({ id: child.id, label: `${prefix}${child.name}` })
            await collect(child.id, depth + 1)
          }
        }

        await collect(rootCategoryId, 0)
        setCategoryOptions(options)
      } catch (err) {
        console.error('Failed to load categories:', err)
        setCategoryOptions([])
      }
    }

    loadCategoryOptions()
  }, [rootCategoryId])

  // Load brands scoped to this topic
  useEffect(() => {
    fetchBrands(rootCategoryId)
      .then(setBrands)
      .catch((err) => console.error('Failed to load brands:', err))
  }, [rootCategoryId])

  // Initialize filter state from URL query parameters.
  useEffect(() => {
    const get = (key: string) => searchParams.get(key)
    setSearchQuery(get("q") || "")
    setSelectedCategory(get("category") || defaultCategoryId?.toString() || "all")
    setSelectedAvailability(get("availability") || "all")
    setSelectedBrand(get("brand") || "all")
    setSelectedCondition(get("condition") || "all")
    setMinPrice(get("minPrice") || "")
    setMaxPrice(get("maxPrice") || "")
    setVehicleMake(get("make") || "")
    setVehicleModel(get("model") || "")
    setYearMin(get("yearMin") || "")
    setYearMax(get("yearMax") || "")
    setMileageMin(get("mileageMin") || "")
    setMileageMax(get("mileageMax") || "")
    setFuelType(get("fuelType") || "all")
    setTransmission(get("transmission") || "all")
    setBodyType(get("bodyType") || "all")
    setDriveType(get("driveType") || "all")
    setCompatibilityMode(get("compatibilityMode") || "all")
    setCompatibleMake(get("compatibleMake") || "")
    setCompatibleModel(get("compatibleModel") || "")
    setCompatibleYear(get("compatibleYear") || "")
    setOemType(get("oemType") || "all")
    setPartCategory(get("partCategory") || "")
    setPartsMain(get("partsMain") || defaultPartsMainFromPage)
    setPartsSub(get("partsSub") || "")
    setPartsDeep(get("partsDeep") || "")
    setWheelDiameterMin(get("wheelDiameterMin") || "")
    setWheelDiameterMax(get("wheelDiameterMax") || "")
    setWheelWidthMin(get("wheelWidthMin") || "")
    setWheelWidthMax(get("wheelWidthMax") || "")
    setWheelOffsetMin(get("wheelOffsetMin") || "")
    setWheelOffsetMax(get("wheelOffsetMax") || "")
    setCenterBoreMin(get("centerBoreMin") || "")
    setCenterBoreMax(get("centerBoreMax") || "")
    setWheelBoltPattern(get("wheelBoltPattern") || "")
    setWheelMaterial(get("wheelMaterial") || "")
    setWheelColor(get("wheelColor") || "")
    setHubCentricRingsNeeded(get("hubCentricRingsNeeded") || "all")
    setEngineType(get("engineType") || "")
    setEngineDisplacementMin(get("engineDisplacementMin") || "")
    setEngineDisplacementMax(get("engineDisplacementMax") || "")
    setEngineCylinders(get("engineCylinders") || "")
    setEnginePowerMin(get("enginePowerMin") || "")
    setEnginePowerMax(get("enginePowerMax") || "")
    setTurboType(get("turboType") || "")
    setFlangeType(get("flangeType") || "")
    setWastegateType(get("wastegateType") || "")
    setRotorDiameterMin(get("rotorDiameterMin") || "")
    setRotorDiameterMax(get("rotorDiameterMax") || "")
    setPadCompound(get("padCompound") || "")
    setAdjustableHeight(get("adjustableHeight") || "all")
    setAdjustableDamping(get("adjustableDamping") || "all")
    setLightingVoltage(get("lightingVoltage") || "")
    setBulbType(get("bulbType") || "")
    setToolCategory(get("toolCategory") || "")
    setPowerSource(get("powerSource") || "all")
    setFinish(get("finish") || "all")
    setStreetLegal(get("streetLegal") || "all")
    setStyleTag(get("styleTag") || "")
    setSortBy((get("sortBy") as SearchParams["sortBy"]) || "newest")
    setPage(Math.max(1, Number.parseInt(get("page") || "1", 10) || 1))
  }, [searchParams, defaultCategoryId, defaultPartsMainFromPage])

  // Keep URL query params in sync with current filters.
  useEffect(() => {
    const params = new URLSearchParams()
    if (searchQuery) params.set("q", searchQuery)
    if (selectedCategory && selectedCategory !== "all") params.set("category", selectedCategory)
    if (selectedAvailability !== "all") params.set("availability", selectedAvailability)
    if (selectedBrand !== "all") params.set("brand", selectedBrand)
    if (selectedCondition !== "all") params.set("condition", selectedCondition)
    if (minPrice) params.set("minPrice", minPrice)
    if (maxPrice) params.set("maxPrice", maxPrice)
    if (vehicleMake) params.set("make", vehicleMake)
    if (vehicleModel) params.set("model", vehicleModel)
    if (yearMin) params.set("yearMin", yearMin)
    if (yearMax) params.set("yearMax", yearMax)
    if (mileageMin) params.set("mileageMin", mileageMin)
    if (mileageMax) params.set("mileageMax", mileageMax)
    if (fuelType !== "all") params.set("fuelType", fuelType)
    if (transmission !== "all") params.set("transmission", transmission)
    if (bodyType !== "all") params.set("bodyType", bodyType)
    if (driveType !== "all") params.set("driveType", driveType)
    if (compatibilityMode !== "all") params.set("compatibilityMode", compatibilityMode)
    if (compatibleMake) params.set("compatibleMake", compatibleMake)
    if (compatibleModel) params.set("compatibleModel", compatibleModel)
    if (compatibleYear) params.set("compatibleYear", compatibleYear)
    if (oemType !== "all") params.set("oemType", oemType)
    if (partCategory) params.set("partCategory", partCategory)
    if (partsMain) params.set("partsMain", partsMain)
    if (partsSub) params.set("partsSub", partsSub)
    if (partsDeep) params.set("partsDeep", partsDeep)
    if (wheelDiameterMin) params.set("wheelDiameterMin", wheelDiameterMin)
    if (wheelDiameterMax) params.set("wheelDiameterMax", wheelDiameterMax)
    if (wheelWidthMin) params.set("wheelWidthMin", wheelWidthMin)
    if (wheelWidthMax) params.set("wheelWidthMax", wheelWidthMax)
    if (wheelOffsetMin) params.set("wheelOffsetMin", wheelOffsetMin)
    if (wheelOffsetMax) params.set("wheelOffsetMax", wheelOffsetMax)
    if (centerBoreMin) params.set("centerBoreMin", centerBoreMin)
    if (centerBoreMax) params.set("centerBoreMax", centerBoreMax)
    if (wheelBoltPattern) params.set("wheelBoltPattern", wheelBoltPattern)
    if (wheelMaterial) params.set("wheelMaterial", wheelMaterial)
    if (wheelColor) params.set("wheelColor", wheelColor)
    if (hubCentricRingsNeeded !== "all") params.set("hubCentricRingsNeeded", hubCentricRingsNeeded)
    if (engineType) params.set("engineType", engineType)
    if (engineDisplacementMin) params.set("engineDisplacementMin", engineDisplacementMin)
    if (engineDisplacementMax) params.set("engineDisplacementMax", engineDisplacementMax)
    if (engineCylinders) params.set("engineCylinders", engineCylinders)
    if (enginePowerMin) params.set("enginePowerMin", enginePowerMin)
    if (enginePowerMax) params.set("enginePowerMax", enginePowerMax)
    if (turboType) params.set("turboType", turboType)
    if (flangeType) params.set("flangeType", flangeType)
    if (wastegateType) params.set("wastegateType", wastegateType)
    if (rotorDiameterMin) params.set("rotorDiameterMin", rotorDiameterMin)
    if (rotorDiameterMax) params.set("rotorDiameterMax", rotorDiameterMax)
    if (padCompound) params.set("padCompound", padCompound)
    if (adjustableHeight !== "all") params.set("adjustableHeight", adjustableHeight)
    if (adjustableDamping !== "all") params.set("adjustableDamping", adjustableDamping)
    if (lightingVoltage) params.set("lightingVoltage", lightingVoltage)
    if (bulbType) params.set("bulbType", bulbType)
    if (toolCategory) params.set("toolCategory", toolCategory)
    if (powerSource !== "all") params.set("powerSource", powerSource)
    if (finish !== "all") params.set("finish", finish)
    if (streetLegal !== "all") params.set("streetLegal", streetLegal)
    if (styleTag) params.set("styleTag", styleTag)
    if (sortBy !== "newest") params.set("sortBy", sortBy)
    if (page > 1) params.set("page", String(page))

    if (!pathname) return
    const query = params.toString()
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false })
  }, [
    searchQuery,
    selectedCategory,
    selectedAvailability,
    selectedBrand,
    selectedCondition,
    minPrice,
    maxPrice,
    vehicleMake,
    vehicleModel,
    yearMin,
    yearMax,
    mileageMin,
    mileageMax,
    fuelType,
    transmission,
    bodyType,
    driveType,
    compatibilityMode,
    compatibleMake,
    compatibleModel,
    compatibleYear,
    oemType,
    partCategory,
    partsMain,
    partsSub,
    partsDeep,
    wheelDiameterMin,
    wheelDiameterMax,
    wheelWidthMin,
    wheelWidthMax,
    wheelOffsetMin,
    wheelOffsetMax,
    centerBoreMin,
    centerBoreMax,
    wheelBoltPattern,
    wheelMaterial,
    wheelColor,
    hubCentricRingsNeeded,
    engineType,
    engineDisplacementMin,
    engineDisplacementMax,
    engineCylinders,
    enginePowerMin,
    enginePowerMax,
    turboType,
    flangeType,
    wastegateType,
    rotorDiameterMin,
    rotorDiameterMax,
    padCompound,
    adjustableHeight,
    adjustableDamping,
    lightingVoltage,
    bulbType,
    toolCategory,
    powerSource,
    finish,
    streetLegal,
    styleTag,
    sortBy,
    page,
    pathname,
    router,
  ])

  // Load products scoped to this topic, applying all active filters server-side.
  useEffect(() => {
    setLoading(true)
    setError(null)

    const params: SearchParams = {
      page,
      limit: DISPLAY_PAGE_SIZE,
      sortBy,
      rootCategoryId,
    }

    if (searchQuery) params.query = searchQuery
    if (selectedCategory && selectedCategory !== "all") params.category = selectedCategory
    if (selectedAvailability && selectedAvailability !== "all") params.availability = selectedAvailability
    if (selectedBrand && selectedBrand !== "all") params.brand = selectedBrand
    if (selectedCondition && selectedCondition !== "all") params.condition = selectedCondition

    const parsedMin = parseFloat(minPrice)
    const parsedMax = parseFloat(maxPrice)
    const parsedYearMin = parseInt(yearMin, 10)
    const parsedYearMax = parseInt(yearMax, 10)
    const parsedMileageMin = parseInt(mileageMin, 10)
    const parsedMileageMax = parseInt(mileageMax, 10)
    const parsedCompatibleYear = parseInt(compatibleYear, 10)

    if (!Number.isNaN(parsedMin) && Number.isFinite(parsedMin)) params.minPrice = parsedMin
    if (!Number.isNaN(parsedMax) && Number.isFinite(parsedMax)) params.maxPrice = parsedMax
    if (vehicleMake) params.make = vehicleMake
    if (vehicleModel) params.model = vehicleModel
    if (!Number.isNaN(parsedYearMin)) params.yearMin = parsedYearMin
    if (!Number.isNaN(parsedYearMax)) params.yearMax = parsedYearMax
    if (!Number.isNaN(parsedMileageMin)) params.mileageMin = parsedMileageMin
    if (!Number.isNaN(parsedMileageMax)) params.mileageMax = parsedMileageMax
    if (fuelType !== "all") params.fuelType = fuelType
    if (transmission !== "all") params.transmission = transmission
    if (bodyType !== "all") params.bodyType = bodyType
    if (driveType !== "all") params.driveType = driveType
    if (compatibilityMode !== "all") params.compatibilityMode = compatibilityMode
    if (compatibleMake) params.compatibleMake = compatibleMake
    if (compatibleModel) params.compatibleModel = compatibleModel
    if (!Number.isNaN(parsedCompatibleYear)) params.compatibleYear = parsedCompatibleYear
    if (oemType !== "all") params.oemType = oemType
    if (partCategory) params.partCategory = partCategory
    if (partsMain) params.partsMain = partsMain
    if (partsSub) params.partsSub = partsSub
    if (partsDeep) params.partsDeep = partsDeep
    const parsedWheelDiameterMin = parseFloat(wheelDiameterMin)
    const parsedWheelDiameterMax = parseFloat(wheelDiameterMax)
    const parsedWheelWidthMin = parseFloat(wheelWidthMin)
    const parsedWheelWidthMax = parseFloat(wheelWidthMax)
    const parsedWheelOffsetMin = parseInt(wheelOffsetMin, 10)
    const parsedWheelOffsetMax = parseInt(wheelOffsetMax, 10)
    const parsedCenterBoreMin = parseFloat(centerBoreMin)
    const parsedCenterBoreMax = parseFloat(centerBoreMax)
    const parsedEngineDisplacementMin = parseInt(engineDisplacementMin, 10)
    const parsedEngineDisplacementMax = parseInt(engineDisplacementMax, 10)
    const parsedEngineCylinders = parseInt(engineCylinders, 10)
    const parsedEnginePowerMin = parseInt(enginePowerMin, 10)
    const parsedEnginePowerMax = parseInt(enginePowerMax, 10)
    const parsedRotorDiameterMin = parseInt(rotorDiameterMin, 10)
    const parsedRotorDiameterMax = parseInt(rotorDiameterMax, 10)
    if (!Number.isNaN(parsedWheelDiameterMin) && Number.isFinite(parsedWheelDiameterMin)) params.wheelDiameterMin = parsedWheelDiameterMin
    if (!Number.isNaN(parsedWheelDiameterMax) && Number.isFinite(parsedWheelDiameterMax)) params.wheelDiameterMax = parsedWheelDiameterMax
    if (!Number.isNaN(parsedWheelWidthMin) && Number.isFinite(parsedWheelWidthMin)) params.wheelWidthMin = parsedWheelWidthMin
    if (!Number.isNaN(parsedWheelWidthMax) && Number.isFinite(parsedWheelWidthMax)) params.wheelWidthMax = parsedWheelWidthMax
    if (!Number.isNaN(parsedWheelOffsetMin)) params.wheelOffsetMin = parsedWheelOffsetMin
    if (!Number.isNaN(parsedWheelOffsetMax)) params.wheelOffsetMax = parsedWheelOffsetMax
    if (!Number.isNaN(parsedCenterBoreMin) && Number.isFinite(parsedCenterBoreMin)) params.centerBoreMin = parsedCenterBoreMin
    if (!Number.isNaN(parsedCenterBoreMax) && Number.isFinite(parsedCenterBoreMax)) params.centerBoreMax = parsedCenterBoreMax
    if (wheelBoltPattern) params.wheelBoltPattern = wheelBoltPattern
    if (wheelMaterial) params.wheelMaterial = wheelMaterial
    if (wheelColor) params.wheelColor = wheelColor
    if (hubCentricRingsNeeded !== "all") params.hubCentricRingsNeeded = hubCentricRingsNeeded === "true"
    if (engineType) params.engineType = engineType
    if (!Number.isNaN(parsedEngineDisplacementMin)) params.engineDisplacementMin = parsedEngineDisplacementMin
    if (!Number.isNaN(parsedEngineDisplacementMax)) params.engineDisplacementMax = parsedEngineDisplacementMax
    if (!Number.isNaN(parsedEngineCylinders)) params.engineCylinders = parsedEngineCylinders
    if (!Number.isNaN(parsedEnginePowerMin)) params.enginePowerMin = parsedEnginePowerMin
    if (!Number.isNaN(parsedEnginePowerMax)) params.enginePowerMax = parsedEnginePowerMax
    if (turboType) params.turboType = turboType
    if (flangeType) params.flangeType = flangeType
    if (wastegateType) params.wastegateType = wastegateType
    if (!Number.isNaN(parsedRotorDiameterMin)) params.rotorDiameterMin = parsedRotorDiameterMin
    if (!Number.isNaN(parsedRotorDiameterMax)) params.rotorDiameterMax = parsedRotorDiameterMax
    if (padCompound) params.padCompound = padCompound
    if (adjustableHeight !== "all") params.adjustableHeight = adjustableHeight === "true"
    if (adjustableDamping !== "all") params.adjustableDamping = adjustableDamping === "true"
    if (lightingVoltage) params.lightingVoltage = lightingVoltage
    if (bulbType) params.bulbType = bulbType
    if (toolCategory) params.toolCategory = toolCategory
    if (powerSource !== "all") params.powerSource = powerSource
    if (finish !== "all") params.finish = finish
    if (streetLegal !== "all") params.streetLegal = streetLegal === "true"
    if (styleTag) params.styleTags = [styleTag]

    fetchProducts(params)
      .then((response) => {
        setProducts(response.products)
        setTotalPages(Math.max(1, response.totalPages || 1))
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message)
        setLoading(false)
      })
  }, [
    searchQuery,
    selectedCategory,
    selectedAvailability,
    selectedBrand,
    selectedCondition,
    minPrice,
    maxPrice,
    vehicleMake,
    vehicleModel,
    yearMin,
    yearMax,
    mileageMin,
    mileageMax,
    fuelType,
    transmission,
    bodyType,
    driveType,
    compatibilityMode,
    compatibleMake,
    compatibleModel,
    compatibleYear,
    oemType,
    partCategory,
    partsMain,
    partsSub,
    partsDeep,
    wheelDiameterMin,
    wheelDiameterMax,
    wheelWidthMin,
    wheelWidthMax,
    wheelOffsetMin,
    wheelOffsetMax,
    centerBoreMin,
    centerBoreMax,
    wheelBoltPattern,
    wheelMaterial,
    wheelColor,
    hubCentricRingsNeeded,
    engineType,
    engineDisplacementMin,
    engineDisplacementMax,
    engineCylinders,
    enginePowerMin,
    enginePowerMax,
    turboType,
    flangeType,
    wastegateType,
    rotorDiameterMin,
    rotorDiameterMax,
    padCompound,
    adjustableHeight,
    adjustableDamping,
    lightingVoltage,
    bulbType,
    toolCategory,
    powerSource,
    finish,
    streetLegal,
    styleTag,
    sortBy,
    page,
    rootCategoryId,
  ])

  const handleSearch = (value: string) => {
    setSearchQuery(value)
    setPage(1)
  }

  const handleMinPriceChange = (value: string) => {
    setMinPrice(value)
    setPage(1)
  }

  const handleMaxPriceChange = (value: string) => {
    setMaxPrice(value)
    setPage(1)
  }

  useEffect(() => {
    if (skipInitialPageReset.current) {
      skipInitialPageReset.current = false
      return
    }
    setPage(1)
  }, [
    selectedCategory,
    selectedAvailability,
    selectedBrand,
    selectedCondition,
    minPrice,
    maxPrice,
    vehicleMake,
    vehicleModel,
    yearMin,
    yearMax,
    mileageMin,
    mileageMax,
    fuelType,
    transmission,
    bodyType,
    driveType,
    compatibleYear,
    oemType,
    partCategory,
    toolCategory,
    powerSource,
    finish,
    streetLegal,
    styleTag,
    sortBy,
  ])

  const paginatedProducts = products
  const effectiveTotalPages = totalPages

  const clearFilters = () => {
    setSearchQuery("")
    setSelectedCategory(defaultCategoryId?.toString() || "all")
    setSelectedAvailability("all")
    setSelectedBrand("all")
    setSelectedCondition("all")
    setMinPrice("")
    setMaxPrice("")
    setVehicleMake("")
    setVehicleModel("")
    setYearMin("")
    setYearMax("")
    setMileageMin("")
    setMileageMax("")
    setFuelType("all")
    setTransmission("all")
    setBodyType("all")
    setDriveType("all")
    setCompatibilityMode("all")
    setCompatibleMake("")
    setCompatibleModel("")
    setCompatibleYear("")
    setOemType("all")
    setPartCategory("")
    setPartsMain(defaultPartsMainFromPage)
    setPartsSub("")
    setPartsDeep("")
    setWheelDiameterMin("")
    setWheelDiameterMax("")
    setWheelWidthMin("")
    setWheelWidthMax("")
    setWheelOffsetMin("")
    setWheelOffsetMax("")
    setCenterBoreMin("")
    setCenterBoreMax("")
    setWheelBoltPattern("")
    setWheelMaterial("")
    setWheelColor("")
    setHubCentricRingsNeeded("all")
    setEngineType("")
    setEngineDisplacementMin("")
    setEngineDisplacementMax("")
    setEngineCylinders("")
    setEnginePowerMin("")
    setEnginePowerMax("")
    setTurboType("")
    setFlangeType("")
    setWastegateType("")
    setRotorDiameterMin("")
    setRotorDiameterMax("")
    setPadCompound("")
    setAdjustableHeight("all")
    setAdjustableDamping("all")
    setLightingVoltage("")
    setBulbType("")
    setToolCategory("")
    setPowerSource("all")
    setFinish("all")
    setStreetLegal("all")
    setStyleTag("")
    setSortBy("newest")
    setPage(1)
  }

  const hasActiveFilters =
    Boolean(searchQuery) ||
    (selectedCategory !== "all" && selectedCategory !== defaultCategoryId?.toString()) ||
    selectedAvailability !== "all" ||
    selectedBrand !== "all" ||
    selectedCondition !== "all" ||
    Boolean(minPrice) ||
    Boolean(maxPrice) ||
    Boolean(vehicleMake) ||
    Boolean(vehicleModel) ||
    Boolean(yearMin) ||
    Boolean(yearMax) ||
    Boolean(mileageMin) ||
    Boolean(mileageMax) ||
    fuelType !== "all" ||
    transmission !== "all" ||
    bodyType !== "all" ||
    driveType !== "all" ||
    compatibilityMode !== "all" ||
    Boolean(compatibleMake) ||
    Boolean(compatibleModel) ||
    Boolean(compatibleYear) ||
    oemType !== "all" ||
    Boolean(partCategory) ||
    partsMain !== defaultPartsMainFromPage ||
    Boolean(partsSub) ||
    Boolean(partsDeep) ||
    Boolean(wheelDiameterMin) ||
    Boolean(wheelDiameterMax) ||
    Boolean(wheelWidthMin) ||
    Boolean(wheelWidthMax) ||
    Boolean(wheelOffsetMin) ||
    Boolean(wheelOffsetMax) ||
    Boolean(centerBoreMin) ||
    Boolean(centerBoreMax) ||
    Boolean(wheelBoltPattern) ||
    Boolean(wheelMaterial) ||
    Boolean(wheelColor) ||
    hubCentricRingsNeeded !== "all" ||
    Boolean(engineType) ||
    Boolean(engineDisplacementMin) ||
    Boolean(engineDisplacementMax) ||
    Boolean(engineCylinders) ||
    Boolean(enginePowerMin) ||
    Boolean(enginePowerMax) ||
    Boolean(turboType) ||
    Boolean(flangeType) ||
    Boolean(wastegateType) ||
    Boolean(rotorDiameterMin) ||
    Boolean(rotorDiameterMax) ||
    Boolean(padCompound) ||
    adjustableHeight !== "all" ||
    adjustableDamping !== "all" ||
    Boolean(lightingVoltage) ||
    Boolean(bulbType) ||
    Boolean(toolCategory) ||
    powerSource !== "all" ||
    finish !== "all" ||
    streetLegal !== "all" ||
    Boolean(styleTag) ||
    sortBy !== "newest"

  const isCarsTopic = topicSlug === "cars"
  const isPartsTopic = topicSlug === "parts"
  const isToolsTopic = topicSlug === "tools"
  const isCustomTopic = topicSlug === "custom"
  const partsBranch = resolvePartsBranch(partsMain, partsSub)
  const partsSubOptions = getPartsSubcategories(partsMain)
  const partsDeepOptions = getPartsDeepCategories(partsMain, partsSub)

  return (
    <div className="flex gap-8">
      {/* Sidebar Filters - Desktop */}
      <aside className="hidden w-64 flex-shrink-0 lg:block">
        <Card>
          <CardContent className="p-6 space-y-6">
            {showBackLink && (
              <div>
                <Link href={backLinkHref} className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  {backLinkLabel}
                </Link>
                <Separator className="mt-4" />
              </div>
            )}

            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold">Filters</h3>
                <div className="flex items-center gap-1">
                  {hasActiveFilters && !filtersCollapsed && (
                    <Button variant="ghost" size="sm" onClick={clearFilters}>
                      Clear
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-auto p-0 !text-white hover:bg-transparent hover:!text-white [&_svg]:!text-white"
                    onClick={() => setFiltersCollapsed((prev) => !prev)}
                    aria-label={filtersCollapsed ? "Show all filters" : "Hide all filters"}
                  >
                    <ChevronDown
                      className={`pointer-events-none size-5 shrink-0 transition-transform duration-200 ${filtersCollapsed ? "rotate-0" : "rotate-180"}`}
                      style={{ color: "#ffffff", stroke: "#ffffff", opacity: 1 }}
                    />
                  </Button>
                </div>
              </div>
              <Separator />
            </div>

            {!filtersCollapsed && (
              <>

                {/* Category Filter */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Category</label>
                  <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                    <SelectTrigger>
                      <SelectValue placeholder="All Categories" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Categories</SelectItem>
                      {categoryOptions.map((option) => (
                        <SelectItem key={option.id} value={option.id.toString()}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Availability Filter */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Availability</label>
                  <Select value={selectedAvailability} onValueChange={setSelectedAvailability}>
                    <SelectTrigger>
                      <SelectValue placeholder="All" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="in_stock">In Stock</SelectItem>
                      <SelectItem value="low_stock">Low Stock</SelectItem>
                      <SelectItem value="pre_order">Pre-Order</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Brand Filter */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Brand</label>
                  <Select value={selectedBrand} onValueChange={setSelectedBrand}>
                    <SelectTrigger>
                      <SelectValue placeholder="All Brands" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Brands</SelectItem>
                      {brands.map((brand) => (
                        <SelectItem key={brand} value={brand}>
                          {brand}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Condition Filter */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Condition</label>
                  <Select value={selectedCondition} onValueChange={setSelectedCondition}>
                    <SelectTrigger>
                      <SelectValue placeholder="All Conditions" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Conditions</SelectItem>
                      {PRODUCT_CONDITIONS.map((value) => (
                        <SelectItem key={value} value={value}>
                          {value.replace("_", " ")}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Topic-specific filters */}
                {(isCarsTopic || isCustomTopic) && (
                  <>
                    <div>
                      <label className="mb-2 block text-sm font-medium">Make</label>
                      <Input value={vehicleMake} onChange={(e) => setVehicleMake(e.target.value)} placeholder="e.g. BMW" />
                    </div>
                    <div>
                      <label className="mb-2 block text-sm font-medium">Model</label>
                      <Input value={vehicleModel} onChange={(e) => setVehicleModel(e.target.value)} placeholder="e.g. 330i" />
                    </div>
                  </>
                )}

                {isCarsTopic && (
                  <>
                    <div className="grid grid-cols-2 gap-2">
                      <Input type="number" value={yearMin} onChange={(e) => setYearMin(e.target.value)} placeholder="Year min" />
                      <Input type="number" value={yearMax} onChange={(e) => setYearMax(e.target.value)} placeholder="Year max" />
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <Input type="number" value={mileageMin} onChange={(e) => setMileageMin(e.target.value)} placeholder="Mileage min" />
                      <Input type="number" value={mileageMax} onChange={(e) => setMileageMax(e.target.value)} placeholder="Mileage max" />
                    </div>
                    <Select value={fuelType} onValueChange={setFuelType}>
                      <SelectTrigger><SelectValue placeholder="Fuel Type" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Fuel Types</SelectItem>
                        {FUEL_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                      </SelectContent>
                    </Select>
                    <Select value={transmission} onValueChange={setTransmission}>
                      <SelectTrigger><SelectValue placeholder="Transmission" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Transmissions</SelectItem>
                        {TRANSMISSION_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                      </SelectContent>
                    </Select>
                    <Select value={bodyType} onValueChange={setBodyType}>
                      <SelectTrigger><SelectValue placeholder="Body Type" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Body Types</SelectItem>
                        {BODY_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                      </SelectContent>
                    </Select>
                    <Select value={driveType} onValueChange={setDriveType}>
                      <SelectTrigger><SelectValue placeholder="Drive Type" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Drive Types</SelectItem>
                        {DRIVE_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </>
                )}

                {isPartsTopic && (
                  <>
                    <div>
                      <label className="mb-2 block text-sm font-medium">My Car</label>
                      <Select value={compatibilityMode} onValueChange={setCompatibilityMode}>
                        <SelectTrigger><SelectValue placeholder="Compatibility mode" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Any</SelectItem>
                          {COMPATIBILITY_MODES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                    {compatibilityMode === "vehicle_specific" && (
                      <>
                        <Input value={compatibleMake} onChange={(e) => setCompatibleMake(e.target.value)} placeholder="Compatible make" />
                        <Input value={compatibleModel} onChange={(e) => setCompatibleModel(e.target.value)} placeholder="Compatible model" />
                      </>
                    )}
                    <div>
                      <label className="mb-2 block text-sm font-medium">Compatible Year</label>
                      <Input type="number" value={compatibleYear} onChange={(e) => setCompatibleYear(e.target.value)} placeholder="e.g. 2019" />
                    </div>
                    <div>
                      <label className="mb-2 block text-sm font-medium">Main Category</label>
                      <Select value={partsMain || "all"} onValueChange={(value) => {
                        const next = value === "all" ? "" : value
                        setPartsMain(next)
                        setPartsSub("")
                        setPartsDeep("")
                      }}>
                        <SelectTrigger><SelectValue placeholder="All main categories" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">All main categories</SelectItem>
                          <SelectItem value="engine-drivetrain">Engine & Drivetrain</SelectItem>
                          <SelectItem value="suspension-steering">Suspension & Steering</SelectItem>
                          <SelectItem value="brakes">Brakes</SelectItem>
                          <SelectItem value="wheels-tires">Wheels & Tires</SelectItem>
                          <SelectItem value="electrical-lighting">Electrical & Lighting</SelectItem>
                          <SelectItem value="exterior-body">Exterior & Body</SelectItem>
                          <SelectItem value="interior">Interior</SelectItem>
                          <SelectItem value="cooling-hvac">Cooling & HVAC</SelectItem>
                          <SelectItem value="maintenance-service">Maintenance & Service</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    {partsMain && (
                      <div>
                        <label className="mb-2 block text-sm font-medium">Subcategory</label>
                        <Select value={partsSub || "all"} onValueChange={(value) => {
                          const next = value === "all" ? "" : value
                          setPartsSub(next)
                          setPartsDeep("")
                        }}>
                          <SelectTrigger><SelectValue placeholder="All subcategories" /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="all">All subcategories</SelectItem>
                            {partsSubOptions.map((sub) => <SelectItem key={sub} value={sub}>{sub.replace(/-/g, " ")}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                    )}
                    {partsSub && (
                      <div>
                        <label className="mb-2 block text-sm font-medium">Deep Category</label>
                        <Select value={partsDeep || "all"} onValueChange={(value) => setPartsDeep(value === "all" ? "" : value)}>
                          <SelectTrigger><SelectValue placeholder="All deep categories" /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="all">All deep categories</SelectItem>
                            {partsDeepOptions.map((deep) => <SelectItem key={deep} value={deep}>{deep.replace(/-/g, " ")}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </div>
                    )}
                    <div>
                      <label className="mb-2 block text-sm font-medium">Part Category</label>
                      <Input value={partCategory} onChange={(e) => setPartCategory(e.target.value)} placeholder="e.g. brakes" />
                    </div>
                    <Select value={oemType} onValueChange={setOemType}>
                      <SelectTrigger><SelectValue placeholder="OEM Type" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All OEM Types</SelectItem>
                        <SelectItem value="oem">OEM</SelectItem>
                        <SelectItem value="aftermarket">Aftermarket</SelectItem>
                      </SelectContent>
                    </Select>

                    {partsBranch === "wheels" && (
                      <>
                        <div className="grid grid-cols-2 gap-2">
                          <Input type="number" value={wheelDiameterMin} onChange={(e) => setWheelDiameterMin(e.target.value)} placeholder="Diameter min" />
                          <Input type="number" value={wheelDiameterMax} onChange={(e) => setWheelDiameterMax(e.target.value)} placeholder="Diameter max" />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <Input type="number" value={wheelWidthMin} onChange={(e) => setWheelWidthMin(e.target.value)} placeholder="Width min" />
                          <Input type="number" value={wheelWidthMax} onChange={(e) => setWheelWidthMax(e.target.value)} placeholder="Width max" />
                        </div>
                        <Input value={wheelBoltPattern} onChange={(e) => setWheelBoltPattern(e.target.value)} placeholder="Bolt pattern (e.g. 5x112)" />
                      </>
                    )}

                    {partsBranch === "engines" && (
                      <>
                        <Input value={engineType} onChange={(e) => setEngineType(e.target.value)} placeholder="Engine type (I4, V8...)" />
                        <div className="grid grid-cols-2 gap-2">
                          <Input type="number" value={engineDisplacementMin} onChange={(e) => setEngineDisplacementMin(e.target.value)} placeholder="Displacement min (cc)" />
                          <Input type="number" value={engineDisplacementMax} onChange={(e) => setEngineDisplacementMax(e.target.value)} placeholder="Displacement max (cc)" />
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                          <Input type="number" value={engineCylinders} onChange={(e) => setEngineCylinders(e.target.value)} placeholder="Cylinders" />
                          <Input type="number" value={enginePowerMin} onChange={(e) => setEnginePowerMin(e.target.value)} placeholder="Power min (hp)" />
                        </div>
                      </>
                    )}

                    {partsBranch === "turbochargers" && (
                      <>
                        <Input value={turboType} onChange={(e) => setTurboType(e.target.value)} placeholder="Turbo type" />
                        <Input value={flangeType} onChange={(e) => setFlangeType(e.target.value)} placeholder="Flange type" />
                        <Input value={wastegateType} onChange={(e) => setWastegateType(e.target.value)} placeholder="Wastegate type" />
                      </>
                    )}

                    {partsBranch === "brakes" && (
                      <>
                        <div className="grid grid-cols-2 gap-2">
                          <Input type="number" value={rotorDiameterMin} onChange={(e) => setRotorDiameterMin(e.target.value)} placeholder="Rotor dia min" />
                          <Input type="number" value={rotorDiameterMax} onChange={(e) => setRotorDiameterMax(e.target.value)} placeholder="Rotor dia max" />
                        </div>
                        <Input value={padCompound} onChange={(e) => setPadCompound(e.target.value)} placeholder="Pad compound" />
                      </>
                    )}

                    {partsBranch === "suspension" && (
                      <>
                        <Select value={adjustableHeight} onValueChange={setAdjustableHeight}>
                          <SelectTrigger><SelectValue placeholder="Adjustable height" /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="all">Any height adjustability</SelectItem>
                            <SelectItem value="true">Height adjustable</SelectItem>
                            <SelectItem value="false">Fixed height</SelectItem>
                          </SelectContent>
                        </Select>
                        <Select value={adjustableDamping} onValueChange={setAdjustableDamping}>
                          <SelectTrigger><SelectValue placeholder="Adjustable damping" /></SelectTrigger>
                          <SelectContent>
                            <SelectItem value="all">Any damping adjustability</SelectItem>
                            <SelectItem value="true">Damping adjustable</SelectItem>
                            <SelectItem value="false">Fixed damping</SelectItem>
                          </SelectContent>
                        </Select>
                      </>
                    )}

                    {partsBranch === "electrical-lighting" && (
                      <>
                        <Input value={lightingVoltage} onChange={(e) => setLightingVoltage(e.target.value)} placeholder="Voltage (e.g. 12V)" />
                        <Input value={bulbType} onChange={(e) => setBulbType(e.target.value)} placeholder="Bulb type (e.g. H7)" />
                      </>
                    )}
                  </>
                )}

                {isToolsTopic && (
                  <>
                    <div>
                      <label className="mb-2 block text-sm font-medium">Tool Category</label>
                      <Input value={toolCategory} onChange={(e) => setToolCategory(e.target.value)} placeholder="e.g. torque_wrench" />
                    </div>
                    <Select value={powerSource} onValueChange={setPowerSource}>
                      <SelectTrigger><SelectValue placeholder="Power Source" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Power Sources</SelectItem>
                        {POWER_SOURCES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </>
                )}

                {isCustomTopic && (
                  <>
                    <div>
                      <label className="mb-2 block text-sm font-medium">Style Tag</label>
                      <Input value={styleTag} onChange={(e) => setStyleTag(e.target.value)} placeholder="e.g. sport" />
                    </div>
                    <Select value={finish} onValueChange={setFinish}>
                      <SelectTrigger><SelectValue placeholder="Finish" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All Finishes</SelectItem>
                        <SelectItem value="matte">Matte</SelectItem>
                        <SelectItem value="gloss">Gloss</SelectItem>
                        <SelectItem value="satin">Satin</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select value={streetLegal} onValueChange={setStreetLegal}>
                      <SelectTrigger><SelectValue placeholder="Street Legal" /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">All</SelectItem>
                        <SelectItem value="true">Street Legal</SelectItem>
                        <SelectItem value="false">Track Only</SelectItem>
                      </SelectContent>
                    </Select>
                  </>
                )}

                {/* Price Range */}
                <div>
                  <label className="mb-2 block text-sm font-medium">Price Range</label>
                  <div className="space-y-2">
                    <Input
                      type="number"
                      placeholder="Min price"
                      value={minPrice}
                      onChange={(e) => handleMinPriceChange(e.target.value)}
                      min="0"
                      step="1"
                    />
                    <Input
                      type="number"
                      placeholder="Max price"
                      value={maxPrice}
                      onChange={(e) => handleMaxPriceChange(e.target.value)}
                      min="0"
                      step="1"
                    />
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </aside>

      {/* Main Content */}
      <div className="flex-1">
        {/* Search and Sort Bar */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative flex-1 sm:max-w-md">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="text"
              placeholder={`Search ${topicName?.toLowerCase() || 'products'}...`}
              value={searchQuery}
              onChange={(e) => handleSearch(e.target.value)}
              className="pl-10"
            />
          </div>

          <div className="flex gap-2">
            {/* Mobile Filters Button */}
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowMobileFilters(!showMobileFilters)}
              className="lg:hidden"
            >
              <Filter className="mr-2 h-4 w-4" />
              Filters
            </Button>

            <Select value={sortBy} onValueChange={(value) => setSortBy(value as NonNullable<SearchParams['sortBy']>)}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Sort by" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="newest">Newest First</SelectItem>
                <SelectItem value="price_asc">Price: Low to High</SelectItem>
                <SelectItem value="price_desc">Price: High to Low</SelectItem>
                <SelectItem value="name_asc">Name: A to Z</SelectItem>
                <SelectItem value="name_desc">Name: Z to A</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Mobile Filters */}
        {showMobileFilters && (
          <Card className="mb-6 lg:hidden">
            <CardContent className="p-4 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold">Filters</h3>
                <Button variant="ghost" size="sm" onClick={() => setShowMobileFilters(false)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium">Category</label>
                <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                  <SelectTrigger>
                    <SelectValue placeholder="All Categories" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Categories</SelectItem>
                    {categoryOptions.map((option) => (
                      <SelectItem key={option.id} value={option.id.toString()}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium">Availability</label>
                <Select value={selectedAvailability} onValueChange={setSelectedAvailability}>
                  <SelectTrigger>
                    <SelectValue placeholder="All" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All</SelectItem>
                    <SelectItem value="in_stock">In Stock</SelectItem>
                    <SelectItem value="low_stock">Low Stock</SelectItem>
                    <SelectItem value="pre_order">Pre-Order</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium">Brand</label>
                <Select value={selectedBrand} onValueChange={setSelectedBrand}>
                  <SelectTrigger>
                    <SelectValue placeholder="All Brands" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Brands</SelectItem>
                    {brands.map((brand) => (
                      <SelectItem key={brand} value={brand}>
                        {brand}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium">Condition</label>
                <Select value={selectedCondition} onValueChange={setSelectedCondition}>
                  <SelectTrigger>
                    <SelectValue placeholder="All Conditions" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Conditions</SelectItem>
                    {PRODUCT_CONDITIONS.map((value) => (
                      <SelectItem key={value} value={value}>
                        {value.replace("_", " ")}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {(isCarsTopic || isCustomTopic) && (
                <div className="grid grid-cols-2 gap-2">
                  <Input value={vehicleMake} onChange={(e) => setVehicleMake(e.target.value)} placeholder="Make" />
                  <Input value={vehicleModel} onChange={(e) => setVehicleModel(e.target.value)} placeholder="Model" />
                </div>
              )}

              {isCarsTopic && (
                <div className="grid grid-cols-2 gap-2">
                  <Input type="number" value={yearMin} onChange={(e) => setYearMin(e.target.value)} placeholder="Year min" />
                  <Input type="number" value={yearMax} onChange={(e) => setYearMax(e.target.value)} placeholder="Year max" />
                </div>
              )}

              {isPartsTopic && (
                <div className="space-y-2">
                  <Select value={compatibilityMode} onValueChange={setCompatibilityMode}>
                    <SelectTrigger><SelectValue placeholder="Compatibility mode" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">Any</SelectItem>
                      {COMPATIBILITY_MODES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                    </SelectContent>
                  </Select>
                  {compatibilityMode === "vehicle_specific" && (
                    <div className="grid grid-cols-2 gap-2">
                      <Input value={compatibleMake} onChange={(e) => setCompatibleMake(e.target.value)} placeholder="Compatible make" />
                      <Input value={compatibleModel} onChange={(e) => setCompatibleModel(e.target.value)} placeholder="Compatible model" />
                    </div>
                  )}
                  <div className="grid grid-cols-2 gap-2">
                    <Input type="number" value={compatibleYear} onChange={(e) => setCompatibleYear(e.target.value)} placeholder="Compatible year" />
                    <Input value={partCategory} onChange={(e) => setPartCategory(e.target.value)} placeholder="Part category" />
                  </div>
                  <Select value={partsMain || "all"} onValueChange={(value) => {
                    const next = value === "all" ? "" : value
                    setPartsMain(next)
                    setPartsSub("")
                    setPartsDeep("")
                  }}>
                    <SelectTrigger><SelectValue placeholder="Main category" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All main categories</SelectItem>
                      <SelectItem value="engine-drivetrain">Engine & Drivetrain</SelectItem>
                      <SelectItem value="suspension-steering">Suspension & Steering</SelectItem>
                      <SelectItem value="brakes">Brakes</SelectItem>
                      <SelectItem value="wheels-tires">Wheels & Tires</SelectItem>
                      <SelectItem value="electrical-lighting">Electrical & Lighting</SelectItem>
                      <SelectItem value="exterior-body">Exterior & Body</SelectItem>
                      <SelectItem value="interior">Interior</SelectItem>
                      <SelectItem value="cooling-hvac">Cooling & HVAC</SelectItem>
                      <SelectItem value="maintenance-service">Maintenance & Service</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              )}

              {isToolsTopic && (
                <Input value={toolCategory} onChange={(e) => setToolCategory(e.target.value)} placeholder="Tool category" />
              )}

              {isCustomTopic && (
                <Input value={styleTag} onChange={(e) => setStyleTag(e.target.value)} placeholder="Style tag" />
              )}

              <div>
                <label className="mb-2 block text-sm font-medium">Price Range</label>
                <div className="space-y-2">
                  <Input
                    type="number"
                    placeholder="Min price"
                    value={minPrice}
                    onChange={(e) => handleMinPriceChange(e.target.value)}
                    min="0"
                    step="1"
                  />
                  <Input
                    type="number"
                    placeholder="Max price"
                    value={maxPrice}
                    onChange={(e) => handleMaxPriceChange(e.target.value)}
                    min="0"
                    step="1"
                  />
                </div>
              </div>

              {hasActiveFilters && (
                <Button variant="outline" size="sm" onClick={clearFilters} className="w-full">
                  Clear Filters
                </Button>
              )}
            </CardContent>
          </Card>
        )}

        {/* Error State */}
        {error && (
          <Card className="border-destructive">
            <CardContent className="p-8 text-center">
              <p className="mb-4 text-destructive">{error}</p>
              <p className="text-sm text-muted-foreground">
                Unable to load products. Please check your connection or try again later.
              </p>
            </CardContent>
          </Card>
        )}

        {/* Loading State */}
        {loading && (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Card key={i}>
                <CardContent className="p-0">
                  <Skeleton className="aspect-square w-full" />
                  <div className="p-4 space-y-2">
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-4 w-1/2" />
                    <Skeleton className="h-8 w-full" />
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && paginatedProducts.length === 0 && (
          <Card>
            <CardContent className="p-12 text-center">
              <ShoppingCart className="mx-auto mb-4 h-16 w-16 text-muted-foreground" />
              <h3 className="mb-2 text-lg font-semibold">No products found</h3>
              <p className="mb-4 text-sm text-muted-foreground">
                {hasActiveFilters
                  ? "Try adjusting your filters or search query"
                  : `No ${topicName?.toLowerCase() || 'products'} available at the moment`}
              </p>
              {hasActiveFilters && (
                <Button onClick={clearFilters}>Clear Filters</Button>
              )}
            </CardContent>
          </Card>
        )}

        {/* Products Grid */}
        {!loading && !error && paginatedProducts.length > 0 && (
          <>
            <div className="space-y-4">
              {paginatedProducts.map((product, i) => {
                const availabilityBadge = getAvailabilityBadge(product.availability)
                const isQuoteOnly = isQuoteOnlyProduct(product)
                const isCarListingCard = isCarsTopic || product.productType === "car"
                const isPartListingCard = isPartsTopic || product.productType === "part"
                const carQuickFacts: string[] = []
                if (product.year) carQuickFacts.push(String(product.year))
                const mileageLabel = formatMileage(product.mileage)
                if (mileageLabel) carQuickFacts.push(mileageLabel)
                const hpLabel = formatHorsepowerFromKw(product.powerKw)
                if (hpLabel) carQuickFacts.push(hpLabel)
                if (product.fuelType) carQuickFacts.push(formatEnumLabel(product.fuelType))
                if (product.transmission) carQuickFacts.push(formatEnumLabel(product.transmission))
                if (product.driveType) carQuickFacts.push(formatEnumLabel(product.driveType))
                const visibleCarFacts = carQuickFacts.slice(0, 6)

                const partFacts: Array<{ label: string; value: string }> = []
                if (isPartListingCard) {
                  if (product.partCategory) partFacts.push({ label: "Category", value: product.partCategory })
                  if (product.condition) partFacts.push({ label: "Condition", value: formatEnumLabel(product.condition) })
                  if (product.material) partFacts.push({ label: "Material", value: product.material })
                  if (product.partPosition && product.partPosition.length > 0) {
                    partFacts.push({ label: "Position", value: product.partPosition.slice(0, 2).join(", ") })
                  }
                }

                const excerpt = getCardExcerpt(product.description)

                return (
                  <motion.div
                    key={product.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.05 }}
                  >
                    <Card className="group overflow-hidden transition-all hover:shadow-lg relative">
                      <CardContent className="p-0">
                        <Link href={`/shop/${product.slug}`} className="flex flex-col md:flex-row md:cursor-pointer hover:opacity-90 transition-opacity">
                          {/* Product Image */}
                          <div className="block md:w-1/3 flex-shrink-0">
                            <div className="relative aspect-square md:aspect-auto md:h-64 overflow-hidden bg-muted rounded-lg m-4 md:m-0 md:rounded-lg">
                              {product.images && product.images[0] ? (
                                <Image
                                  src={getImageUrl(product.images[0])}
                                  alt={product.name}
                                  fill
                                  className="object-cover transition-transform group-hover:scale-105 rounded-lg"
                                />
                              ) : (
                                <div className="flex h-full items-center justify-center">
                                  <ShoppingCart className="h-16 w-16 text-muted-foreground" />
                                </div>
                              )}
                              <Badge
                                variant={availabilityBadge.variant}
                                className="absolute right-2 top-2"
                              >
                                {availabilityBadge.label}
                              </Badge>
                            </div>
                          </div>

                          {/* Product Details */}
                          <div className="flex-1 p-6 flex flex-col justify-between">
                            <div className="space-y-3">
                              <h3 className="text-lg font-semibold line-clamp-2 hover:text-primary">
                                {product.name}
                              </h3>

                              {product.partNumber && (
                                <p className="text-sm text-muted-foreground">
                                  Part #: {product.partNumber}
                                </p>
                              )}

                              {isCarListingCard && visibleCarFacts.length > 0 && (
                                <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm font-medium text-foreground/90 sm:grid-cols-3">
                                  {visibleCarFacts.map((fact, factIndex) => (
                                    <p key={`${product.id}-fact-${factIndex}`}>{fact}</p>
                                  ))}
                                </div>
                              )}

                              {isPartListingCard && partFacts.length > 0 && (
                                <div className="flex flex-wrap gap-2">
                                  {partFacts.slice(0, 4).map((fact) => (
                                    <Badge
                                      key={`${product.id}-partfact-${fact.label}`}
                                      variant="secondary"
                                      className="text-xs font-normal"
                                      title={`${fact.label}: ${fact.value}`}
                                    >
                                      {fact.label}: {fact.value}
                                    </Badge>
                                  ))}
                                </div>
                              )}

                              {excerpt && (
                                <p className="text-sm text-muted-foreground line-clamp-2" title={product.description}>
                                  {excerpt}
                                </p>
                              )}

                              {product.leadTime && (
                                <p className="text-sm text-muted-foreground">
                                  Lead Time: {product.leadTime}
                                </p>
                              )}
                            </div>

                            <div className="flex items-center justify-between gap-4 mt-4">
                              <p className="text-2xl font-bold">
                                {formatCurrency(product.price, product.currency)}
                              </p>
                              <div className="flex gap-2">
                                <Button asChild variant="outline">
                                  <span>View</span>
                                </Button>
                                {!isQuoteOnly ? (
                                  <Button
                                    onClick={(e) => {
                                      e.preventDefault()
                                      if (isQuoteOnly) return
                                      addItem(product)
                                    }}
                                    disabled={product.availability === 'out_of_stock'}
                                  >
                                    <ShoppingCart className="mr-2 h-4 w-4" />
                                    Add
                                  </Button>
                                ) : (
                                  <Button
                                    asChild
                                    variant="secondary"
                                  >
                                    <span>Request Quote</span>
                                  </Button>
                                )}
                              </div>
                            </div>
                          </div>
                        </Link>
                      </CardContent>
                    </Card>
                  </motion.div>
                )
              })}
            </div>

            {/* Pagination */}
            {effectiveTotalPages > 1 && (
              <div className="mt-8 flex justify-center gap-2">
                <Button
                  variant="outline"
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page === 1}
                >
                  Previous
                </Button>
                <div className="flex items-center gap-2 px-4">
                  <span className="text-sm text-muted-foreground">
                    Page {page} of {effectiveTotalPages}
                  </span>
                </div>
                <Button
                  variant="outline"
                  onClick={() => setPage((p) => Math.min(effectiveTotalPages, p + 1))}
                  disabled={page === effectiveTotalPages}
                >
                  Next
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
