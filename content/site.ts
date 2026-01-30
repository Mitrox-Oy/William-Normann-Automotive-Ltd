export const siteConfig = {
  company: {
    name: "William Automotive",
    shortName: "William Automotive",
    tagline: "Connecting Automotive Markets Across Continents",
    description:
      "Specialized automotive parts sourcing connecting workshops, distributors, and enthusiasts with verified suppliers across EU, UK, MENA, and Asia.",
  },
  contact: {
    email: "info@williamnormann.com",
    phone: "+44 20 1234 5678",
    address: {
      line1: "123 Automotive Way",
      line2: "London, UK",
      postcode: "SW1A 1AA",
    },
  },
  social: {
    linkedin: "https://linkedin.com/company/williamnormann",
    twitter: "https://twitter.com/williamnormann",
    instagram: "https://instagram.com/williamautomotive",
  },
  nav: [
    { label: "Home", href: "/" },
    { label: "Shop", href: "/shop" },
    { label: "About & Contact", href: "/about" },
    { label: "Gallery", href: "/gallery" },
  ],
  hero: {
    headline: "Hard-to-Source Automotive Parts, Delivered Globally",
    subheadline:
      "From OEM classics to performance upgrades and rare SKUs. We connect serious workshops, distributors, and builders with verified suppliers across Europe, Middle East, and Asia.",
    primaryCTA: "Request a Quote",
    secondaryCTA: "Browse Shop",
    badges: ["First reply within 24 hours", "Cross-border sourcing: EU • UK • MENA • Asia"],
  },
  trust: [
    {
      title: "OEM & Verified Sources",
      description: "Factory-spec parts from authorized suppliers only",
    },
    {
      title: "Cross-Border Expertise",
      description: "Direct access to EU, UK, Middle East, and Asian markets",
    },
    {
      title: "24-Hour Response SLA",
      description: "Part numbers, origin, lead time, and FOB pricing",
    },
    {
      title: "Transparent Quoting",
      description: "Full breakdown: part cost, freight, duties, timeline",
    },
    {
      title: "Technical Support",
      description: "Compatibility checks and spec verification included",
    },
  ],
  services: [
    {
      title: "Turnkey Car Import Service",
      slug: "car-import",
      description:
        "Complete car import solution from start to finish. We handle everything from sourcing to delivery at your doorstep.",
      icon: "truck",
      forWho: "Private buyers • Dealerships • Collectors",
      turnaround: "Quote within 24h",
      featured: true,
      features: [
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
      ],
    },
    {
      title: "Parts & Engines Import by Commission",
      slug: "parts-engines-import",
      description:
        "Specialized import service for spare parts and engines. We source and import on commission basis for your specific needs.",
      icon: "search",
      forWho: "Workshops • Distributors • Restoration shops",
      turnaround: "Quote within 24h",
      features: [
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
      ],
    },
    {
      title: "Webshop with Partner & Own Inventory",
      slug: "webshop",
      description:
        "Access our online shop featuring products from trusted partners and our own inventory. Delivery within 4-7 days from market to market.",
      icon: "globe",
      forWho: "Businesses • Individual buyers • Workshops",
      turnaround: "Delivery 4-7 days",
      features: [
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
      ],
    },
    {
      title: "Global Product Warranty",
      slug: "warranty",
      description:
        "All products come with global warranty. Products are either inspected or new with manufacturer warranty.",
      icon: "wrench",
      forWho: "All customers • Global coverage",
      turnaround: "Warranty included",
      features: [
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
        "XXXX Text - Work in progress",
      ],
    },
  ],
  howItWorks: [
    {
      step: 1,
      title: "Submit Your Inquiry",
      description: "Tell us what you need - parts, quantities, and any specifications. We respond within 24 hours.",
    },
    {
      step: 2,
      title: "We Source & Match",
      description: "Our team searches our global network to find the best suppliers and pricing for your requirements.",
    },
    {
      step: 3,
      title: "Receive Your Offer",
      description: "Get a transparent quote with pricing, availability, and delivery timeline. No hidden fees.",
    },
    {
      step: 4,
      title: "Delivery & Support",
      description: "Once approved, we handle logistics and keep you updated. Full support until delivery and beyond.",
    },
  ],
  differentiator: {
    headline: "Global Sourcing, Simplified",
    description:
      "When local distributors can't deliver, we tap into verified supplier networks across EU, UK, MENA, and Asia. Direct access to discontinued lines, region-specific SKUs, and OEM archives—with transparent pricing and realistic lead times.",
    benefits: [
      "Direct factory and distributor relationships (no resellers)",
      "Access to region-locked SKUs (JDM, GCC-spec, Euro-only)",
      "Customs and compliance handled (HS codes, duties, COO docs)",
    ],
    learnMoreLink: "/services#global-sourcing",
  },
  about: {
    story:
      "William Automotive was founded with a simple mission: make global automotive sourcing accessible to everyone. Starting from a passion for classic cars and a frustration with limited local availability, we've grown into a trusted partner for workshops, enthusiasts, and distributors across three continents.\n\nToday, we leverage our extensive network and expertise to help clients find exactly what they need, wherever it comes from. Whether you're restoring a vintage classic, running a busy workshop, or managing wholesale distribution, we're here to make your automotive sourcing seamless and reliable.",
    values: [
      {
        title: "Trust & Transparency",
        description: "No hidden fees, no surprises. Clear communication every step of the way.",
      },
      {
        title: "Quality First",
        description: "We work only with verified suppliers and guarantee the quality of every part.",
      },
      {
        title: "Speed & Reliability",
        description: "Fast response times and dependable delivery. We respect your time.",
      },
      {
        title: "Global Expertise",
        description: "Deep knowledge of international markets, regulations, and logistics.",
      },
    ],
  },
  leadForm: {
    headline: "Get Your Quote",
    description:
      "Part numbers, vehicle details, quantities—whatever you've got. We'll respond within 24 hours with pricing, lead time, and shipping options.",
    responseTime: "Typical first reply: 24 hours on weekdays",
    interests: [
      "Hard-to-source parts",
      "Wholesale / bulk orders",
      "Cross-border sourcing",
      "Technical verification",
      "Custom project",
      "Other",
    ],
  },
  integrations: {
    headline: "Available Integrations",
    description:
      "We support various payment methods and integration options. Contact us to discuss your specific needs.",
    items: [
      { name: "Paytrail", type: "Payment" },
      { name: "PayPal", type: "Payment" },
      { name: "Klarna", type: "Payment" },
      { name: "Visa/Mastercard", type: "Payment" },
      { name: "Cryptocurrency", type: "Payment" },
      { name: "API Integration", type: "Technical" },
      { name: "Mailchimp / Brevo", type: "Marketing" },
      { name: "Calendly / Google Calendar", type: "Booking" },
      { name: "Instagram / YouTube", type: "Social" },
      { name: "Google Maps", type: "Location" },
      { name: "Typeform / Tally", type: "Forms" },
      { name: "Mitrox AI Advisor", type: "AI" },
    ],
  },
  testimonials: [
    {
      name: "James Fletcher",
      role: "Workshop Owner",
      company: "Manchester, UK",
      quote:
        "Sourced a discontinued E46 M3 CSL airbox from Germany in under a week. Pricing was 40% below UK suppliers.",
    },
    {
      name: "Omar Al-Rashid",
      role: "Parts Buyer",
      company: "Dubai, UAE",
      quote:
        "18 months of reliable JDM imports. Clean documentation, and they handle UK-UAE customs without issues.",
    },
    {
      name: "Sofia Bianchi",
      role: "Classic Car Restorer",
      company: "Milan, Italy",
      quote:
        "Found NOS Alfa Romeo Giulia trim pieces I'd hunted for two years. Coordinated shipping from three suppliers into one shipment.",
    },
  ],
  proofPoints: {
    headline: "What You Get in Every Quote",
    items: [
      { title: "Part Numbers & Alternatives", description: "OEM codes, cross-references, and verified aftermarket options" },
      { title: "Origin & Lead Time", description: "Source location, availability status, and realistic delivery estimates" },
      { title: "Shipping Options & Costs", description: "Multiple freight options with transparent pricing and tracking" },
      { title: "Duties & HS Codes Guidance", description: "Import classification, estimated duties, and compliance documentation" },
    ],
  },
  footer: {
    tagline: "Your trusted partner for global automotive solutions.",
    copyright: `© ${new Date().getFullYear()} William Automotive. All rights reserved.`,
    quickLinks: [
      { label: "Privacy Policy", href: "/privacy" },
      { label: "Terms of Service", href: "/terms" },
      { label: "Sitemap", href: "/sitemap.xml" },
    ],
  },
  blog: {
    posts: [
      {
        slug: "sourcing-rare-parts-guide",
        title: "The Ultimate Guide to Sourcing Rare Automotive Parts",
        excerpt:
          "Learn proven strategies for finding hard-to-locate OEM and aftermarket parts for your restoration projects.",
        date: "2024-01-15",
        readTime: 8,
        category: "Guides",
        image: "/automotive-parts-warehouse.jpg",
      },
      {
        slug: "cross-border-automotive-trade",
        title: "Navigating Cross-Border Automotive Trade in 2024",
        excerpt:
          "Essential insights into international automotive sourcing, customs, and logistics for workshops and distributors.",
        date: "2024-01-08",
        readTime: 6,
        category: "Industry",
        image: "/global-shipping-containers.jpg",
      },
      {
        slug: "oem-vs-aftermarket-parts",
        title: "OEM vs Aftermarket: Making the Right Choice",
        excerpt:
          "Understand the key differences, quality considerations, and cost implications when choosing automotive parts.",
        date: "2024-01-02",
        readTime: 5,
        category: "Education",
        image: "/car-engine-parts.png",
      },
    ],
  },
}
