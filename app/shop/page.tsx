"use client"

import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { SHOP_TOPICS, TOPIC_INFO, type ShopTopic } from "@/lib/shopApi"
import Image from "next/image"
import Link from "next/link"
import { motion } from "framer-motion"

export default function ShopPage() {
  return (
    <section className="py-24 lg:py-32">
      <Container>
        <SectionHeading
          title="Shop"
          subtitle="Browse our automotive products and services"
          centered
          className="mb-12"
        />

        {/* Topic Tiles Grid - 4 tiles in one row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {SHOP_TOPICS.map((topic, i) => (
            <TopicTile key={topic} topic={topic} index={i} />
          ))}
        </div>
      </Container>
    </section>
  )
}

interface TopicTileProps {
  topic: ShopTopic
  index: number
}

function TopicTile({ topic, index }: TopicTileProps) {
  const info = TOPIC_INFO[topic]
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1, duration: 0.5 }}
    >
      <Link 
        href={`/shop/${topic}`}
        className="group block relative aspect-square overflow-hidden rounded-2xl bg-muted"
      >
        {/* Background Image */}
        <div className="absolute inset-0">
          <Image
            src={info.image}
            alt={info.label}
            fill
            className="object-cover transition-transform duration-500 group-hover:scale-110"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
          />
          {/* Dark overlay for text readability */}
          <div className="absolute inset-0 bg-black/40 transition-colors duration-300 group-hover:bg-black/50" />
        </div>

        {/* Centered Label */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="text-center">
            <h2 className="text-3xl sm:text-4xl lg:text-3xl xl:text-4xl font-bold text-white tracking-wider drop-shadow-lg">
              {info.label}
            </h2>
            <p className="mt-2 text-sm text-white/80 opacity-0 translate-y-2 transition-all duration-300 group-hover:opacity-100 group-hover:translate-y-0">
              {info.description}
            </p>
          </div>
        </div>

        {/* Hover border effect */}
        <div className="absolute inset-0 rounded-2xl border-2 border-transparent transition-colors duration-300 group-hover:border-primary/50" />
      </Link>
    </motion.div>
  )
}

