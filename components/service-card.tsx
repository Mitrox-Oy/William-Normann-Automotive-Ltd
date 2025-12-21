import Link from "next/link"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { ArrowRight, Globe, Search, Truck, Wrench, Settings } from "lucide-react"

const iconMap = {
  search: Search,
  truck: Truck,
  globe: Globe,
  wrench: Wrench,
  settings: Settings,
}

interface ServiceCardProps {
  title: string
  description: string
  icon: keyof typeof iconMap
  slug: string
  forWho?: string
  turnaround?: string
}

export function ServiceCard({ title, description, icon, slug, forWho, turnaround }: ServiceCardProps) {
  const Icon = iconMap[icon] || Settings

  return (
    <Card className="flex h-full flex-col transition-shadow hover:shadow-lg">
      <CardHeader>
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
          <Icon className="h-6 w-6 text-primary" />
        </div>
        <CardTitle className="text-xl">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="flex-1">
        {(forWho || turnaround) && (
          <div className="space-y-2">
            {forWho && (
              <div className="flex items-center gap-2 text-sm">
                <Badge variant="secondary">{forWho}</Badge>
              </div>
            )}
            {turnaround && (
              <p className="text-sm text-muted-foreground">
                <span className="font-medium">Turnaround:</span> {turnaround}
              </p>
            )}
          </div>
        )}
      </CardContent>
      <CardFooter>
        <Button asChild variant="ghost" className="group w-full">
          <Link href={`/services#${slug}`}>
            Learn more
            <ArrowRight className="ml-2 h-4 w-4 transition-transform group-hover:translate-x-1" />
          </Link>
        </Button>
      </CardFooter>
    </Card>
  )
}
