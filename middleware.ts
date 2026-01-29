import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

/**
 * Middleware for route protection
 * 
 * Protects routes based on authentication and role:
 * - /admin/* requires owner role
 * - /account/* requires customer or owner role
 * 
 * Strategy:
 * Since we're using client-side auth with potential httpOnly cookies,
 * this middleware performs basic path matching and redirects.
 * The actual auth check happens client-side in AuthProvider.
 * 
 * For production with httpOnly cookies, you'd want to verify the
 * cookie here and extract user info from it.
 */

const publicPaths = ['/', '/shop', '/services', '/about', '/blog', '/login']
const accountPaths = ['/account']
const adminPaths = ['/admin']

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl
  
  // Allow public paths
  if (publicPaths.some(path => pathname === path || pathname.startsWith(`${path}/`))) {
    return NextResponse.next()
  }
  
  // Allow static files and API routes
  if (
    pathname.startsWith('/_next') ||
    pathname.startsWith('/api') ||
    pathname.includes('.')
  ) {
    return NextResponse.next()
  }
  
  /**
   * Protected routes - redirect to login if not authenticated
   * 
   * Note: This is a basic implementation. In production with httpOnly cookies,
   * you would:
   * 1. Read the auth cookie from request.cookies
   * 2. Verify the token/session
   * 3. Extract user role
   * 4. Perform role-based access control here
   * 
   * Example with JWT in httpOnly cookie:
   * 
   * const token = request.cookies.get('auth_token')?.value
   * if (!token) {
   *   return NextResponse.redirect(new URL(`/login?next=${pathname}`, request.url))
   * }
   * 
   * try {
   *   const decoded = verifyJWT(token)
   *   if (pathname.startsWith('/admin') && decoded.role !== 'owner') {
   *     return NextResponse.redirect(new URL('/account', request.url))
   *   }
   * } catch (error) {
   *   return NextResponse.redirect(new URL('/login', request.url))
   * }
   */
  
  // Check for admin routes
  if (adminPaths.some(path => pathname.startsWith(path))) {
    // In client-side auth mode, let the client handle the redirect
    // If you have server-side session, verify role here
    return NextResponse.next()
  }
  
  // Check for account routes
  if (accountPaths.some(path => pathname.startsWith(path))) {
    // In client-side auth mode, let the client handle the redirect
    // If you have server-side session, verify authentication here
    return NextResponse.next()
  }
  
  return NextResponse.next()
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files (images, etc)
     */
    '/((?!api|_next/static|_next/image|favicon.ico|.*\\..*$).*)',
  ],
}

