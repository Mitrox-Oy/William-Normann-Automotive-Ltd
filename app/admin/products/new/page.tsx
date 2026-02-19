"use client"

import { useState, useEffect, useRef, type ChangeEvent } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { RequireAuth } from "@/components/AuthProvider"
import { createProduct, createProductVariant, uploadProductImage, getAllCategories, getCategoriesByTopic, ocrPrefillProductFromImage, type ProductCreateInput, type ProductVariantCreateInput, type ProductOcrPrefillResponse } from "@/lib/adminApi"
import { SHOP_TOPICS, TOPIC_INFO, isValidTopic, type ShopTopic } from "@/lib/shopApi"
import {
  COMPATIBILITY_MODES,
  PRODUCT_CONDITIONS,
  OEM_TYPES,
  FUEL_TYPES,
  TRANSMISSION_TYPES,
  BODY_TYPES,
  DRIVE_TYPES,
  POWER_SOURCES,
  INSTALLATION_DIFFICULTIES,
  resolveProductTypeFromCategory,
  listToCsv,
  csvToList,
  type ProductType,
} from "@/lib/filterAttributes"
import { PARTS_MAIN_CATEGORIES, getPartsDeepCategories, getPartsSubcategories, resolvePartsBranch } from "@/lib/partsTaxonomy"
import { ChevronLeft, ChevronRight, Plus, Trash2, Upload, X } from "lucide-react"
import Link from "next/link"
// Simple toast implementation - replace with proper toast library if needed
function showToast(message: string, type: 'success' | 'error' | 'warning' = 'success') {
  // For now, use console and alert - replace with proper toast library
  console.log(`[${type.toUpperCase()}] ${message}`)
  if (type === 'error') {
    alert(`Error: ${message}`)
  } else if (type === 'warning') {
    alert(`Warning: ${message}`)
  } else {
    // Success messages can be less intrusive
    console.log(`Success: ${message}`)
  }
}

interface Category {
  id: number
  name: string
  slug: string
  description?: string
  parentId?: number | string | null
  parent_id?: number | string | null
  parent?: { id?: number | string | null } | null
}

function resolvePartsMainFromCategory(categoryId: string, categories: Category[]): string {
  if (!categoryId) return ""

  const selectedId = Number.parseInt(categoryId, 10)
  if (!Number.isFinite(selectedId)) return ""

  const categoryMap = new Map<number, Category>()
  categories.forEach((category) => categoryMap.set(category.id, category))

  const resolveParentId = (category?: Category): number | undefined => {
    if (!category) return undefined

    const rawParentId = category.parentId ?? category.parent_id ?? category.parent?.id
    if (rawParentId === null || rawParentId === undefined || rawParentId === "") {
      return undefined
    }

    const parsedParentId = typeof rawParentId === "number" ? rawParentId : Number.parseInt(String(rawParentId), 10)
    return Number.isFinite(parsedParentId) ? parsedParentId : undefined
  }

  const matchMainBySlug = (slug?: string): string => {
    const normalizedSlug = (slug || "").toLowerCase()
    if (!normalizedSlug) return ""

    const matchedMain = PARTS_MAIN_CATEGORIES.find(
      (mainCategory) =>
        !mainCategory.hidden &&
        (normalizedSlug === `parts-${mainCategory.slug}` || normalizedSlug.startsWith(`parts-${mainCategory.slug}-`)),
    )

    return matchedMain?.slug || ""
  }

  let current: Category | undefined = categoryMap.get(selectedId)
  while (current) {
    const matchedBySlug = matchMainBySlug(current.slug)
    if (matchedBySlug) {
      return matchedBySlug
    }

    const parentId = resolveParentId(current)
    if (!parentId) {
      break
    }

    current = categoryMap.get(parentId)
  }

  const selectedCategory = categoryMap.get(selectedId)
  return matchMainBySlug(selectedCategory?.slug)
}

interface InfoSection {
  title: string
  content: string
  enabled: boolean
}

function getProductNamePlaceholder(productType?: ProductType): string {
  switch (productType) {
    case "car":
      return "e.g., BMW 330i M Sport"
    case "part":
      return "e.g., Brembo Front Brake Pads"
    case "custom":
      return "e.g., Custom Carbon Fiber Splitter"
    default:
      return "e.g., Product name"
  }
}

function NewProductPageContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const topicParam = searchParams.get('topic')
  const selectedTopic: ShopTopic = (topicParam && isValidTopic(topicParam)) ? topicParam : 'parts'

  const [loading, setLoading] = useState(false)
  const [ocrLoading, setOcrLoading] = useState(false)
  const [ocrResult, setOcrResult] = useState<ProductOcrPrefillResponse | null>(null)
  const ocrInputRef = useRef<HTMLInputElement | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [loadingCategories, setLoadingCategories] = useState(true)

  // Basic product info
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [price, setPrice] = useState("")
  const [sku, setSku] = useState("")
  const [stockQuantity, setStockQuantity] = useState("0")
  const [categoryId, setCategoryId] = useState<string>("")
  const [imageUrl, setImageUrl] = useState("")
  const [weight, setWeight] = useState("")
  const [brand, setBrand] = useState("")
  const [active, setActive] = useState(true)
  const [featured, setFeatured] = useState(false)
  const [quoteOnly, setQuoteOnly] = useState(selectedTopic === 'cars')
  const [productType, setProductType] = useState<ProductType | undefined>(selectedTopic === "cars" ? "car" : selectedTopic === "parts" ? "part" : "custom")
  const [condition, setCondition] = useState<string>("all")
  const [oemType, setOemType] = useState<string>("all")

  const [compatibilityMode, setCompatibilityMode] = useState<string>("universal")
  const [compatibleMakesInput, setCompatibleMakesInput] = useState("")
  const [compatibleModelsInput, setCompatibleModelsInput] = useState("")
  const [compatibleYearStart, setCompatibleYearStart] = useState("")
  const [compatibleYearEnd, setCompatibleYearEnd] = useState("")
  const [vinCompatible, setVinCompatible] = useState(false)

  const [carMake, setCarMake] = useState("")
  const [carModel, setCarModel] = useState("")
  const [carYear, setCarYear] = useState("")
  const [mileage, setMileage] = useState("")
  const [fuelType, setFuelType] = useState("all")
  const [transmission, setTransmission] = useState("all")
  const [bodyType, setBodyType] = useState("all")
  const [driveType, setDriveType] = useState("all")
  const [powerKw, setPowerKw] = useState("")
  const [vehicleColor, setVehicleColor] = useState("")
  const [warrantyIncluded, setWarrantyIncluded] = useState(false)

  const [partCategory, setPartCategory] = useState("")
  const [partsMainCategory, setPartsMainCategory] = useState("")
  const [partsSubCategory, setPartsSubCategory] = useState("")
  const [partsDeepCategory, setPartsDeepCategory] = useState("")
  const [partNumber, setPartNumber] = useState("")
  const [partPositionInput, setPartPositionInput] = useState("")
  const [partMaterial, setPartMaterial] = useState("")
  const [reconditioned, setReconditioned] = useState(false)
  const [wheelDiameterInch, setWheelDiameterInch] = useState("")
  const [wheelWidthInch, setWheelWidthInch] = useState("")
  const [wheelBoltPattern, setWheelBoltPattern] = useState("")
  const [wheelOffsetEt, setWheelOffsetEt] = useState("")
  const [engineType, setEngineType] = useState("")
  const [engineDisplacementCc, setEngineDisplacementCc] = useState("")
  const [enginePowerHp, setEnginePowerHp] = useState("")

  const [toolCategory, setToolCategory] = useState("")
  const [powerSource, setPowerSource] = useState("all")
  const [voltage, setVoltage] = useState("")
  const [torqueMinNm, setTorqueMinNm] = useState("")
  const [torqueMaxNm, setTorqueMaxNm] = useState("")
  const [driveSize, setDriveSize] = useState("")
  const [professionalGrade, setProfessionalGrade] = useState(false)
  const [isKit, setIsKit] = useState(false)

  const [customCategory, setCustomCategory] = useState("")
  const [styleTagsInput, setStyleTagsInput] = useState("")
  const [customFinish, setCustomFinish] = useState("")
  const [streetLegal, setStreetLegal] = useState(true)
  const [installationDifficulty, setInstallationDifficulty] = useState("all")

  // Info sections (1-7)
  const [infoSections, setInfoSections] = useState<InfoSection[]>([
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
    { title: "", content: "", enabled: false },
  ])

  // Variants
  const [variants, setVariants] = useState<ProductVariantCreateInput[]>([])
  const [showVariantForm, setShowVariantForm] = useState(false)
  const [newVariant, setNewVariant] = useState<ProductVariantCreateInput>({
    name: "",
    sku: "",
    price: undefined,
    stockQuantity: 0,
    active: true,
    defaultVariant: false,
    options: {},
  })

  // Images
  const [images, setImages] = useState<File[]>([])
  const [imageUrls, setImageUrls] = useState<string[]>([])
  const [mainImageIndex, setMainImageIndex] = useState<number | null>(null)

  useEffect(() => {
    loadCategories()
  }, [selectedTopic])

  useEffect(() => {
    const resolvedType = resolveProductTypeFromCategory(categoryId, categories)
    if (resolvedType) {
      setProductType(resolvedType)
    }

    if (resolvedType === "part") {
      const resolvedMain = resolvePartsMainFromCategory(categoryId, categories)
      if (resolvedMain && resolvedMain !== partsMainCategory) {
        setPartsMainCategory(resolvedMain)
        setPartsSubCategory("")
        setPartsDeepCategory("")
      }
    } else if (partsMainCategory) {
      setPartsMainCategory("")
      setPartsSubCategory("")
      setPartsDeepCategory("")
    }
  }, [categoryId, categories, partsMainCategory])

  async function loadCategories() {
    try {
      setLoadingCategories(true)
      // Load categories filtered by topic
      const cats = await getCategoriesByTopic(selectedTopic)
      // Exclude the root category itself - only show subcategories for product assignment
      const subcats = cats.filter(c => c.slug !== selectedTopic)
      setCategories(subcats)
    } catch (error) {
      console.error("Failed to load categories:", error)
      showToast("Failed to load categories", "error")
    } finally {
      setLoadingCategories(false)
    }
  }

  function openOcrPicker() {
    ocrInputRef.current?.click()
  }

  async function handleOcrFileSelect(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return

    try {
      setOcrLoading(true)
      const result = await ocrPrefillProductFromImage(file)
      setOcrResult(result)
      showToast(result.message || "OCR scan completed", result.implemented ? "success" : "warning")
    } catch (error: any) {
      showToast(error?.message || "OCR scan failed", "error")
    } finally {
      setOcrLoading(false)
      event.target.value = ""
    }
  }

  function applyOcrSuggestions() {
    if (!ocrResult?.fields) return

    const fieldValue = (key: string) => ocrResult.fields[key]?.value?.trim()
    const asBoolean = (key: string): boolean | undefined => {
      const value = fieldValue(key)
      if (!value) return undefined
      const normalized = value.toLowerCase()
      if (["true", "1", "yes"].includes(normalized)) return true
      if (["false", "0", "no"].includes(normalized)) return false
      return undefined
    }

    const asNumberString = (key: string): string | undefined => {
      const value = fieldValue(key)
      if (!value) return undefined
      const normalized = value.replace(/[^0-9.\-]/g, "")
      return normalized || undefined
    }

    const categorySlug = fieldValue("categorySlug")
    if (categorySlug) {
      const matched = categories.find((category) => category.slug.toLowerCase() === categorySlug.toLowerCase())
      if (matched) {
        setCategoryId(String(matched.id))
      }
    }

    const maybeName = fieldValue("name")
    if (maybeName) setName(maybeName)
    const maybeDescription = fieldValue("description")
    if (maybeDescription) setDescription(maybeDescription)
    const maybeBrand = fieldValue("brand")
    if (maybeBrand) setBrand(maybeBrand)
    const maybeCondition = fieldValue("condition")
    if (maybeCondition) setCondition(maybeCondition)
    const maybeType = fieldValue("productType")
    if (maybeType && ["car", "part", "custom"].includes(maybeType.toLowerCase())) {
      setProductType(maybeType.toLowerCase() as ProductType)
    }

    const maybePrice = asNumberString("price")
    if (maybePrice) setPrice(maybePrice)
    const maybeStock = asNumberString("stockQuantity")
    if (maybeStock) setStockQuantity(maybeStock)
    const maybeYear = asNumberString("year")
    if (maybeYear) setCarYear(maybeYear)
    const maybeMileage = asNumberString("mileage")
    if (maybeMileage) setMileage(maybeMileage)
    const maybePower = asNumberString("powerKw")
    if (maybePower) setPowerKw(maybePower)
    const maybeWeight = asNumberString("weight")
    if (maybeWeight) setWeight(maybeWeight)

    const maybeMake = fieldValue("make")
    if (maybeMake) setCarMake(maybeMake)
    const maybeModel = fieldValue("model")
    if (maybeModel) setCarModel(maybeModel)

    const maybeFuel = fieldValue("fuelType")
    if (maybeFuel && FUEL_TYPES.includes(maybeFuel as any)) setFuelType(maybeFuel)
    const maybeTransmission = fieldValue("transmission")
    if (maybeTransmission && TRANSMISSION_TYPES.includes(maybeTransmission as any)) setTransmission(maybeTransmission)
    const maybeBody = fieldValue("bodyType")
    if (maybeBody && BODY_TYPES.includes(maybeBody as any)) setBodyType(maybeBody)
    const maybeDrive = fieldValue("driveType")
    if (maybeDrive && DRIVE_TYPES.includes(maybeDrive as any)) setDriveType(maybeDrive)

    const maybeActive = asBoolean("active")
    if (maybeActive !== undefined) setActive(maybeActive)
    const maybeFeatured = asBoolean("featured")
    if (maybeFeatured !== undefined) setFeatured(maybeFeatured)
    const maybeQuoteOnly = asBoolean("quoteOnly")
    if (maybeQuoteOnly !== undefined) setQuoteOnly(maybeQuoteOnly)
    const maybeStreetLegal = asBoolean("streetLegal")
    if (maybeStreetLegal !== undefined) setStreetLegal(maybeStreetLegal)
    const maybeWarranty = asBoolean("warrantyIncluded")
    if (maybeWarranty !== undefined) setWarrantyIncluded(maybeWarranty)

    // Parts-specific fields
    const maybePartCategory = fieldValue("partCategory")
    if (maybePartCategory) setPartCategory(maybePartCategory)
    const maybePartsDeep = fieldValue("partsDeepCategory")
    if (maybePartsDeep) setPartsDeepCategory(maybePartsDeep)
    const maybePartPosition = fieldValue("partPosition")
    if (maybePartPosition) setPartPositionInput(maybePartPosition)

    // Wheel-specific fields
    const maybeWheelDiameter = asNumberString("wheelDiameterInch")
    if (maybeWheelDiameter) setWheelDiameterInch(maybeWheelDiameter)
    const maybeWheelWidth = asNumberString("wheelWidthInch")
    if (maybeWheelWidth) setWheelWidthInch(maybeWheelWidth)
    const maybeWheelBolt = fieldValue("wheelBoltPattern")
    if (maybeWheelBolt) setWheelBoltPattern(maybeWheelBolt)
    const maybeWheelOffset = asNumberString("wheelOffsetEt")
    if (maybeWheelOffset) setWheelOffsetEt(maybeWheelOffset)

    // Engine-specific fields
    const maybeEngineType = fieldValue("engineType")
    if (maybeEngineType) setEngineType(maybeEngineType)
    const maybeEngineDisplacement = asNumberString("engineDisplacementCc")
    if (maybeEngineDisplacement) setEngineDisplacementCc(maybeEngineDisplacement)
    const maybeEnginePower = asNumberString("enginePowerHp")
    if (maybeEnginePower) setEnginePowerHp(maybeEnginePower)

    showToast("OCR suggestions applied. Please review before saving.", "success")
  }

  function updateInfoSection(index: number, field: keyof InfoSection, value: string | boolean) {
    const updated = [...infoSections]
    updated[index] = { ...updated[index], [field]: value }
    setInfoSections(updated)
  }

  function addVariant() {
    if (!newVariant.name || !newVariant.sku) {
      showToast("Variant name and SKU are required", "error")
      return
    }

    setVariants([...variants, { ...newVariant }])
    setNewVariant({
      name: "",
      sku: "",
      price: undefined,
      stockQuantity: 0,
      active: true,
      defaultVariant: false,
      options: {},
    })
    setShowVariantForm(false)
  }

  function removeVariant(index: number) {
    setVariants(variants.filter((_, i) => i !== index))
  }

  function handleImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files || [])
    if (files.length === 0) return

    setImages((prev) => [...prev, ...files])

    files.forEach((file) => {
      const reader = new FileReader()
      reader.onload = (event) => {
        const imageDataUrl = event.target?.result as string
        if (!imageDataUrl) return
        setImageUrls((prev) => [...prev, imageDataUrl])
      }
      reader.readAsDataURL(file)
    })

    e.target.value = ""
  }

  function removeImage(index: number) {
    setImages(images.filter((_, i) => i !== index))
    setImageUrls(imageUrls.filter((_, i) => i !== index))
    if (mainImageIndex === index) {
      setMainImageIndex(null)
    } else if (mainImageIndex !== null && mainImageIndex > index) {
      setMainImageIndex(mainImageIndex - 1)
    }
  }

  function moveQueuedImage(index: number, direction: "left" | "right") {
    const targetIndex = direction === "left" ? index - 1 : index + 1
    if (targetIndex < 0 || targetIndex >= imageUrls.length) return

    setImages((prev) => {
      const next = [...prev]
        ;[next[index], next[targetIndex]] = [next[targetIndex], next[index]]
      return next
    })

    setImageUrls((prev) => {
      const next = [...prev]
        ;[next[index], next[targetIndex]] = [next[targetIndex], next[index]]
      return next
    })

    setMainImageIndex((prev) => {
      if (prev === null) return prev
      if (prev === index) return targetIndex
      if (prev === targetIndex) return index
      return prev
    })
  }

  function csvToArray(value: string): string[] {
    return value
      .split(",")
      .map((entry) => entry.trim())
      .filter(Boolean)
  }

  function validatePublishAttributes(): string | null {
    if (!active) return null

    if (!condition || condition === "all") {
      return "Condition is required for active products"
    }

    if (productType === "car") {
      if (!carMake || !carModel || !carYear) {
        return "Cars require make, model and year when active"
      }
    }

    if (compatibilityMode === "vehicle_specific") {
      if (!compatibleMakesInput || !compatibleModelsInput || !compatibleYearStart || !compatibleYearEnd) {
        return "Vehicle-specific compatibility requires make/model and year range when active"
      }
    }

    if (productType === "part") {
      if (!partsMainCategory || !partsSubCategory) {
        return "Parts require main and sub taxonomy categories"
      }
      if (partsBranch === "wheels" && (!wheelDiameterInch || !wheelWidthInch || !wheelBoltPattern || !wheelOffsetEt)) {
        return "Wheel parts require diameter, width, bolt pattern and offset ET"
      }
      if (partsBranch === "engines" && (!engineType || !engineDisplacementCc || !enginePowerHp)) {
        return "Engine parts require engine type, displacement and power"
      }
    }

    return null
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    if (!name || !price || !categoryId) {
      showToast("Please fill in all required fields", "error")
      return
    }

    const publishValidationError = validatePublishAttributes()
    if (publishValidationError) {
      showToast(publishValidationError, "error")
      return
    }

    if (productType === "tool") {
      showToast("Tools are no longer sold and cannot be created", "error")
      return
    }

    try {
      setLoading(true)

      // Build product data
      const productData: ProductCreateInput = {
        name,
        description: description || undefined,
        price: parseFloat(price),
        stockQuantity: parseInt(stockQuantity) || 0,
        sku: sku.trim() ? sku.trim() : undefined,
        categoryId: parseInt(categoryId),
        imageUrl: imageUrl || undefined,
        active,
        featured,
        quoteOnly,
        productType,
        condition: condition !== "all" ? condition : undefined,
        oemType: oemType !== "all" ? oemType : undefined,
        compatibilityMode: compatibilityMode || undefined,
        compatibleMakes: compatibleMakesInput ? csvToArray(compatibleMakesInput) : undefined,
        compatibleModels: compatibleModelsInput ? csvToArray(compatibleModelsInput) : undefined,
        compatibleYearStart: compatibleYearStart ? parseInt(compatibleYearStart, 10) : undefined,
        compatibleYearEnd: compatibleYearEnd ? parseInt(compatibleYearEnd, 10) : undefined,
        vinCompatible: compatibilityMode === "vehicle_specific" ? vinCompatible : undefined,
        make: carMake || undefined,
        model: carModel || undefined,
        year: carYear ? parseInt(carYear, 10) : undefined,
        mileage: mileage ? parseInt(mileage, 10) : undefined,
        fuelType: fuelType !== "all" ? fuelType : undefined,
        transmission: transmission !== "all" ? transmission : undefined,
        bodyType: bodyType !== "all" ? bodyType : undefined,
        driveType: driveType !== "all" ? driveType : undefined,
        powerKw: powerKw ? parseInt(powerKw, 10) : undefined,
        color: vehicleColor || undefined,
        warrantyIncluded: productType === "car" ? warrantyIncluded : undefined,
        partCategory: partCategory || undefined,
        partsMainCategory: partsMainCategory || undefined,
        partsSubCategory: partsSubCategory || undefined,
        partsDeepCategory: partsDeepCategory || undefined,
        partNumber: partNumber || undefined,
        partPosition: partPositionInput ? csvToArray(partPositionInput) : undefined,
        wheelDiameterInch: wheelDiameterInch ? parseFloat(wheelDiameterInch) : undefined,
        wheelWidthInch: wheelWidthInch ? parseFloat(wheelWidthInch) : undefined,
        wheelBoltPattern: wheelBoltPattern || undefined,
        wheelOffsetEt: wheelOffsetEt ? parseInt(wheelOffsetEt, 10) : undefined,
        engineType: engineType || undefined,
        engineDisplacementCc: engineDisplacementCc ? parseInt(engineDisplacementCc, 10) : undefined,
        enginePowerHp: enginePowerHp ? parseInt(enginePowerHp, 10) : undefined,
        material: partMaterial || undefined,
        reconditioned: reconditioned || undefined,
        toolCategory: toolCategory || undefined,
        powerSource: powerSource !== "all" ? powerSource : undefined,
        voltage: voltage ? parseInt(voltage, 10) : undefined,
        torqueMinNm: torqueMinNm ? parseInt(torqueMinNm, 10) : undefined,
        torqueMaxNm: torqueMaxNm ? parseInt(torqueMaxNm, 10) : undefined,
        driveSize: driveSize || undefined,
        professionalGrade: professionalGrade || undefined,
        isKit: isKit || undefined,
        customCategory: customCategory || undefined,
        styleTags: styleTagsInput ? csvToArray(styleTagsInput) : undefined,
        finish: customFinish || undefined,
        streetLegal: productType === "custom" ? streetLegal : undefined,
        installationDifficulty: installationDifficulty !== "all" ? installationDifficulty : undefined,
        weight: weight ? parseFloat(weight) : undefined,
        brand: brand || undefined,
      }

      // Add info sections
      infoSections.forEach((section, index) => {
        const sectionNum = index + 1
        if (section.enabled && section.title && section.content) {
          const target = productData as any
          target[`infoSection${sectionNum}Title`] = section.title
          target[`infoSection${sectionNum}Content`] = section.content
          target[`infoSection${sectionNum}Enabled`] = true
        }
      })

      // Create product
      const createdProduct = await createProduct(productData)
      const productId = createdProduct.id.toString()

      showToast("Product created successfully!", "success")

      // Upload images
      if (images.length > 0) {
        for (let i = 0; i < images.length; i++) {
          const isMain = mainImageIndex === i
          try {
            await uploadProductImage(productId, images[i], isMain)
          } catch (error) {
            console.error(`Failed to upload image ${i + 1}:`, error)
            showToast(`Failed to upload image ${i + 1}`, "warning")
          }
        }
      }

      // Create variants
      if (variants.length > 0) {
        for (const variant of variants) {
          try {
            await createProductVariant(productId, variant)
          } catch (error) {
            console.error(`Failed to create variant ${variant.name}:`, error)
            showToast(`Failed to create variant ${variant.name}`, "warning")
          }
        }
      }

      // Redirect to product list or edit page
      router.push(`/admin/products`)
    } catch (error: any) {
      console.error("Failed to create product:", error)
      showToast(error.message || "Failed to create product", "error")
    } finally {
      setLoading(false)
    }
  }

  const isCarProduct = productType === "car"
  const showVehicleTab = productType === "part" || productType === "custom"
  const namePlaceholder = getProductNamePlaceholder(productType)
  const partsSubOptions = getPartsSubcategories(partsMainCategory)
  const partsDeepOptions = getPartsDeepCategories(partsMainCategory, partsSubCategory)
  const partsBranch = resolvePartsBranch(partsMainCategory, partsSubCategory)

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/admin/products" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Products
          </Link>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="mb-8">
            <SectionHeading
              title={`Create New Product — ${TOPIC_INFO[selectedTopic].label}`}
              subtitle={`Add a new product to the ${TOPIC_INFO[selectedTopic].label.toLowerCase()} catalog`}
            />
          </div>

          <Tabs defaultValue="basic" className="space-y-6">
            <TabsList>
              <TabsTrigger value="basic">Basic Info</TabsTrigger>
              {!isCarProduct && <TabsTrigger value="details">Details</TabsTrigger>}
              {showVehicleTab && <TabsTrigger value="vehicle">Vehicle</TabsTrigger>}
              {productType === "car" && <TabsTrigger value="car-fields">Car Fields</TabsTrigger>}
              {productType === "part" && <TabsTrigger value="parts">Parts</TabsTrigger>}
              {productType === "custom" && <TabsTrigger value="custom">Custom</TabsTrigger>}
              <TabsTrigger value="images">Images</TabsTrigger>
              <TabsTrigger value="variants">Variants</TabsTrigger>
              <TabsTrigger value="info-sections">Info Sections</TabsTrigger>
            </TabsList>

            {/* Basic Info Tab */}
            <TabsContent value="basic" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>OCR Quick Fill (Beta)</CardTitle>
                  <CardDescription>Drop a product sheet photo to extract and prefill fields.</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div
                    className="rounded-lg border border-dashed p-6 text-center"
                    onDragOver={(event) => event.preventDefault()}
                    onDrop={async (event) => {
                      event.preventDefault()
                      const file = event.dataTransfer.files?.[0]
                      if (!file) return
                      try {
                        setOcrLoading(true)
                        const result = await ocrPrefillProductFromImage(file)
                        setOcrResult(result)
                        showToast(result.message || "OCR scan completed", result.implemented ? "success" : "warning")
                      } catch (error: any) {
                        showToast(error?.message || "OCR scan failed", "error")
                      } finally {
                        setOcrLoading(false)
                      }
                    }}
                  >
                    <p className="text-sm text-muted-foreground mb-3">Drop image here or choose file</p>
                    <Button type="button" variant="outline" onClick={openOcrPicker} disabled={ocrLoading}>
                      {ocrLoading ? "Scanning..." : "Scan Product Photo"}
                    </Button>
                    <input
                      ref={ocrInputRef}
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handleOcrFileSelect}
                    />
                  </div>

                  {ocrResult && (
                    <div className="rounded-md border p-3 space-y-3">
                      <div className="text-sm">
                        <p><strong>Provider:</strong> {ocrResult.provider || "n/a"}</p>
                        <p><strong>Status:</strong> {ocrResult.implemented ? "Ready" : "Not configured"}</p>
                        <p><strong>Message:</strong> {ocrResult.message}</p>
                        <p><strong>Detected fields:</strong> {Object.keys(ocrResult.fields || {}).length}</p>
                      </div>
                      {ocrResult.missingRequiredFields?.length > 0 && (
                        <p className="text-xs text-destructive">
                          Missing required suggestions: {ocrResult.missingRequiredFields.join(", ")}
                        </p>
                      )}
                      <Button type="button" onClick={applyOcrSuggestions} disabled={!ocrResult.implemented}>
                        Apply Suggested Fields
                      </Button>
                    </div>
                  )}
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Basic Information</CardTitle>
                  <CardDescription>Essential product details</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="name">Product Name *</Label>
                    <Input
                      id="name"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder={namePlaceholder}
                      required
                      minLength={2}
                      maxLength={200}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="sku">SKU (optional)</Label>
                    <Input
                      id="sku"
                      value={sku}
                      onChange={(e) => setSku(e.target.value)}
                      placeholder="Leave blank to auto-generate"
                      maxLength={50}
                    />
                    <p className="text-xs text-muted-foreground">
                      If left blank, the backend will generate a stable SKU on creation.
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="category">Category *</Label>
                    <Select value={categoryId} onValueChange={setCategoryId} required>
                      <SelectTrigger>
                        <SelectValue placeholder="Select a category" />
                      </SelectTrigger>
                      <SelectContent>
                        {loadingCategories ? (
                          <SelectItem value="loading" disabled>Loading categories...</SelectItem>
                        ) : categories.length > 0 ? (
                          categories.map((cat) => (
                            <SelectItem key={cat.id} value={cat.id.toString()}>
                              {cat.name}
                            </SelectItem>
                          ))
                        ) : (
                          <SelectItem value="none" disabled>No categories available</SelectItem>
                        )}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Product Type</Label>
                      <Input value={productType || ""} readOnly />
                    </div>
                    <div className="space-y-2">
                      <Label>Condition *</Label>
                      <Select value={condition} onValueChange={setCondition}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select condition" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Select condition</SelectItem>
                          {PRODUCT_CONDITIONS.map((value) => (
                            <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="price">Price *</Label>
                      <Input
                        id="price"
                        type="number"
                        step="0.01"
                        min="0"
                        value={price}
                        onChange={(e) => setPrice(e.target.value)}
                        placeholder="0.00"
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="stock">Stock Quantity</Label>
                      <Input
                        id="stock"
                        type="number"
                        min="0"
                        value={stockQuantity}
                        onChange={(e) => setStockQuantity(e.target.value)}
                        placeholder="0"
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="description">Description</Label>
                    <Textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Product description (max 1000 characters)"
                      maxLength={1000}
                      rows={4}
                    />
                    <p className="text-xs text-muted-foreground">{description.length}/1000 characters</p>
                  </div>

                  <div className="flex items-center space-x-6">
                    <div className="flex items-center space-x-2">
                      <Switch id="active" checked={active} onCheckedChange={setActive} />
                      <Label htmlFor="active">Active</Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Switch id="featured" checked={featured} onCheckedChange={setFeatured} />
                      <Label htmlFor="featured">Featured</Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Switch id="quoteOnly" checked={quoteOnly} onCheckedChange={setQuoteOnly} />
                      <Label htmlFor="quoteOnly">Request Quote Only</Label>
                    </div>
                  </div>
                  {quoteOnly && (
                    <p className="text-sm text-muted-foreground">
                      This product will show a &quot;Request Quote&quot; button instead of &quot;Add to Cart&quot;
                    </p>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            {/* Details Tab */}
            {!isCarProduct && (
              <TabsContent value="details" className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Additional Details</CardTitle>
                    <CardDescription>Optional product information</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor="brand">Brand</Label>
                        <Input
                          id="brand"
                          value={brand}
                          onChange={(e) => setBrand(e.target.value)}
                          placeholder="e.g., BMW, Brembo, Flow Forged"
                          maxLength={100}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>OEM Type</Label>
                        <Select value={oemType} onValueChange={setOemType}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select OEM type" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="all">Not specified</SelectItem>
                            {OEM_TYPES.map((value) => (
                              <SelectItem key={value} value={value}>{value.toUpperCase()}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {/* Vehicle Compatibility Tab */}
            {showVehicleTab && (
              <TabsContent value="vehicle" className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Vehicle Compatibility</CardTitle>
                    <CardDescription>Use for parts/custom</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="space-y-2">
                      <Label>Compatibility Mode</Label>
                      <Select value={compatibilityMode} onValueChange={setCompatibilityMode}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select mode" />
                        </SelectTrigger>
                        <SelectContent>
                          {COMPATIBILITY_MODES.map((value) => (
                            <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    {compatibilityMode === "vehicle_specific" && (
                      <>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label>Compatible Makes (comma-separated)</Label>
                            <Input value={compatibleMakesInput} onChange={(e) => setCompatibleMakesInput(e.target.value)} placeholder="BMW, Mercedes-Benz" />
                          </div>
                          <div className="space-y-2">
                            <Label>Compatible Models (comma-separated)</Label>
                            <Input value={compatibleModelsInput} onChange={(e) => setCompatibleModelsInput(e.target.value)} placeholder="330i, C250" />
                          </div>
                        </div>
                        <div className="grid grid-cols-3 gap-4">
                          <div className="space-y-2">
                            <Label>Year Start</Label>
                            <Input type="number" value={compatibleYearStart} onChange={(e) => setCompatibleYearStart(e.target.value)} />
                          </div>
                          <div className="space-y-2">
                            <Label>Year End</Label>
                            <Input type="number" value={compatibleYearEnd} onChange={(e) => setCompatibleYearEnd(e.target.value)} />
                          </div>
                          <div className="flex items-center space-x-2 pt-8">
                            <Switch checked={vinCompatible} onCheckedChange={setVinCompatible} />
                            <Label>VIN Compatible</Label>
                          </div>
                        </div>
                      </>
                    )}
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {productType === "car" && (
              <TabsContent value="car-fields" className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Car Attributes</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-3 gap-4">
                      <Input value={carMake} onChange={(e) => setCarMake(e.target.value)} placeholder="Make" />
                      <Input value={carModel} onChange={(e) => setCarModel(e.target.value)} placeholder="Model" />
                      <Input type="number" value={carYear} onChange={(e) => setCarYear(e.target.value)} placeholder="Year" />
                    </div>
                    <div className="grid grid-cols-3 gap-4">
                      <Input type="number" value={mileage} onChange={(e) => setMileage(e.target.value)} placeholder="Mileage" />
                      <Input type="number" value={powerKw} onChange={(e) => setPowerKw(e.target.value)} placeholder="Power (kW)" />
                      <Input value={vehicleColor} onChange={(e) => setVehicleColor(e.target.value)} placeholder="Color" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="car-weight">Weight (kg)</Label>
                      <Input
                        id="car-weight"
                        type="number"
                        step="0.001"
                        min="0"
                        value={weight}
                        onChange={(e) => setWeight(e.target.value)}
                        placeholder="0.000"
                      />
                    </div>
                    <div className="grid grid-cols-4 gap-4">
                      <Select value={fuelType} onValueChange={setFuelType}>
                        <SelectTrigger><SelectValue placeholder="Fuel" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Fuel</SelectItem>
                          {FUEL_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                      <Select value={transmission} onValueChange={setTransmission}>
                        <SelectTrigger><SelectValue placeholder="Transmission" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Transmission</SelectItem>
                          {TRANSMISSION_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                      <Select value={bodyType} onValueChange={setBodyType}>
                        <SelectTrigger><SelectValue placeholder="Body" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Body</SelectItem>
                          {BODY_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                      <Select value={driveType} onValueChange={setDriveType}>
                        <SelectTrigger><SelectValue placeholder="Drive" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Drive</SelectItem>
                          {DRIVE_TYPES.map((value) => <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Switch checked={warrantyIncluded} onCheckedChange={setWarrantyIncluded} />
                      <Label>Warranty Included</Label>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {productType === "part" && (
              <TabsContent value="parts" className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Part Attributes</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <Input value={partCategory} onChange={(e) => setPartCategory(e.target.value)} placeholder="Part category" />
                      <Input value={partNumber} onChange={(e) => setPartNumber(e.target.value)} placeholder="Part number" />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <Select value={partsSubCategory || "all"} onValueChange={(value) => {
                        const next = value === "all" ? "" : value
                        setPartsSubCategory(next)
                        setPartsDeepCategory("")
                      }}>
                        <SelectTrigger><SelectValue placeholder="Subcategory" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Select subcategory</SelectItem>
                          {partsSubOptions.map((sub) => <SelectItem key={sub} value={sub}>{sub.replace(/-/g, " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                      <Select value={partsDeepCategory || "all"} onValueChange={(value) => setPartsDeepCategory(value === "all" ? "" : value)}>
                        <SelectTrigger><SelectValue placeholder="Deep category (optional)" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Deep category (optional)</SelectItem>
                          {partsDeepOptions.map((deep) => <SelectItem key={deep} value={deep}>{deep.replace(/-/g, " ")}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <Input value={partPositionInput} onChange={(e) => setPartPositionInput(e.target.value)} placeholder="Position tags (front,left)" />
                      <Input value={partMaterial} onChange={(e) => setPartMaterial(e.target.value)} placeholder="Material" />
                    </div>

                    {partsBranch === "wheels" && (
                      <div className="grid grid-cols-4 gap-4">
                        <Input type="number" value={wheelDiameterInch} onChange={(e) => setWheelDiameterInch(e.target.value)} placeholder="Diameter (inch)" />
                        <Input type="number" value={wheelWidthInch} onChange={(e) => setWheelWidthInch(e.target.value)} placeholder="Width (inch)" />
                        <Input value={wheelBoltPattern} onChange={(e) => setWheelBoltPattern(e.target.value)} placeholder="Bolt pattern" />
                        <Input type="number" value={wheelOffsetEt} onChange={(e) => setWheelOffsetEt(e.target.value)} placeholder="Offset ET" />
                      </div>
                    )}

                    {partsBranch === "engines" && (
                      <div className="grid grid-cols-3 gap-4">
                        <Input value={engineType} onChange={(e) => setEngineType(e.target.value)} placeholder="Engine type" />
                        <Input type="number" value={engineDisplacementCc} onChange={(e) => setEngineDisplacementCc(e.target.value)} placeholder="Displacement (cc)" />
                        <Input type="number" value={enginePowerHp} onChange={(e) => setEnginePowerHp(e.target.value)} placeholder="Power (hp)" />
                      </div>
                    )}
                    <div className="flex items-center space-x-2">
                      <Switch checked={reconditioned} onCheckedChange={setReconditioned} />
                      <Label>Reconditioned</Label>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {productType === "custom" && (
              <TabsContent value="custom" className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Custom Attributes</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <Input value={customCategory} onChange={(e) => setCustomCategory(e.target.value)} placeholder="Custom category" />
                      <Input value={styleTagsInput} onChange={(e) => setStyleTagsInput(e.target.value)} placeholder="Style tags (sport,luxury)" />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <Input value={customFinish} onChange={(e) => setCustomFinish(e.target.value)} placeholder="Finish" />
                      <Select value={installationDifficulty} onValueChange={setInstallationDifficulty}>
                        <SelectTrigger><SelectValue placeholder="Installation difficulty" /></SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">Not specified</SelectItem>
                          {INSTALLATION_DIFFICULTIES.map((value) => (
                            <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Switch checked={streetLegal} onCheckedChange={setStreetLegal} />
                      <Label>Street Legal</Label>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            )}

            {/* Images Tab */}
            <TabsContent value="images" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Product Images</CardTitle>
                  <CardDescription>Upload product images (max 10 images)</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="imageUrl">Main Image URL</Label>
                    <Input
                      id="imageUrl"
                      value={imageUrl}
                      onChange={(e) => setImageUrl(e.target.value)}
                      placeholder="https://example.com/image.jpg"
                    />
                    <p className="text-xs text-muted-foreground">Optional: use a direct image URL or upload files below</p>
                  </div>

                  <Label htmlFor="image-upload" className="block cursor-pointer border-2 border-dashed border-muted-foreground/25 rounded-lg p-6">
                    <div className="flex flex-col items-center justify-center space-y-4">
                      <Upload className="h-12 w-12 text-muted-foreground" />
                      <div className="text-center">
                        <span className="text-primary hover:underline">Click to upload</span> or drag and drop
                        <Input
                          id="image-upload"
                          type="file"
                          accept="image/*"
                          multiple
                          onChange={handleImageSelect}
                          className="hidden"
                        />
                        <p className="text-xs text-muted-foreground mt-2">PNG, JPG, GIF up to 5MB each</p>
                      </div>
                    </div>
                  </Label>

                  {imageUrls.length > 0 && (
                    <div className="grid grid-cols-4 gap-4">
                      {imageUrls.map((url, index) => (
                        <div key={index} className="relative group">
                          <img
                            src={url}
                            alt={`Product image ${index + 1}`}
                            className="w-full h-32 object-cover rounded-lg border"
                          />
                          {mainImageIndex === index && (
                            <div className="absolute top-2 left-2 bg-primary text-primary-foreground text-xs px-2 py-1 rounded">
                              Main
                            </div>
                          )}
                          <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-lg flex items-center justify-center gap-2">
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              className="text-black hover:text-black"
                              onClick={() => moveQueuedImage(index, "left")}
                              disabled={index === 0}
                              title="Move left"
                            >
                              <ChevronLeft className="h-4 w-4" />
                            </Button>
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              className="text-black hover:text-black"
                              onClick={() => moveQueuedImage(index, "right")}
                              disabled={index === imageUrls.length - 1}
                              title="Move right"
                            >
                              <ChevronRight className="h-4 w-4" />
                            </Button>
                            <Button
                              type="button"
                              variant="secondary"
                              size="sm"
                              className="text-black hover:text-black"
                              onClick={() => setMainImageIndex(index)}
                            >
                              Set Main
                            </Button>
                            <Button
                              type="button"
                              variant="destructive"
                              size="sm"
                              onClick={() => removeImage(index)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            {/* Variants Tab */}
            <TabsContent value="variants" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Product Variants</CardTitle>
                  <CardDescription>Add product variants (e.g., sizes, colors)</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  {variants.length > 0 && (
                    <div className="space-y-2">
                      {variants.map((variant, index) => (
                        <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                          <div>
                            <p className="font-medium">{variant.name}</p>
                            <p className="text-sm text-muted-foreground">SKU: {variant.sku}</p>
                            {variant.price && <p className="text-sm">Price: ${variant.price}</p>}
                          </div>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => removeVariant(index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}

                  {showVariantForm ? (
                    <Card>
                      <CardContent className="pt-6 space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="variant-name">Variant Name *</Label>
                            <Input
                              id="variant-name"
                              value={newVariant.name}
                              onChange={(e) => setNewVariant({ ...newVariant, name: e.target.value })}
                              placeholder="e.g., 20x10.5 / Matte Black"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="variant-sku">Variant SKU *</Label>
                            <Input
                              id="variant-sku"
                              value={newVariant.sku}
                              onChange={(e) => setNewVariant({ ...newVariant, sku: e.target.value })}
                              placeholder="e.g., FF-8018-20X105-MBLK"
                            />
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-2">
                            <Label htmlFor="variant-price">Price Override</Label>
                            <Input
                              id="variant-price"
                              type="number"
                              step="0.01"
                              min="0"
                              value={newVariant.price || ""}
                              onChange={(e) => setNewVariant({ ...newVariant, price: e.target.value ? parseFloat(e.target.value) : undefined })}
                              placeholder="Optional override for this variant"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor="variant-stock">Stock Quantity</Label>
                            <Input
                              id="variant-stock"
                              type="number"
                              min="0"
                              value={newVariant.stockQuantity || 0}
                              onChange={(e) => setNewVariant({ ...newVariant, stockQuantity: parseInt(e.target.value) || 0 })}
                              placeholder="e.g., 12"
                            />
                          </div>
                        </div>
                        <div className="flex items-center space-x-4">
                          <div className="flex items-center space-x-2">
                            <Switch
                              checked={newVariant.active ?? true}
                              onCheckedChange={(checked) => setNewVariant({ ...newVariant, active: checked })}
                            />
                            <Label>Active</Label>
                          </div>
                          <div className="flex items-center space-x-2">
                            <Switch
                              checked={newVariant.defaultVariant ?? false}
                              onCheckedChange={(checked) => setNewVariant({ ...newVariant, defaultVariant: checked })}
                            />
                            <Label>Default Variant</Label>
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <Button type="button" onClick={addVariant}>
                            Add Variant
                          </Button>
                          <Button type="button" variant="outline" onClick={() => setShowVariantForm(false)}>
                            Cancel
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  ) : (
                    <Button type="button" variant="outline" onClick={() => setShowVariantForm(true)}>
                      <Plus className="mr-2 h-4 w-4" />
                      Add Variant
                    </Button>
                  )}
                </CardContent>
              </Card>
            </TabsContent>

            {/* Info Sections Tab */}
            <TabsContent value="info-sections" className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Information Sections</CardTitle>
                  <CardDescription>Add up to 7 custom information sections</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                  {infoSections.map((section, index) => (
                    <Card key={index}>
                      <CardHeader>
                        <div className="flex items-center justify-between">
                          <CardTitle className="text-lg">Section {index + 1}</CardTitle>
                          <div className="flex items-center space-x-2">
                            <Switch
                              checked={section.enabled}
                              onCheckedChange={(checked) => updateInfoSection(index, "enabled", checked)}
                            />
                            <Label>Enable</Label>
                          </div>
                        </div>
                      </CardHeader>
                      {section.enabled && (
                        <CardContent className="space-y-4">
                          <div className="space-y-2">
                            <Label htmlFor={`section-${index}-title`}>Title</Label>
                            <Input
                              id={`section-${index}-title`}
                              value={section.title}
                              onChange={(e) => updateInfoSection(index, "title", e.target.value)}
                              placeholder="e.g., Warranty Information"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor={`section-${index}-content`}>Content</Label>
                            <Textarea
                              id={`section-${index}-content`}
                              value={section.content}
                              onChange={(e) => updateInfoSection(index, "content", e.target.value)}
                              placeholder="Section content..."
                              rows={4}
                            />
                          </div>
                        </CardContent>
                      )}
                    </Card>
                  ))}
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>

          <div className="mt-6 flex justify-end gap-4">
            <Button type="button" variant="outline" onClick={() => router.back()}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Creating..." : "Create Product"}
            </Button>
          </div>
        </form>
      </Container>
    </section>
  )
}

export default function NewProductPage() {
  return (
    <RequireAuth allowedRoles={["owner"]}>
      <NewProductPageContent />
    </RequireAuth>
  )
}

