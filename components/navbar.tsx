"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Menu, X, User, Package, Heart, MapPin, Settings, LogOut, LayoutDashboard, ShoppingBag, Users, BarChart3 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Container } from "@/components/container"
import { CartDrawer } from "@/components/cart-drawer"
import { useAuth } from "@/components/AuthProvider"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { siteConfig } from "@/content/site"
import { cn } from "@/lib/utils"

export function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const pathname = usePathname()
  const { user, logout, isOwner, isCustomer, loading } = useAuth()

  useEffect(() => {
    function handleScroll() {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  const getUserInitials = () => {
    if (!user) return "U"
    return user.name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2)
  }

  const handleLogout = async () => {
    await logout()
  }

  return (
    <header className="sticky top-0 z-50 transition-all duration-300 pt-4">
      <Container>
        <nav
          className={cn(
            "mx-auto flex h-16 items-center justify-between rounded-2xl px-6 transition-all duration-300",
            "bg-gray-900/40 backdrop-blur-2xl border border-white/10 shadow-lg",
            scrolled && "bg-gray-900/50",
          )}
        >
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary font-bold text-white shadow-sm">
              WN
            </div>
            <span className="hidden font-bold text-white sm:inline-block">{siteConfig.company.shortName}</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden items-center gap-8 md:flex">
            {siteConfig.nav.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "text-sm font-medium text-white transition-colors hover:text-primary",
                  pathname === item.href && "text-primary",
                )}
              >
                {item.label}
              </Link>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <CartDrawer />
            
            {/* Auth Section - Desktop */}
            <div className="hidden md:flex items-center gap-2">
              {!loading && (
                <>
                  {!user ? (
                    <Button asChild size="sm" variant="default">
                      <Link href="/login">Sign in</Link>
                    </Button>
                  ) : isOwner ? (
                    // Admin Dropdown
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="gap-2 text-white hover:text-primary">
                          <Avatar className="h-7 w-7">
                            <AvatarImage src={user.avatar} alt={user.name} />
                            <AvatarFallback className="text-xs">{getUserInitials()}</AvatarFallback>
                          </Avatar>
                          <span className="hidden lg:inline">Admin</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-56">
                        <DropdownMenuLabel>
                          <div className="flex flex-col space-y-1">
                            <p className="text-sm font-medium leading-none">{user.name}</p>
                            <p className="text-xs leading-none text-muted-foreground">{user.email}</p>
                          </div>
                        </DropdownMenuLabel>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem asChild>
                          <Link href="/admin" className="cursor-pointer">
                            <LayoutDashboard className="mr-2 h-4 w-4" />
                            Dashboard
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/admin/products" className="cursor-pointer">
                            <ShoppingBag className="mr-2 h-4 w-4" />
                            Products
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/admin/orders" className="cursor-pointer">
                            <Package className="mr-2 h-4 w-4" />
                            Orders
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/admin/customers" className="cursor-pointer">
                            <Users className="mr-2 h-4 w-4" />
                            Customers
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/admin/inventory" className="cursor-pointer">
                            <BarChart3 className="mr-2 h-4 w-4" />
                            Inventory
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-destructive">
                          <LogOut className="mr-2 h-4 w-4" />
                          Sign out
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  ) : (
                    // Customer Dropdown
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="gap-2 text-white hover:text-primary">
                          <Avatar className="h-7 w-7">
                            <AvatarImage src={user.avatar} alt={user.name} />
                            <AvatarFallback className="text-xs">{getUserInitials()}</AvatarFallback>
                          </Avatar>
                          <span className="hidden lg:inline">Account</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-56">
                        <DropdownMenuLabel>
                          <div className="flex flex-col space-y-1">
                            <p className="text-sm font-medium leading-none">{user.name}</p>
                            <p className="text-xs leading-none text-muted-foreground">{user.email}</p>
                          </div>
                        </DropdownMenuLabel>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem asChild>
                          <Link href="/account" className="cursor-pointer">
                            <User className="mr-2 h-4 w-4" />
                            Profile
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/account/orders" className="cursor-pointer">
                            <Package className="mr-2 h-4 w-4" />
                            Orders
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/account/wishlist" className="cursor-pointer">
                            <Heart className="mr-2 h-4 w-4" />
                            Wishlist
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/account/addresses" className="cursor-pointer">
                            <MapPin className="mr-2 h-4 w-4" />
                            Addresses
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href="/account/settings" className="cursor-pointer">
                            <Settings className="mr-2 h-4 w-4" />
                            Settings
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-destructive">
                          <LogOut className="mr-2 h-4 w-4" />
                          Sign out
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
                </>
              )}
            </div>

            {/* Mobile Menu Button */}
            <Button variant="ghost" size="sm" className="md:hidden text-white" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </Button>
          </div>
        </nav>

        {/* Mobile Navigation */}
        {mobileMenuOpen && (
          <div className="mt-2 rounded-2xl bg-gray-900/40 backdrop-blur-2xl border border-white/10 pb-4 md:hidden">
            <div className="space-y-1 px-6 pt-4">
              {/* Nav Links */}
              {siteConfig.nav.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "block rounded-md px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-white/10",
                    pathname === item.href && "bg-white/10",
                  )}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  {item.label}
                </Link>
              ))}
              
              {/* Auth Section - Mobile */}
              {!loading && (
                <div className="pt-2 space-y-1">
                  {!user ? (
                    <Button asChild size="sm" className="w-full">
                      <Link href="/login" onClick={() => setMobileMenuOpen(false)}>
                        Sign in
                      </Link>
                    </Button>
                  ) : (
                    <>
                      {/* User Info */}
                      <div className="px-3 py-2 text-white">
                        <p className="text-sm font-medium">{user.name}</p>
                        <p className="text-xs text-muted-foreground">{user.email}</p>
                      </div>
                      
                      {/* Role-specific links */}
                      {isOwner ? (
                        <>
                          <Link href="/admin" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <LayoutDashboard className="mr-2 inline h-4 w-4" />
                            Dashboard
                          </Link>
                          <Link href="/admin/products" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <ShoppingBag className="mr-2 inline h-4 w-4" />
                            Products
                          </Link>
                          <Link href="/admin/orders" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <Package className="mr-2 inline h-4 w-4" />
                            Orders
                          </Link>
                          <Link href="/admin/customers" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <Users className="mr-2 inline h-4 w-4" />
                            Customers
                          </Link>
                        </>
                      ) : (
                        <>
                          <Link href="/account" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <User className="mr-2 inline h-4 w-4" />
                            Profile
                          </Link>
                          <Link href="/account/orders" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <Package className="mr-2 inline h-4 w-4" />
                            Orders
                          </Link>
                          <Link href="/account/wishlist" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <Heart className="mr-2 inline h-4 w-4" />
                            Wishlist
                          </Link>
                          <Link href="/account/addresses" className="block rounded-md px-3 py-2 text-sm text-white hover:bg-white/10" onClick={() => setMobileMenuOpen(false)}>
                            <MapPin className="mr-2 inline h-4 w-4" />
                            Addresses
                          </Link>
                        </>
                      )}
                      
                      <button
                        onClick={() => {
                          handleLogout()
                          setMobileMenuOpen(false)
                        }}
                        className="block w-full rounded-md px-3 py-2 text-left text-sm text-destructive hover:bg-white/10"
                      >
                        <LogOut className="mr-2 inline h-4 w-4" />
                        Sign out
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </Container>
    </header>
  )
}
