/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
    // Configure remote image patterns for the shop backend
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '8080',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: '**.herokuapp.com',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: '**.yourdomain.com',
        pathname: '/**',
      },
    ],
  },
  // Use 'export' for GitHub Pages only
  // For Netlify/production, we need server-side features
  output: process.env.NEXT_PUBLIC_BASE_PATH ? 'export' : undefined,
  basePath: process.env.NEXT_PUBLIC_BASE_PATH || '',
  assetPrefix: process.env.NEXT_PUBLIC_BASE_PATH || '',
}

export default nextConfig
