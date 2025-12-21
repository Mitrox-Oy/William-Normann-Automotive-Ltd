# GitHub Pages Deployment Guide

This project is configured to deploy to GitHub Pages using the `/docs` folder method.

## Quick Start

1. **Set up environment variables:**
   - Create a `.env.local` file in the root directory
   - For project pages: Add `NEXT_PUBLIC_BASE_PATH=/your-repo-name` and `NEXT_PUBLIC_SITE_URL=https://username.github.io/your-repo-name`
   - For user/org pages: Leave `NEXT_PUBLIC_BASE_PATH` empty and set `NEXT_PUBLIC_SITE_URL=https://username.github.io`

2. **Build and deploy:**
   ```bash
   npm run build:gh-pages
   git add docs
   git commit -m "Deploy to GitHub Pages"
   git push
   ```

3. **Configure GitHub Pages:**
   - Go to repository Settings → Pages
   - Select "Deploy from a branch" → Choose your branch → Select `/docs` folder
   - Or use GitHub Actions (see Automatic Deployment below)

## Prerequisites

1. A GitHub repository
2. GitHub Pages enabled in your repository settings
3. Node.js and npm installed locally

## Configuration

### For User/Organization Pages (username.github.io)

If your repository name matches your GitHub username (e.g., `username/username.github.io`):

1. Create a `.env.local` file in the root directory:
```bash
NEXT_PUBLIC_BASE_PATH=
NEXT_PUBLIC_SITE_URL=https://username.github.io
```

2. The site will be served from the root URL: `https://username.github.io`

### For Project Pages (username.github.io/repo-name)

If your repository has a different name (e.g., `username/automotive-website-scaffold`):

1. Create a `.env.local` file in the root directory:
```bash
NEXT_PUBLIC_BASE_PATH=/automotive-website-scaffold
NEXT_PUBLIC_SITE_URL=https://username.github.io/automotive-website-scaffold
```

2. Replace `automotive-website-scaffold` with your actual repository name.

3. The site will be served from: `https://username.github.io/automotive-website-scaffold`

## Manual Deployment

1. **Build the project:**
   ```bash
   npm run build:gh-pages
   ```

2. **Commit the `/docs` folder:**
   ```bash
   git add docs
   git commit -m "Deploy to GitHub Pages"
   git push
   ```

3. **Configure GitHub Pages:**
   - Go to your repository Settings → Pages
   - Under "Source", select "Deploy from a branch"
   - Select the branch (usually `main` or `master`)
   - Select `/docs` as the folder
   - Click Save

## Automatic Deployment (GitHub Actions)

The project includes a GitHub Actions workflow (`.github/workflows/deploy.yml`) that automatically:

1. Builds the project when you push to `main` or `master`
2. Detects if it's a user/org page or project page
3. Sets the correct `basePath` automatically
4. Deploys to GitHub Pages

### Setting up GitHub Actions

1. **Enable GitHub Pages:**
   - Go to repository Settings → Pages
   - Under "Source", select "GitHub Actions"

2. **Push your code:**
   ```bash
   git add .
   git commit -m "Add GitHub Pages deployment"
   git push
   ```

3. The workflow will run automatically and deploy your site.

## Important Notes

- **Images and Assets:** All images, icons, and assets in the `public/` folder will work correctly with the basePath configuration.
- **Routes:** All internal links and routes are automatically handled by Next.js with the basePath.
- **Environment Variables:** Make sure to set `NEXT_PUBLIC_BASE_PATH` and `NEXT_PUBLIC_SITE_URL` correctly for your setup.
- **The `/docs` folder:** This folder contains the built static files. You can choose to commit it or ignore it (it's currently not ignored in `.gitignore`).

### ⚠️ API Routes Limitation

**Important:** GitHub Pages only serves static files. The API route at `/api/lead` will **not work** in the deployed site. The forms (`LeadForm` and `QuoteForm`) currently use this API route and will fail on GitHub Pages.

**Solutions:**
1. **Use a third-party form service** (recommended):
   - Formspree: `https://formspree.io`
   - Netlify Forms: `https://www.netlify.com/products/forms/`
   - EmailJS: `https://www.emailjs.com`
   - Update the form components to use these services instead

2. **Use a serverless function**:
   - Vercel Functions (if using Vercel)
   - Cloudflare Workers
   - AWS Lambda

3. **Use a backend service**:
   - Supabase
   - Firebase
   - Your own backend API

For now, the site will build and deploy successfully, but form submissions will fail. You'll need to update the form components to use one of the solutions above.

## Troubleshooting

### Assets not loading

- Verify that `NEXT_PUBLIC_BASE_PATH` is set correctly
- Check that the basePath matches your repository name (for project pages)
- Ensure images use Next.js `Image` component or relative paths

### 404 errors on routes

- Make sure `trailingSlash: true` is set in `next.config.mjs` (already configured)
- Verify the basePath is correct
- Check that all routes are properly exported as static pages

### Build fails

- Ensure all pages can be statically generated (no server-side only features)
- Check that `generateStaticParams` is used for dynamic routes
- Verify all API routes are removed or handled differently (GitHub Pages is static only)

## Testing Locally

To test the production build locally:

```bash
npm run build:gh-pages
npx serve docs
```

Then visit `http://localhost:3000` (or the URL shown by serve) to test your site.

