"use client"

import { Instagram, MessageCircle, Tag } from "lucide-react"
import Link from "next/link"

export function AnnouncementBar() {
    const announcements = [
        {
            icon: <MessageCircle className="h-4 w-4" />,
            text: "WhatsApp: +971 58 534 7970",
            href: "https://wa.me/971585347970",
        },
        {
            icon: <Instagram className="h-4 w-4" />,
            text: "Follow us @williamautomotive",
            href: "https://instagram.com/williamautomotive",
        },
        {
            icon: <Tag className="h-4 w-4" />,
            text: "Use code SAVE10 for 10% off your first order",
            href: "/shop",
        },
    ]

    return (
        <div className="sticky top-0 z-50 relative overflow-hidden bg-gray-900/40 backdrop-blur-2xl border-b border-white/10 py-2.5">
            <div className="flex animate-marquee-infinite">
                {/* Triple the items for seamless loop */}
                {[...announcements, ...announcements, ...announcements, ...announcements, ...announcements, ...announcements].map((item, index) => (
                    <Link
                        key={index}
                        href={item.href}
                        target={item.href.startsWith("http") ? "_blank" : undefined}
                        rel={item.href.startsWith("http") ? "noopener noreferrer" : undefined}
                        className="inline-flex items-center gap-2 px-8 text-sm text-white/90 hover:text-white transition-colors whitespace-nowrap"
                    >
                        {item.icon}
                        <span>{item.text}</span>
                    </Link>
                ))}
            </div>
        </div>
    )
}
