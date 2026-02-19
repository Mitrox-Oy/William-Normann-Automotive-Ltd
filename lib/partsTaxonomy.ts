export type PartsMainSlug =
    | "engine-drivetrain"
    | "wheels"
    | "others"
    | "off-road-utility"
    | "uncategorized"

export type PartsTaxonomyTree = Record<PartsMainSlug, Record<string, string[]>>

export const PARTS_MAIN_CATEGORIES: Array<{ slug: PartsMainSlug; label: string; hidden?: boolean }> = [
    { slug: "engine-drivetrain", label: "Engine & Drivetrain" },
    { slug: "wheels", label: "Wheels" },
    { slug: "others", label: "Others" },
    { slug: "off-road-utility", label: "Off-road & Utility", hidden: true },
    { slug: "uncategorized", label: "Uncategorized", hidden: true },
]

export const PARTS_TAXONOMY: PartsTaxonomyTree = {
    "engine-drivetrain": {
        engines: ["long-block", "short-block", "crate-engines"],
        turbochargers: ["turbos", "wastegates", "blow-off-valves", "turbo-manifolds", "lines-fittings", "gaskets-hardware"],
        intake: ["intakes", "air-filters", "throttle-bodies", "intake-manifolds"],
        exhaust: ["headers-manifolds", "downpipes", "cat-back", "mufflers", "catalytic-converters"],
        "fuel-system": ["injectors", "fuel-pumps", "rails", "regulators", "lines"],
        "cooling-engine": ["radiators", "intercoolers", "oil-coolers", "thermostats", "hoses"],
        ignition: ["coils", "plugs", "ignition-modules"],
        "sensors-electronics": ["o2", "maf", "map", "crank-cam", "boost-controllers"],
        "ecu-tuning": ["ecus", "piggybacks", "tuning-accessories"],
        transmission: ["gearboxes", "clutches", "flywheels", "torque-converters"],
        driveline: ["driveshafts", "axles", "cv-joints"],
        differential: ["diffs", "lsd", "mounts"],
    },
    wheels: {
        wheels: ["alloy", "forged", "steel", "center-caps"],
        tires: ["summer", "winter", "all-season"],
        hardware: ["lug-nuts", "bolts", "studs", "spacers", "hub-rings", "tpms"],
    },
    others: {
        // Miscellaneous parts - no specific subcategories
        // Products will be filtered primarily by car brand/model
        miscellaneous: ["misc"],
    },
    "off-road-utility": {
        recovery: ["winches", "recovery-kits"],
        protection: ["rock-sliders", "underbody-guards"],
    },
    uncategorized: {
        uncategorized: ["uncategorized"],
    },
}

export function getPartsSubcategories(main: string | undefined): string[] {
    if (!main) return []
    const key = main as PartsMainSlug
    return PARTS_TAXONOMY[key] ? Object.keys(PARTS_TAXONOMY[key]) : []
}

export function getPartsDeepCategories(main: string | undefined, sub: string | undefined): string[] {
    if (!main || !sub) return []
    const key = main as PartsMainSlug
    return PARTS_TAXONOMY[key]?.[sub] || []
}

export type PartsBranchKey =
    | "global"
    | "wheels"
    | "engines"
    | "turbochargers"

export function resolvePartsBranch(main?: string, sub?: string): PartsBranchKey {
    if (!main && !sub) return "global"
    if (main === "wheels" || sub === "wheels") return "wheels"
    if (sub === "engines") return "engines"
    if (sub === "turbochargers") return "turbochargers"
    return "global"
}