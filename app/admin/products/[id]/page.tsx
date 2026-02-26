"use client"

import { useEffect, useRef, useState, type CSSProperties } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { ChevronLeft, ChevronRight, Save, Plus, Trash2, Upload, X, GripVertical, RefreshCcw } from "lucide-react"
import { Container } from "@/components/container"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Switch } from "@/components/ui/switch"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
    getAdminProduct,
    updateProduct,
    createProductVariant,
    uploadProductImage,
    uploadVariantImage,
    getProductImages,
    deleteProductImage,
    reorderProductImages,
    replaceProductImage,
    setMainProductImage,
    updateProductVariant,
    deleteProductVariant,
    getAllCategories,
    type AdminProduct,
    type ProductVariantCreateInput
} from "@/lib/adminApi"
import type { ProductVariant } from "@/lib/shopApi"
import { getImageUrl } from "@/lib/utils"
import {
    BODY_TYPES,
    COMPATIBILITY_MODES,
    DRIVE_TYPES,
    FUEL_TYPES,
    INSTALLATION_DIFFICULTIES,
    OEM_TYPES,
    POWER_SOURCES,
    PRODUCT_CONDITIONS,
    TRANSMISSION_TYPES,
    resolveProductTypeFromCategory,
    type ProductType,
} from "@/lib/filterAttributes"
import { PARTS_MAIN_CATEGORIES, getPartsDeepCategories, getPartsSubcategories, resolvePartsBranch } from "@/lib/partsTaxonomy"

import {
    DndContext,
    PointerSensor,
    closestCenter,
    useSensor,
    useSensors,
    type DragEndEvent,
} from "@dnd-kit/core"
import {
    SortableContext,
    useSortable,
    arrayMove,
    rectSortingStrategy,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"

function showToast(message: string, type: 'success' | 'error' | 'warning' = 'success') {
    console.log(`[${type.toUpperCase()}] ${message}`)
    if (type === 'error' || type === 'warning') {
        alert(`${type === 'error' ? 'Error' : 'Warning'}: ${message}`)
    }
}

const CAR_CONDITION_SCORES = ["1", "2", "3", "4", "5"] as const

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
        case "tool":
            return "e.g., Digital Torque Wrench Set"
        case "custom":
            return "e.g., Custom Carbon Fiber Splitter"
        default:
            return "e.g., Product name"
    }
}


