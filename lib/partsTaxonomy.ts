export type PartsMainSlug =
    | "engine-drivetrain"
    | "suspension-steering"
    | "brakes"
    | "wheels-tires"
    | "electrical-lighting"
    | "exterior-body"
    | "interior"
    | "cooling-hvac"
    | "maintenance-service"
    | "off-road-utility"
    | "uncategorized"

export type PartsTaxonomyTree = Record<PartsMainSlug, Record<string, string[]>>

export const PARTS_MAIN_CATEGORIES: Array<{ slug: PartsMainSlug; label: string; hidden?: boolean }> = [
    { slug: "engine-drivetrain", label: "Engine & Drivetrain" },
    { slug: "suspension-steering", label: "Suspension & Steering" },
    { slug: "brakes", label: "Brakes" },
    { slug: "wheels-tires", label: "Wheels & Tires" },
    { slug: "electrical-lighting", label: "Electrical & Lighting" },
    { slug: "exterior-body", label: "Exterior & Body" },
    { slug: "interior", label: "Interior" },
    { slug: "cooling-hvac", label: "Cooling & HVAC" },
    { slug: "maintenance-service", label: "Maintenance & Service" },
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
    "suspension-steering": {
        coilovers: ["coilovers", "springs", "top-mounts"],
        "shocks-struts": ["shocks", "struts"],
        "arms-links": ["control-arms", "tie-rods", "end-links"],
        bushings: ["bushings", "mounts"],
        chassis: ["strut-bars", "braces"],
    },
    brakes: {
        "brake-kits": ["big-brake-kits", "upgrade-kits"],
        calipers: ["calipers", "rebuild-kits"],
        "discs-rotors": ["discs", "rotors"],
        pads: ["pads"],
        "lines-fluid": ["brake-lines", "master-cylinders", "fluid"],
    },
    "wheels-tires": {
        wheels: ["alloy", "forged", "steel", "center-caps"],
        tires: ["summer", "winter", "all-season"],
        hardware: ["lug-nuts", "bolts", "studs", "spacers", "hub-rings", "tpms"],
    },
    "electrical-lighting": {
        "power-starting": ["batteries", "alternators", "starters"],
        wiring: ["harnesses", "relays", "fuses"],
        lighting: ["headlights", "taillights", "indicators", "led-kits", "light-bars"],
    },
    "exterior-body": {
        aero: ["splitters", "diffusers", "wings"],
        "body-parts": ["bumpers", "fenders", "hoods", "mirrors"],
        protection: ["skid-plates", "guards"],
    },
    interior: {
        "seats-mounts": ["seats", "rails"],
        controls: ["steering-wheels", "shifters"],
        gauges: ["gauges", "pods"],
        audio: ["head-units", "speakers"],
    },
    "cooling-hvac": {
        "ac-parts": ["compressors", "condensers"],
        cabin: ["cabin-filters", "blowers"],
    },
    "maintenance-service": {
        filters: ["oil", "air", "cabin"],
        fluids: ["oils", "coolants"],
        "belts-pulleys": ["belts", "pulleys"],
        "gaskets-seals": ["gaskets", "seals"],
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
    | "brakes"
    | "suspension"
    | "electrical-lighting"

export function resolvePartsBranch(main?: string, sub?: string): PartsBranchKey {
    if (!main && !sub) return "global"
    if (main === "wheels-tires" || sub === "wheels") return "wheels"
    if (sub === "engines") return "engines"
    if (sub === "turbochargers") return "turbochargers"
    if (main === "brakes") return "brakes"
    if (main === "suspension-steering") return "suspension"
    if (main === "electrical-lighting") return "electrical-lighting"
    return "global"
}