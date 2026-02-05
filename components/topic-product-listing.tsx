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
  DRIVE_TYPES,
  FUEL_TYPES,
  POWER_SOURCES,
  PRODUCT_CONDITIONS,
  TRANSMISSION_TYPES,
} from "@/lib/filterAttributes"
import { getImageUrl } from "@/lib/utils"
import { Search, ShoppingCart, Filter, X, ArrowLeft } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { motion } from "framer-motion"

interface TopicProductListingProps {
  rootCategoryId: number
  topicSlug: string
  topicName?: string
  topicDescription?: string
  defaultCategoryId?: number
  showBackLink?: boolean
  backLinkHref?: string
  backLinkLabel?: string
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

export function TopicProductListing({
  rootCategoryId,
  topicSlug,
  topicName,
  topicDescription,
  defaultCategoryId,
  showBackLink = true,
  backLinkHref = "/shop",
  backLinkLabel = "Back to Shop"
}: TopicProductListingProps) {
  const DISPLAY_PAGE_SIZE = 12
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
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
  const [compatibleYear, setCompatibleYear] = useState("")
  const [oemType, setOemType] = useState("all")
  const [partCategory, setPartCategory] = useState("")
  const [toolCategory, setToolCategory] = useState("")
  const [powerSource, setPowerSource] = useState("all")
  const [finish, setFinish] = useState("all")
  const [streetLegal, setStreetLegal] = useState("all")
  const [styleTag, setStyleTag] = useState("")

  const [sortBy, setSortBy] = useState<SearchParams['sortBy']>("newest")
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [showMobileFilters, setShowMobileFilters] = useState(false)
  const [brands, setBrands] = useState<string[]>([])
  const skipInitialPageReset = useRef(true)

  const { addItem } = useCart()

  // Load categories for this topic (children of root category)
  useEffect(() => {
    fetchCategoryChildren(rootCategoryId)
      .then(setCategories)
      .catch((err) => console.error('Failed to load categories:', err))
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
    setCompatibleYear(get("compatibleYear") || "")
    setOemType(get("oemType") || "all")
    setPartCategory(get("partCategory") || "")
    setToolCategory(get("toolCategory") || "")
    setPowerSource(get("powerSource") || "all")
    setFinish(get("finish") || "all")
    setStreetLegal(get("streetLegal") || "all")
    setStyleTag(get("styleTag") || "")
    setSortBy((get("sortBy") as SearchParams["sortBy"]) || "newest")
    setPage(Math.max(1, Number.parseInt(get("page") || "1", 10) || 1))
  }, [searchParams, defaultCategoryId])

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
    if (compatibleYear) params.set("compatibleYear", compatibleYear)
    if (oemType !== "all") params.set("oemType", oemType)
    if (partCategory) params.set("partCategory", partCategory)
    if (toolCategory) params.set("toolCategory", toolCategory)
    if (powerSource !== "all") params.set("powerSource", powerSource)
    if (finish !== "all") params.set("finish", finish)
    if (streetLegal !== "all") params.set("streetLegal", streetLegal)
    if (styleTag) params.set("styleTag", styleTag)
    if (sortBy !== "newest") params.set("sortBy", sortBy)
    if (page > 1) params.set("page", String(page))

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
    compatibleYear,
    oemType,
    partCategory,
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
    if (!Number.isNaN(parsedCompatibleYear)) params.compatibleYear = parsedCompatibleYear
    if (oemType !== "all") params.oemType = oemType
    if (partCategory) params.partCategory = partCategory
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
    compatibleYear,
    oemType,
    partCategory,
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
    setCompatibleYear("")
    setOemType("all")
    setPartCategory("")
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
    Boolean(compatibleYear) ||
    oemType !== "all" ||
    Boolean(partCategory) ||
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
                {hasActiveFilters && (
                  <Button variant="ghost" size="sm" onClick={clearFilters}>
                    Clear
                  </Button>
                )}
              </div>
              <Separator />
            </div>

            {/* Category Filter */}
            <div>
              <label className="mb-2 block text-sm font-medium">Category</label>
              <Select value={selectedCategory} onValueChange={setSelectedCategory}>
                <SelectTrigger>
                  <SelectValue placeholder="All Categories" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Categories</SelectItem>
                  {categories.map((cat) => (
                    <SelectItem key={cat.id} value={cat.id.toString()}>
                      {cat.name}
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
            {(isCarsTopic || isPartsTopic || isCustomTopic) && (
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
                  <label className="mb-2 block text-sm font-medium">Compatible Year</label>
                  <Input type="number" value={compatibleYear} onChange={(e) => setCompatibleYear(e.target.value)} placeholder="e.g. 2019" />
                </div>
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

            <Select value={sortBy} onValueChange={(value) => setSortBy(value as SearchParams['sortBy'])}>
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
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id.toString()}>
                        {cat.name}
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

              {(isCarsTopic || isPartsTopic || isCustomTopic) && (
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
                <div className="grid grid-cols-2 gap-2">
                  <Input value={partCategory} onChange={(e) => setPartCategory(e.target.value)} placeholder="Part category" />
                  <Input type="number" value={compatibleYear} onChange={(e) => setCompatibleYear(e.target.value)} placeholder="Compatible year" />
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

                              {product.description && (
                                <p className="text-sm text-muted-foreground line-clamp-2">
                                  {product.description}
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
                                {isCarListingCard ? (
                                  <Button
                                    asChild
                                    variant="secondary"
                                  >
                                    <span>Quote</span>
                                  </Button>
                                ) : !isQuoteOnly ? (
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
                                    <span>Quote</span>
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