export default function EditProductPage() {
    const params = useParams()
    const router = useRouter()
    const productId = params.id as string

    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [product, setProduct] = useState<AdminProduct | null>(null)
    const [categories, setCategories] = useState<Category[]>([])
    const [loadingCategories, setLoadingCategories] = useState(true)

    // Form fields - Basic Info
    const [name, setName] = useState("")
    const [description, setDescription] = useState("")
    const [price, setPrice] = useState("")
    const [sku, setSku] = useState("")
    const [stockQuantity, setStockQuantity] = useState("")
    const [categoryId, setCategoryId] = useState<string>("")
    const [imageUrl, setImageUrl] = useState("")
    const [initialImageUrl, setInitialImageUrl] = useState("")
    const [weight, setWeight] = useState("")
    const [brand, setBrand] = useState("")
    const [active, setActive] = useState(true)
    const [featured, setFeatured] = useState(false)
    const [quoteOnly, setQuoteOnly] = useState(false)
    const [productType, setProductType] = useState<ProductType | undefined>(undefined)
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
    const [existingVariants, setExistingVariants] = useState<ProductVariant[]>([])
    const [variants, setVariants] = useState<(ProductVariantCreateInput & { imageFile?: File, imagePreview?: string })[]>([])
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
    const [variantImageFile, setVariantImageFile] = useState<File | null>(null)
    const [variantImagePreview, setVariantImagePreview] = useState<string | null>(null)

    // Images
    const [images, setImages] = useState<File[]>([])
    const [imageUrls, setImageUrls] = useState<string[]>([])
    const [mainImageIndex, setMainImageIndex] = useState<number | null>(null)
    type ExistingImage = { id: string, imageUrl: string, isMain: boolean, position: number }
    const [existingImages, setExistingImages] = useState<ExistingImage[]>([])
    const [imagesBusy, setImagesBusy] = useState(false)
    const [imageBatchProgress, setImageBatchProgress] = useState<{ current: number, total: number } | null>(null)
    const replaceInputRef = useRef<HTMLInputElement | null>(null)
    const [replaceTargetImageId, setReplaceTargetImageId] = useState<string | null>(null)
    const [editingVariant, setEditingVariant] = useState<ProductVariant | null>(null)
    const [showEditVariantForm, setShowEditVariantForm] = useState(false)
    const [editVariantImageFile, setEditVariantImageFile] = useState<File | null>(null)
    const [editVariantImagePreview, setEditVariantImagePreview] = useState<string | null>(null)

    useEffect(() => {
        loadCategories()
    }, [])

    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 6 } })
    )

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
            const cats = await getAllCategories()
            setCategories(cats)
        } catch (error) {
            console.error("Failed to load categories:", error)
            showToast("Failed to load categories", "error")
        } finally {
            setLoadingCategories(false)
        }
    }

    async function refreshExistingImages() {
        if (!productId) return
        try {
            setImagesBusy(true)
            const imgs = await getProductImages(productId)
            const mapped = (imgs || [])
                .filter((img) => Boolean(img.id))
                .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
                .map((img) => ({
                    id: img.id,
                    imageUrl: img.imageUrl,
                    isMain: Boolean(img.isMain),
                    position: img.position ?? 0,
                }))
            setExistingImages(mapped)
        } catch (error) {
            console.error("Failed to refresh product images:", error)
            // Keep existing state as-is; image tab UI will still allow uploads.
        } finally {
            setImagesBusy(false)
        }
    }

    useEffect(() => {
        if (!productId) return

        setLoading(true)
        getAdminProduct(productId)
            .then((product) => {
                setProduct(product)
                // Populate form fields
                setName(product.name || "")
                setDescription(product.description || "")
                setPrice(product.price?.toString() || "")
                setSku(product.sku || "")
                setStockQuantity(product.stockQuantity?.toString() || product.stockLevel?.toString() || "0")
                setCategoryId(product.categoryId?.toString() || product.category || "")
                setImageUrl(product.imageUrl || "")
                setInitialImageUrl(product.imageUrl || "")
                setWeight(product.weight?.toString() || "")
                setBrand(product.brand || "")
                setActive(product.active ?? true)
                setFeatured(product.featured ?? false)
                setQuoteOnly(product.quoteOnly ?? false)
                setCondition((product as any).condition || "all")
                setOemType((product as any).oemType || "all")
                setCompatibilityMode((product as any).compatibilityMode || "universal")
                setCompatibleMakesInput(((product as any).compatibleMakes || []).join(", "))
                setCompatibleModelsInput(((product as any).compatibleModels || []).join(", "))
                setCompatibleYearStart((product as any).compatibleYearStart?.toString() || "")
                setCompatibleYearEnd((product as any).compatibleYearEnd?.toString() || "")
                setVinCompatible(Boolean((product as any).vinCompatible))
                setCarMake((product as any).make || "")
                setCarModel((product as any).model || "")
                setCarYear((product as any).year?.toString() || "")
                setMileage((product as any).mileage?.toString() || "")
                setFuelType((product as any).fuelType || "all")
                setTransmission((product as any).transmission || "all")
                setBodyType((product as any).bodyType || "all")
                setDriveType((product as any).driveType || "all")
                setPowerKw((product as any).powerKw?.toString() || "")
                setVehicleColor((product as any).color || "")
                setWarrantyIncluded(Boolean((product as any).warrantyIncluded))
                setPartCategory((product as any).partCategory || "")
                setPartsMainCategory((product as any).partsMainCategory || "")
                setPartsSubCategory((product as any).partsSubCategory || "")
                setPartsDeepCategory((product as any).partsDeepCategory || "")
                setPartNumber((product as any).partNumber || "")
                setPartPositionInput(((product as any).partPosition || []).join(", "))
                setWheelDiameterInch((product as any).wheelDiameterInch?.toString() || "")
                setWheelWidthInch((product as any).wheelWidthInch?.toString() || "")
                setWheelBoltPattern((product as any).wheelBoltPattern || "")
                setWheelOffsetEt((product as any).wheelOffsetEt?.toString() || "")
                setEngineType((product as any).engineType || "")
                setEngineDisplacementCc((product as any).engineDisplacementCc?.toString() || "")
                setEnginePowerHp((product as any).enginePowerHp?.toString() || "")
                setPartMaterial((product as any).material || "")
                setReconditioned(Boolean((product as any).reconditioned))
                setToolCategory((product as any).toolCategory || "")
                setPowerSource((product as any).powerSource || "all")
                setVoltage((product as any).voltage?.toString() || "")
                setTorqueMinNm((product as any).torqueMinNm?.toString() || "")
                setTorqueMaxNm((product as any).torqueMaxNm?.toString() || "")
                setDriveSize((product as any).driveSize || "")
                setProfessionalGrade(Boolean((product as any).professionalGrade))
                setIsKit(Boolean((product as any).isKit))
                setCustomCategory((product as any).customCategory || "")
                setStyleTagsInput(((product as any).styleTags || []).join(", "))
                setCustomFinish((product as any).finish || "")
                setStreetLegal((product as any).streetLegal ?? true)
                setInstallationDifficulty((product as any).installationDifficulty || "all")

                // Populate existing images (prefer the dedicated images endpoint for stable ids/positions).
                refreshExistingImages()
                // Fallback for legacy data if endpoint fails.
                if ((!product.images || product.images.length === 0) && product.imageUrl) {
                    setExistingImages([{ id: '', imageUrl: product.imageUrl, isMain: true, position: 0 }])
                }

                // Populate existing variants
                if (product.variants && product.variants.length > 0) {
                    setExistingVariants(product.variants)
                }

                // Populate info sections if they exist
                const sections = [...infoSections]
                for (let i = 1; i <= 7; i++) {
                    const titleKey = `infoSection${i}Title` as keyof AdminProduct
                    const contentKey = `infoSection${i}Content` as keyof AdminProduct
                    const enabledKey = `infoSection${i}Enabled` as keyof AdminProduct

                    if (product[titleKey] || product[contentKey]) {
                        sections[i - 1] = {
                            title: (product[titleKey] as string) || "",
                            content: (product[contentKey] as string) || "",
                            enabled: (product[enabledKey] as boolean) ?? false,
                        }
                    }
                }
                setInfoSections(sections)

                setLoading(false)
            })
            .catch((err) => {
                console.error("Failed to load product:", err)
                setError(err.message || "Failed to load product")
                setLoading(false)
            })
    }, [productId])

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

        // Store the variant with its image file if provided
        const variantToAdd: ProductVariantCreateInput & { imageFile?: File, imagePreview?: string } = {
            ...newVariant,
            imageFile: variantImageFile || undefined,
            imagePreview: variantImagePreview || undefined
        }

        setVariants([...variants, variantToAdd])
        setNewVariant({
            name: "",
            sku: "",
            price: undefined,
            stockQuantity: 0,
            active: true,
            defaultVariant: false,
            options: {},
        })
        setVariantImageFile(null)
        setVariantImagePreview(null)
        setShowVariantForm(false)
    }

    function handleVariantImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0]
        if (file) {
            setVariantImageFile(file)
            const reader = new FileReader()
            reader.onload = (e) => {
                setVariantImagePreview(e.target?.result as string)
            }
            reader.readAsDataURL(file)
        }
    }

    function handleEditVariantImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0]
        if (file) {
            setEditVariantImageFile(file)
            const reader = new FileReader()
            reader.onload = (e) => {
                setEditVariantImagePreview(e.target?.result as string)
            }
            reader.readAsDataURL(file)
        }
    }

    function removeVariant(index: number) {
        setVariants(variants.filter((_, i) => i !== index))
    }

    async function handleDeleteExistingImage(imageId: string) {
        // Legacy products can have only Product.imageUrl without ProductImage rows.
        // Those legacy images don't have an id, so they cannot be deleted via the images API.
        if (!imageId || Number.isNaN(Number.parseInt(imageId, 10))) {
            showToast('This is a legacy image and cannot be deleted. Upload a new image to manage images.', 'error')
            return
        }
        if (!confirm('Are you sure you want to delete this image?')) return

        try {
            await deleteProductImage(productId, imageId)
            await refreshExistingImages()
            showToast('Image deleted successfully', 'success')
        } catch (error) {
            console.error('Failed to delete image:', error)
            const msg = error instanceof Error ? error.message : ''
            showToast(msg ? `Failed to delete image: ${msg}` : 'Failed to delete image', 'error')
        }
    }

    async function handleSetMainImage(imageId: string) {
        if (!imageId || Number.isNaN(Number.parseInt(imageId, 10))) {
            showToast('This is a legacy image. Upload a new image to set a main image.', 'error')
            return
        }
        try {
            const selectedImage = existingImages.find((img) => img.id === imageId)
            await setMainProductImage(productId, imageId)
            await refreshExistingImages()
            if (selectedImage?.imageUrl) {
                setImageUrl(selectedImage.imageUrl)
                setInitialImageUrl(selectedImage.imageUrl)
            }
            showToast('Main image updated', 'success')
        } catch (error) {
            console.error('Failed to set main image:', error)
            showToast('Failed to set main image', 'error')
        }
    }

    function startReplaceExistingImage(imageId: string) {
        if (!imageId || Number.isNaN(Number.parseInt(imageId, 10))) {
            showToast('This is a legacy image and cannot be replaced. Upload a new image instead.', 'error')
            return
        }
        setReplaceTargetImageId(imageId)
        // reset + open file picker
        if (replaceInputRef.current) {
            replaceInputRef.current.value = ""
            replaceInputRef.current.click()
        }
    }

    async function handleReplaceFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0]
        const imageId = replaceTargetImageId
        if (!file || !imageId) return

        try {
            setImagesBusy(true)
            await replaceProductImage(productId, imageId, file)
            await refreshExistingImages()
            showToast("Image replaced successfully", "success")
        } catch (error) {
            console.error("Failed to replace image:", error)
            showToast("Failed to replace image", "error")
        } finally {
            setImagesBusy(false)
            setReplaceTargetImageId(null)
        }
    }

    async function handleExistingImagesDragEnd(event: DragEndEvent) {
        if (imagesBusy) return
        const { active, over } = event
        if (!over) return
        const activeId = String(active.id)
        const overId = String(over.id)
        if (activeId === overId) return

        const oldIndex = existingImages.findIndex((img) => img.id === activeId)
        const newIndex = existingImages.findIndex((img) => img.id === overId)
        if (oldIndex === -1 || newIndex === -1) return

        const prev = existingImages
        const moved = arrayMove(existingImages, oldIndex, newIndex).map((img, idx) => ({
            ...img,
            position: idx,
        }))
        setExistingImages(moved)

        try {
            setImagesBusy(true)
            await reorderProductImages(
                productId,
                moved.map((img, idx) => ({ imageId: img.id, position: idx }))
            )
            await refreshExistingImages()
            showToast("Image order updated", "success")
        } catch (error) {
            console.error("Failed to reorder images:", error)
            setExistingImages(prev)
            showToast("Failed to reorder images", "error")
        } finally {
            setImagesBusy(false)
        }
    }

    async function moveExistingImage(imageId: string, direction: "left" | "right") {
        if (imagesBusy) return

        const currentIndex = existingImages.findIndex((img) => img.id === imageId)
        if (currentIndex === -1) return

        const targetIndex = direction === "left" ? currentIndex - 1 : currentIndex + 1
        if (targetIndex < 0 || targetIndex >= existingImages.length) return

        const previous = existingImages
        const reordered = arrayMove(existingImages, currentIndex, targetIndex).map((img, idx) => ({
            ...img,
            position: idx,
        }))

        setExistingImages(reordered)

        try {
            setImagesBusy(true)
            await reorderProductImages(
                productId,
                reordered.map((img, idx) => ({ imageId: img.id, position: idx }))
            )
            await refreshExistingImages()
            showToast("Image order updated", "success")
        } catch (error) {
            console.error("Failed to reorder images:", error)
            setExistingImages(previous)
            showToast("Failed to reorder images", "error")
        } finally {
            setImagesBusy(false)
        }
    }

    function SortableExistingImageTile({ img }: { img: ExistingImage }) {
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ id: img.id })

        const style: CSSProperties = {
            transform: CSS.Transform.toString(transform),
            transition,
        }

        return (
            <div
                ref={setNodeRef}
                style={style}
                className={`relative group ${isDragging ? "z-10 opacity-90" : ""}`}
            >
                <img
                    src={getImageUrl(img.imageUrl)}
                    alt={`Existing image ${img.position + 1}`}
                    className="w-full h-32 object-cover rounded-lg border"
                />

                {img.isMain && (
                    <div className="absolute top-2 left-2 bg-primary text-primary-foreground text-xs px-2 py-1 rounded">
                        Main
                    </div>
                )}

                {/* Drag handle (always visible so reorder is discoverable) */}
                <div
                    className="absolute top-2 right-2 bg-background/90 border rounded p-1 shadow-sm cursor-grab active:cursor-grabbing"
                    {...attributes}
                    {...listeners}
                    title="Drag to reorder"
                >
                    <GripVertical className="h-4 w-4 text-muted-foreground" />
                </div>

                <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity rounded-lg flex items-center justify-center gap-2">
                    <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        className="text-black hover:text-black"
                        onClick={() => moveExistingImage(img.id, "left")}
                        disabled={imagesBusy || img.position === 0}
                        title="Move left"
                    >
                        <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        className="text-black hover:text-black"
                        onClick={() => moveExistingImage(img.id, "right")}
                        disabled={imagesBusy || img.position === existingImages.length - 1}
                        title="Move right"
                    >
                        <ChevronRight className="h-4 w-4" />
                    </Button>
                    {!img.isMain && (
                        <Button
                            type="button"
                            variant="secondary"
                            size="sm"
                            className="text-black hover:text-black"
                            onClick={() => handleSetMainImage(img.id)}
                            disabled={imagesBusy}
                        >
                            Set Main
                        </Button>
                    )}
                    <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        className="text-black hover:text-black"
                        onClick={() => startReplaceExistingImage(img.id)}
                        disabled={imagesBusy}
                        title="Replace file"
                    >
                        <RefreshCcw className="h-4 w-4" />
                    </Button>
                    <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        onClick={() => handleDeleteExistingImage(img.id)}
                        disabled={imagesBusy}
                        title="Delete image"
                    >
                        <Trash2 className="h-4 w-4" />
                    </Button>
                </div>
            </div>
        )
    }

    function handleEditVariant(variant: ProductVariant) {
        setEditingVariant(variant)
        setShowEditVariantForm(true)
        // Reset image state
        setEditVariantImageFile(null)
        setEditVariantImagePreview(null)
    }

    async function handleUpdateVariant() {
        if (!editingVariant) return

        try {
            // First upload image if a new one was selected
            let imageUrl = editingVariant.imageUrl
            if (editVariantImageFile) {
                const uploadedImage = await uploadVariantImage(productId, editingVariant.id.toString(), editVariantImageFile)
                imageUrl = uploadedImage.imageUrl
            }

            await updateProductVariant(productId, editingVariant.id.toString(), {
                name: editingVariant.name,
                sku: editingVariant.sku,
                price: editingVariant.price,
                stockQuantity: editingVariant.stockQuantity,
                active: editingVariant.active,
                defaultVariant: editingVariant.defaultVariant,
                options: editingVariant.options,
                imageUrl: imageUrl
            })

            setExistingVariants(existingVariants.map(v =>
                v.id === editingVariant.id ? { ...editingVariant, imageUrl } : v
            ))
            setEditingVariant(null)
            setShowEditVariantForm(false)
            setEditVariantImageFile(null)
            setEditVariantImagePreview(null)
            showToast('Variant updated successfully', 'success')
        } catch (error) {
            console.error('Failed to update variant:', error)
            showToast('Failed to update variant', 'error')
        }
    }

    async function handleDeleteVariant(variantId: number) {
        if (!confirm('Are you sure you want to delete this variant?')) return

        try {
            await deleteProductVariant(productId, variantId.toString())
            setExistingVariants(existingVariants.filter(v => v.id !== variantId))
            showToast('Variant deleted successfully', 'success')
        } catch (error) {
            console.error('Failed to delete variant:', error)
            showToast('Failed to delete variant', 'error')
        }
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

        if (productType === "car" && (!carMake || !carModel || !carYear)) {
            return "Cars require make, model and year when active"
        }

        if (compatibilityMode === "vehicle_specific" && (!compatibleMakesInput || !compatibleModelsInput || !compatibleYearStart || !compatibleYearEnd)) {
            return "Vehicle-specific compatibility requires make/model and year range when active"
        }

        return null
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        if (!product) return

        const publishValidationError = validatePublishAttributes()
        if (publishValidationError) {
            showToast(publishValidationError, "error")
            return
        }

        try {
            setSaving(true)

            const updateData: any = {
                name,
                description: description || undefined,
                price: parseFloat(price),
                sku,
                stockQuantity: parseInt(stockQuantity) || 0,
                weight: weight ? parseFloat(weight) : undefined,
                brand: brand || undefined,
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
            }

            if (imageUrl.trim() !== initialImageUrl.trim()) {
                updateData.imageUrl = imageUrl || undefined
            }

            if (categoryId) {
                updateData.categoryId = parseInt(categoryId)
            }

            // Add info sections
            infoSections.forEach((section, index) => {
                const sectionNum = index + 1
                if (section.enabled && section.title && section.content) {
                    updateData[`infoSection${sectionNum}Title`] = section.title
                    updateData[`infoSection${sectionNum}Content`] = section.content
                    updateData[`infoSection${sectionNum}Enabled`] = true
                } else {
                    // Clear disabled sections
                    updateData[`infoSection${sectionNum}Title`] = ""
                    updateData[`infoSection${sectionNum}Content`] = ""
                    updateData[`infoSection${sectionNum}Enabled`] = false
                }
            })

            await updateProduct(productId, updateData)
            showToast("Product updated successfully!", "success")

            // Upload new images if any
            if (images.length > 0) {
                setImageBatchProgress({ current: 0, total: images.length })
                const uploadedImageIds: Array<string | null> = new Array(images.length).fill(null)
                for (let i = 0; i < images.length; i++) {
                    const isMain = mainImageIndex === i
                    try {
                        const uploaded = await uploadProductImage(productId, images[i], isMain)
                        uploadedImageIds[i] = uploaded?.id?.toString?.() || null
                    } catch (error) {
                        console.error(`Failed to upload image ${i + 1}:`, error)
                        showToast(`Failed to upload image ${i + 1}`, "warning")
                    } finally {
                        setImageBatchProgress({ current: i + 1, total: images.length })
                    }
                }
                setImageBatchProgress(null)

                if (mainImageIndex !== null) {
                    const selectedMainUploadedId = uploadedImageIds[mainImageIndex]
                    if (selectedMainUploadedId) {
                        try {
                            await setMainProductImage(productId, selectedMainUploadedId)
                        } catch (error) {
                            console.error("Failed to set selected uploaded image as main:", error)
                        }
                    }
                }

                // Refresh existing images so positions/ids stay consistent after upload.
                await refreshExistingImages()

                // Clear local queue to avoid duplicate uploads on subsequent saves.
                setImages([])
                setImageUrls([])
                setMainImageIndex(null)
            }

            // Create new variants if any
            if (variants.length > 0) {
                for (let i = 0; i < variants.length; i++) {
                    const variant = variants[i]
                    try {
                        const createdVariant = await createProductVariant(productId, variant)

                        // Upload variant image if there's one
                        if (variant.imageFile) {
                            try {
                                await uploadVariantImage(productId, createdVariant.id.toString(), variant.imageFile)
                            } catch (imgError) {
                                console.error(`Failed to upload image for variant ${variant.name}:`, imgError)
                                showToast(`Variant created but image upload failed for ${variant.name}`, "warning")
                            }
                        }
                    } catch (error) {
                        console.error(`Failed to create variant ${variant.name}:`, error)
                        showToast(`Failed to create variant ${variant.name}`, "warning")
                    }
                }
            }

            router.push("/admin/products")
        } catch (error: any) {
            console.error("Failed to update product:", error)
            showToast(error.message || "Failed to update product", "error")
        } finally {
            setSaving(false)
        }
    }

    if (loading) {
        return (
            <section className="py-24 lg:py-32">
                <Container>
                    <div className="mb-8">
                        <Link href="/admin/products" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
                            <ChevronLeft className="mr-1 h-4 w-4" />
                            Back to Products
                        </Link>
                    </div>
                    <p>Loading product...</p>
                </Container>
            </section>
        )
    }

    if (error || !product) {
        return (
            <section className="py-24 lg:py-32">
                <Container>
                    <div className="mb-8">
                        <Link href="/admin/products" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
                            <ChevronLeft className="mr-1 h-4 w-4" />
                            Back to Products
                        </Link>
                    </div>
                    <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
                        <p className="text-destructive">{error || "Product not found"}</p>
                    </div>
                    <Button onClick={() => router.push("/admin/products")} className="mt-4">
                        Return to Products
                    </Button>
                </Container>
            </section>
        )
    }

    const isCarProduct = productType === "car"
    const usesConditionScore = productType === "car" || productType === "part" || productType === "custom"
    const conditionOptions = usesConditionScore ? CAR_CONDITION_SCORES : PRODUCT_CONDITIONS
    const hasLegacyConditionValue = usesConditionScore && condition !== "all" && !CAR_CONDITION_SCORES.includes(condition as any)
    const conditionPlaceholder = usesConditionScore ? "Select condition (1-5)" : "Select condition"
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
                        <h1 className="text-3xl font-bold mb-2">Edit Product</h1>
                        <p className="text-muted-foreground">Update product details</p>
                    </div>

                    <Tabs defaultValue="basic" className="space-y-6">
                        <TabsList>
                            <TabsTrigger value="basic">Basic Info</TabsTrigger>
                            {!isCarProduct && <TabsTrigger value="details">Details</TabsTrigger>}
                            {showVehicleTab && <TabsTrigger value="vehicle">Vehicle</TabsTrigger>}
                            {productType === "car" && <TabsTrigger value="car-fields">Car Fields</TabsTrigger>}
                            {productType === "part" && <TabsTrigger value="parts">Parts</TabsTrigger>}
                            {productType === "tool" && <TabsTrigger value="tools">Tools</TabsTrigger>}
                            {productType === "custom" && <TabsTrigger value="custom">Custom</TabsTrigger>}
                            <TabsTrigger value="images">Images</TabsTrigger>
                            <TabsTrigger value="variants">Variants</TabsTrigger>
                            <TabsTrigger value="info-sections">Info Sections</TabsTrigger>
                        </TabsList>

                        {/* Basic Info Tab */}
                        <TabsContent value="basic" className="space-y-6">
                            <Card>
                                <CardHeader>
                                    <CardTitle>Basic Information</CardTitle>
                                    <CardDescription>Core product details</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div>
                                        <Label htmlFor="name">Product Name *</Label>
                                        <Input
                                            id="name"
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            required
                                            placeholder={namePlaceholder}
                                        />
                                    </div>

                                    <div>
                                        <Label htmlFor="description">Description</Label>
                                        <Textarea
                                            id="description"
                                            value={description}
                                            onChange={(e) => setDescription(e.target.value)}
                                            rows={4}
                                            placeholder="Product description..."
                                            maxLength={1000}
                                        />
                                        <p className="text-xs text-muted-foreground mt-1">{description.length}/1000 characters</p>
                                    </div>

                                    <div className="grid gap-4 sm:grid-cols-2">
                                        <div>
                                            <Label htmlFor="sku">SKU *</Label>
                                            <Input
                                                id="sku"
                                                value={sku}
                                                onChange={(e) => setSku(e.target.value)}
                                                required
                                                placeholder="e.g., BRK-001"
                                            />
                                        </div>

                                        <div>
                                            <Label htmlFor="category">Category</Label>
                                            <Select value={categoryId} onValueChange={setCategoryId}>
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
                                    </div>

                                    <div className="grid gap-4 sm:grid-cols-2">
                                        <div>
                                            <Label>Product Type</Label>
                                            <Input value={productType || ""} readOnly />
                                        </div>
                                        <div>
                                            <Label>{usesConditionScore ? "Condition (1-5) *" : "Condition *"}</Label>
                                            <Select value={condition} onValueChange={setCondition}>
                                                <SelectTrigger>
                                                    <SelectValue placeholder={conditionPlaceholder} />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="all">{conditionPlaceholder}</SelectItem>
                                                    {conditionOptions.map((value) => (
                                                        <SelectItem key={value} value={value}>
                                                            {usesConditionScore ? `${value}/5` : value.replace("_", " ")}
                                                        </SelectItem>
                                                    ))}
                                                    {hasLegacyConditionValue && (
                                                        <SelectItem value={condition}>{condition}</SelectItem>
                                                    )}
                                                </SelectContent>
                                            </Select>
                                        </div>
                                    </div>

                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>Pricing & Inventory</CardTitle>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="grid gap-4 sm:grid-cols-3">
                                        <div>
                                            <Label htmlFor="price">Price (€) *</Label>
                                            <Input
                                                id="price"
                                                type="number"
                                                step="0.01"
                                                min="0"
                                                value={price}
                                                onChange={(e) => setPrice(e.target.value)}
                                                required
                                                placeholder="0.00"
                                            />
                                        </div>

                                        <div>
                                            <Label htmlFor="stockQuantity">Stock Quantity *</Label>
                                            <Input
                                                id="stockQuantity"
                                                type="number"
                                                min="0"
                                                value={stockQuantity}
                                                onChange={(e) => setStockQuantity(e.target.value)}
                                                required
                                                placeholder="0"
                                            />
                                        </div>

                                        {!isCarProduct && (
                                            <div>
                                                <Label htmlFor="weight">Weight (kg)</Label>
                                                <Input
                                                    id="weight"
                                                    type="number"
                                                    step="0.01"
                                                    min="0"
                                                    value={weight}
                                                    onChange={(e) => setWeight(e.target.value)}
                                                    placeholder="0.00"
                                                />
                                            </div>
                                        )}
                                    </div>
                                </CardContent>
                            </Card>

                            <Card>
                                <CardHeader>
                                    <CardTitle>Product Status</CardTitle>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <div>
                                            <Label htmlFor="active">Active</Label>
                                            <p className="text-sm text-muted-foreground">
                                                Product is visible in the shop
                                            </p>
                                        </div>
                                        <Switch
                                            id="active"
                                            checked={active}
                                            onCheckedChange={setActive}
                                        />
                                    </div>

                                    <div className="flex items-center justify-between">
                                        <div>
                                            <Label htmlFor="featured">Featured</Label>
                                            <p className="text-sm text-muted-foreground">
                                                Show on homepage and featured sections
                                            </p>
                                        </div>
                                        <Switch
                                            id="featured"
                                            checked={featured}
                                            onCheckedChange={setFeatured}
                                        />
                                    </div>

                                    <div className="flex items-center justify-between">
                                        <div>
                                            <Label htmlFor="quoteOnly">Request Quote Only</Label>
                                            <p className="text-sm text-muted-foreground">
                                                Show &quot;Request Quote&quot; instead of &quot;Add to Cart&quot;
                                            </p>
                                        </div>
                                        <Switch
                                            id="quoteOnly"
                                            checked={quoteOnly}
                                            onCheckedChange={setQuoteOnly}
                                        />
                                    </div>
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
                                        <div className="grid gap-4 sm:grid-cols-2">
                                            <div>
                                                <Label htmlFor="brand">Brand</Label>
                                                <Input
                                                    id="brand"
                                                    value={brand}
                                                    onChange={(e) => setBrand(e.target.value)}
                                                    placeholder="e.g., Bosch"
                                                    maxLength={100}
                                                />
                                            </div>
                                            <div>
                                                <Label>OEM Type</Label>
                                                <Select value={oemType} onValueChange={setOemType}>
                                                    <SelectTrigger>
                                                        <SelectValue placeholder="Select OEM type" />
                                                    </SelectTrigger>
                                                    <SelectContent>
                                                        <SelectItem value="all">Not specified</SelectItem>
                                                        {OEM_TYPES.map((value) => (
                                                            <SelectItem key={value} value={value}>
                                                                {value.toUpperCase()}
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                            </div>
                                        </div>
                                    </CardContent>
                                </Card>
                            </TabsContent>
                        )}

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
                                                        <SelectItem key={value} value={value}>
                                                            {value.replace("_", " ")}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </div>

                                        {compatibilityMode === "vehicle_specific" && (
                                            <>
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="space-y-2">
                                                        <Label>Compatible Makes (comma-separated)</Label>
                                                        <Input
                                                            value={compatibleMakesInput}
                                                            onChange={(e) => setCompatibleMakesInput(e.target.value)}
                                                            placeholder="BMW, Mercedes-Benz"
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label>Compatible Models (comma-separated)</Label>
                                                        <Input
                                                            value={compatibleModelsInput}
                                                            onChange={(e) => setCompatibleModelsInput(e.target.value)}
                                                            placeholder="330i, C250"
                                                        />
                                                    </div>
                                                </div>
                                                <div className="grid grid-cols-3 gap-4">
                                                    <div className="space-y-2">
                                                        <Label>Year Start</Label>
                                                        <Input
                                                            type="number"
                                                            value={compatibleYearStart}
                                                            onChange={(e) => setCompatibleYearStart(e.target.value)}
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label>Year End</Label>
                                                        <Input
                                                            type="number"
                                                            value={compatibleYearEnd}
                                                            onChange={(e) => setCompatibleYearEnd(e.target.value)}
                                                        />
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
                                                step="0.01"
                                                min="0"
                                                value={weight}
                                                onChange={(e) => setWeight(e.target.value)}
                                                placeholder="0.00"
                                            />
                                        </div>
                                        <div className="grid grid-cols-4 gap-4">
                                            <Select value={fuelType} onValueChange={setFuelType}>
                                                <SelectTrigger><SelectValue placeholder="Fuel" /></SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="all">Fuel</SelectItem>
                                                    {FUEL_TYPES.map((value) => (
                                                        <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <Select value={transmission} onValueChange={setTransmission}>
                                                <SelectTrigger><SelectValue placeholder="Transmission" /></SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="all">Transmission</SelectItem>
                                                    {TRANSMISSION_TYPES.map((value) => (
                                                        <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <Select value={bodyType} onValueChange={setBodyType}>
                                                <SelectTrigger><SelectValue placeholder="Body" /></SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="all">Body</SelectItem>
                                                    {BODY_TYPES.map((value) => (
                                                        <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <Select value={driveType} onValueChange={setDriveType}>
                                                <SelectTrigger><SelectValue placeholder="Drive" /></SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="all">Drive</SelectItem>
                                                    {DRIVE_TYPES.map((value) => (
                                                        <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                                                    ))}
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

                        {productType === "tool" && (
                            <TabsContent value="tools" className="space-y-6">
                                <Card>
                                    <CardHeader>
                                        <CardTitle>Tool Attributes</CardTitle>
                                    </CardHeader>
                                    <CardContent className="space-y-4">
                                        <div className="grid grid-cols-3 gap-4">
                                            <Input value={toolCategory} onChange={(e) => setToolCategory(e.target.value)} placeholder="Tool category" />
                                            <Input type="number" value={voltage} onChange={(e) => setVoltage(e.target.value)} placeholder="Voltage" />
                                            <Input value={driveSize} onChange={(e) => setDriveSize(e.target.value)} placeholder='Drive size (e.g. 1/2")' />
                                        </div>
                                        <div className="grid grid-cols-3 gap-4">
                                            <Input type="number" value={torqueMinNm} onChange={(e) => setTorqueMinNm(e.target.value)} placeholder="Torque min (Nm)" />
                                            <Input type="number" value={torqueMaxNm} onChange={(e) => setTorqueMaxNm(e.target.value)} placeholder="Torque max (Nm)" />
                                            <Select value={powerSource} onValueChange={setPowerSource}>
                                                <SelectTrigger><SelectValue placeholder="Power source" /></SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="all">Power source</SelectItem>
                                                    {POWER_SOURCES.map((value) => (
                                                        <SelectItem key={value} value={value}>{value.replace("_", " ")}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </div>
                                        <div className="flex items-center gap-6">
                                            <div className="flex items-center space-x-2">
                                                <Switch checked={professionalGrade} onCheckedChange={setProfessionalGrade} />
                                                <Label>Professional Grade</Label>
                                            </div>
                                            <div className="flex items-center space-x-2">
                                                <Switch checked={isKit} onCheckedChange={setIsKit} />
                                                <Label>Kit</Label>
                                            </div>
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

                                    {/* Existing Images */}
                                    {existingImages.length > 0 && (
                                        <div>
                                            <Label className="mb-2 block">Current Images</Label>
                                            <p className="text-xs text-muted-foreground mb-3">
                                                Use drag handle or left/right arrows to reorder. Changes save immediately.
                                            </p>

                                            {/* Hidden input used for "Replace" actions */}
                                            <input
                                                ref={replaceInputRef}
                                                type="file"
                                                accept="image/*"
                                                className="hidden"
                                                onChange={handleReplaceFileSelected}
                                            />

                                            <DndContext
                                                sensors={sensors}
                                                collisionDetection={closestCenter}
                                                onDragEnd={handleExistingImagesDragEnd}
                                            >
                                                <SortableContext
                                                    items={existingImages.filter((img) => Boolean(img.id)).map((img) => img.id)}
                                                    strategy={rectSortingStrategy}
                                                >
                                                    <div className="grid grid-cols-4 gap-4 mb-4">
                                                        {existingImages.map((img) => (
                                                            img.id ? (
                                                                <SortableExistingImageTile key={img.id} img={img} />
                                                            ) : (
                                                                <div key={`existing-fallback-${img.position}`} className="relative group">
                                                                    <img
                                                                        src={getImageUrl(img.imageUrl)}
                                                                        alt={`Existing image ${img.position + 1}`}
                                                                        className="w-full h-32 object-cover rounded-lg border"
                                                                    />
                                                                    {img.isMain && (
                                                                        <div className="absolute top-2 left-2 bg-primary text-primary-foreground text-xs px-2 py-1 rounded">
                                                                            Main
                                                                        </div>
                                                                    )}
                                                                </div>
                                                            )
                                                        ))}
                                                    </div>
                                                </SortableContext>
                                            </DndContext>
                                        </div>
                                    )}

                                    {/* Upload New Images */}
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

                                    {/* New Images Preview */}
                                    {imageUrls.length > 0 && (
                                        <div>
                                            <Label className="mb-2 block">New Images to Upload</Label>
                                            <div className="grid grid-cols-4 gap-4">
                                                {imageUrls.map((url, index) => (
                                                    <div key={`new-${index}`} className="relative group">
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
                                    {/* Existing Variants */}
                                    {existingVariants.length > 0 && (
                                        <div className="space-y-2">
                                            <Label>Current Variants</Label>
                                            {existingVariants.map((variant, index) => (
                                                <div key={`existing-${variant.id}`} className="flex items-center justify-between p-3 border rounded-lg bg-muted/30">
                                                    <div className="flex items-center gap-4">
                                                        {variant.imageUrl && (
                                                            <img
                                                                src={variant.imageUrl}
                                                                alt={variant.name}
                                                                className="w-12 h-12 object-cover rounded"
                                                            />
                                                        )}
                                                        <div>
                                                            <p className="font-medium">{variant.name}</p>
                                                            <p className="text-sm text-muted-foreground">SKU: {variant.sku}</p>
                                                            {variant.price && <p className="text-sm">Price: €{variant.price}</p>}
                                                            <p className="text-sm text-muted-foreground">Stock: {variant.stockQuantity}</p>
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center gap-2">
                                                        {variant.defaultVariant && (
                                                            <span className="text-xs bg-primary text-primary-foreground px-2 py-1 rounded">Default</span>
                                                        )}
                                                        {variant.active ? (
                                                            <span className="text-xs bg-green-500 text-white px-2 py-1 rounded">Active</span>
                                                        ) : (
                                                            <span className="text-xs bg-gray-500 text-white px-2 py-1 rounded">Inactive</span>
                                                        )}
                                                        <Button
                                                            type="button"
                                                            variant="outline"
                                                            size="sm"
                                                            onClick={() => handleEditVariant(variant)}
                                                        >
                                                            Edit
                                                        </Button>
                                                        <Button
                                                            type="button"
                                                            variant="destructive"
                                                            size="sm"
                                                            onClick={() => handleDeleteVariant(variant.id)}
                                                        >
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {/* New Variants to Create */}
                                    {variants.length > 0 && (
                                        <div className="space-y-2">
                                            <Label>New Variants to Create</Label>
                                            {variants.map((variant, index) => (
                                                <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                                                    <div className="flex items-center gap-4">
                                                        {variant.imagePreview && (
                                                            <img
                                                                src={variant.imagePreview}
                                                                alt={variant.name}
                                                                className="w-12 h-12 object-cover rounded"
                                                            />
                                                        )}
                                                        <div>
                                                            <p className="font-medium">{variant.name}</p>
                                                            <p className="text-sm text-muted-foreground">SKU: {variant.sku}</p>
                                                            {variant.price && <p className="text-sm">Price: €{variant.price}</p>}
                                                            {variant.imageFile && (
                                                                <p className="text-sm text-primary">📷 Image attached</p>
                                                            )}
                                                        </div>
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
                                                            placeholder="e.g., Pink / 128 GB"
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label htmlFor="variant-sku">Variant SKU *</Label>
                                                        <Input
                                                            id="variant-sku"
                                                            value={newVariant.sku}
                                                            onChange={(e) => setNewVariant({ ...newVariant, sku: e.target.value })}
                                                            placeholder="e.g., PHN-128-PNK"
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
                                                            placeholder="Leave empty to use product price"
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
                                                        />
                                                    </div>
                                                </div>

                                                {/* Variant Image Upload */}
                                                <div className="space-y-2">
                                                    <Label htmlFor="variant-image">Variant Image (Optional)</Label>
                                                    <div className="flex items-center gap-4">
                                                        <Input
                                                            id="variant-image"
                                                            type="file"
                                                            accept="image/*"
                                                            onChange={handleVariantImageSelect}
                                                            className="flex-1"
                                                        />
                                                        {variantImagePreview && (
                                                            <div className="relative">
                                                                <img
                                                                    src={variantImagePreview}
                                                                    alt="Variant preview"
                                                                    className="w-16 h-16 object-cover rounded border"
                                                                />
                                                                <Button
                                                                    type="button"
                                                                    variant="destructive"
                                                                    size="sm"
                                                                    className="absolute -top-2 -right-2 h-5 w-5 p-0"
                                                                    onClick={() => {
                                                                        setVariantImageFile(null)
                                                                        setVariantImagePreview(null)
                                                                    }}
                                                                >
                                                                    <X className="h-3 w-3" />
                                                                </Button>
                                                            </div>
                                                        )}
                                                    </div>
                                                    <p className="text-xs text-muted-foreground">Upload a variant-specific image (e.g., pink phone for pink variant)</p>
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
                                                    <Button type="button" variant="outline" onClick={() => {
                                                        setShowVariantForm(false)
                                                        setVariantImageFile(null)
                                                        setVariantImagePreview(null)
                                                    }}>
                                                        Cancel
                                                    </Button>
                                                </div>
                                            </CardContent>
                                        </Card>
                                    ) : (
                                        <Button type="button" variant="outline" onClick={() => setShowVariantForm(true)}>
                                            <Plus className="mr-2 h-4 w-4" />
                                            Add New Variant
                                        </Button>
                                    )}

                                    {/* Edit Variant Form */}
                                    {showEditVariantForm && editingVariant && (
                                        <Card className="border-primary">
                                            <CardHeader>
                                                <CardTitle>Edit Variant</CardTitle>
                                            </CardHeader>
                                            <CardContent className="space-y-4">
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-name">Variant Name *</Label>
                                                        <Input
                                                            id="edit-variant-name"
                                                            value={editingVariant.name}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, name: e.target.value })}
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-sku">Variant SKU *</Label>
                                                        <Input
                                                            id="edit-variant-sku"
                                                            value={editingVariant.sku}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, sku: e.target.value })}
                                                        />
                                                    </div>
                                                </div>
                                                <div className="grid grid-cols-2 gap-4">
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-price">Price Override</Label>
                                                        <Input
                                                            id="edit-variant-price"
                                                            type="number"
                                                            step="0.01"
                                                            min="0"
                                                            value={editingVariant.price || ""}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, price: e.target.value ? parseFloat(e.target.value) : undefined })}
                                                        />
                                                    </div>
                                                    <div className="space-y-2">
                                                        <Label htmlFor="edit-variant-stock">Stock Quantity</Label>
                                                        <Input
                                                            id="edit-variant-stock"
                                                            type="number"
                                                            min="0"
                                                            value={editingVariant.stockQuantity || 0}
                                                            onChange={(e) => setEditingVariant({ ...editingVariant, stockQuantity: parseInt(e.target.value) || 0 })}
                                                        />
                                                    </div>
                                                </div>

                                                {/* Variant Image Upload/Edit */}
                                                <div className="space-y-2">
                                                    <Label htmlFor="edit-variant-image">Variant Image</Label>
                                                    <div className="space-y-2">
                                                        {/* Current variant image */}
                                                        {editingVariant.imageUrl && !editVariantImagePreview && (
                                                            <div className="flex items-center gap-4">
                                                                <div className="relative">
                                                                    <img
                                                                        src={getImageUrl(editingVariant.imageUrl)}
                                                                        alt="Current variant"
                                                                        className="w-20 h-20 object-cover rounded border"
                                                                    />
                                                                    <Button
                                                                        type="button"
                                                                        variant="destructive"
                                                                        size="sm"
                                                                        className="absolute -top-2 -right-2 h-5 w-5 p-0"
                                                                        onClick={() => {
                                                                            setEditingVariant({ ...editingVariant, imageUrl: undefined })
                                                                        }}
                                                                    >
                                                                        <X className="h-3 w-3" />
                                                                    </Button>
                                                                </div>
                                                                <span className="text-sm text-muted-foreground">Current image</span>
                                                            </div>
                                                        )}

                                                        {/* New image upload */}
                                                        <div className="flex items-center gap-4">
                                                            <Input
                                                                id="edit-variant-image"
                                                                type="file"
                                                                accept="image/*"
                                                                onChange={handleEditVariantImageSelect}
                                                                className="flex-1"
                                                            />
                                                            {editVariantImagePreview && (
                                                                <div className="relative">
                                                                    <img
                                                                        src={editVariantImagePreview}
                                                                        alt="New variant preview"
                                                                        className="w-16 h-16 object-cover rounded border"
                                                                    />
                                                                    <Button
                                                                        type="button"
                                                                        variant="destructive"
                                                                        size="sm"
                                                                        className="absolute -top-2 -right-2 h-5 w-5 p-0"
                                                                        onClick={() => {
                                                                            setEditVariantImageFile(null)
                                                                            setEditVariantImagePreview(null)
                                                                        }}
                                                                    >
                                                                        <X className="h-3 w-3" />
                                                                    </Button>
                                                                </div>
                                                            )}
                                                        </div>
                                                        <p className="text-xs text-muted-foreground">
                                                            {editingVariant.imageUrl || editVariantImagePreview
                                                                ? "Upload a new image to replace the current one"
                                                                : "Upload a variant-specific image"}
                                                        </p>
                                                    </div>
                                                </div>

                                                <div className="flex items-center space-x-4">
                                                    <div className="flex items-center space-x-2">
                                                        <Switch
                                                            checked={editingVariant.active ?? true}
                                                            onCheckedChange={(checked) => setEditingVariant({ ...editingVariant, active: checked })}
                                                        />
                                                        <Label>Active</Label>
                                                    </div>
                                                    <div className="flex items-center space-x-2">
                                                        <Switch
                                                            checked={editingVariant.defaultVariant ?? false}
                                                            onCheckedChange={(checked) => setEditingVariant({ ...editingVariant, defaultVariant: checked })}
                                                        />
                                                        <Label>Default Variant</Label>
                                                    </div>
                                                </div>
                                                <div className="flex gap-2">
                                                    <Button type="button" onClick={handleUpdateVariant}>
                                                        Save Changes
                                                    </Button>
                                                    <Button type="button" variant="outline" onClick={() => {
                                                        setShowEditVariantForm(false)
                                                        setEditingVariant(null)
                                                    }}>
                                                        Cancel
                                                    </Button>
                                                </div>
                                            </CardContent>
                                        </Card>
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

                    <div className="mt-6 flex gap-4">
                        <Button type="submit" disabled={saving} className="flex-1">
                            <Save className="mr-2 h-4 w-4" />
                            {saving
                                ? (imageBatchProgress
                                    ? `Saving... (Uploading images ${imageBatchProgress.current}/${imageBatchProgress.total})`
                                    : "Saving...")
                                : "Save Changes"}
                        </Button>
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => router.push("/admin/products")}
                        >
                            Cancel
                        </Button>
                    </div>
                </form>
            </Container>
        </section>
    )
}
