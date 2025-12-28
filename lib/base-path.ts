/**
 * Get the base path for assets (used for GitHub Pages deployment)
 * This should match NEXT_PUBLIC_BASE_PATH from environment variables
 */
export function getBasePath(): string {
  // Use the base path from the environment variable
  // This works in both client and server components
  if (typeof process !== 'undefined' && process.env.NEXT_PUBLIC_BASE_PATH) {
    return process.env.NEXT_PUBLIC_BASE_PATH
  }
  return ''
}

/**
 * Prepend base path to an asset URL
 * Use this for static assets like videos, images in <img> tags, etc.
 * Note: Next.js Image component automatically handles basePath, so this is mainly for:
 * - Video elements
 * - Regular img tags
 * - Metadata icons
 * - Other non-Next.js Image assets
 */
export function withBasePath(path: string): string {
  const basePath = getBasePath()
  if (!basePath) {
    return path
  }
  // Ensure path starts with / and basePath doesn't end with /
  const cleanPath = path.startsWith('/') ? path : `/${path}`
  const cleanBasePath = basePath.endsWith('/') ? basePath.slice(0, -1) : basePath
  return `${cleanBasePath}${cleanPath}`
}

