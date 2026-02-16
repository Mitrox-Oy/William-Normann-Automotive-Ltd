/**
 * Cached data fetchers — deduplicate API calls between generateMetadata()
 * and the page component within a single server request using React cache().
 */

import { cache } from 'react'
import {
    fetchProductBySlug,
    fetchCategoryBySlug,
    fetchProducts,
    fetchCategories,
    type Product,
    type Category,
    type ProductsResponse,
} from '@/lib/shopApi'

/** Fetch a single product by SKU (slug). Cached per request. */
export const getCachedProduct = cache(
    async (slug: string): Promise<Product | null> => fetchProductBySlug(slug),
)

/** Fetch a single category by slug. Cached per request. */
export const getCachedCategory = cache(
    async (slug: string): Promise<Category | null> => fetchCategoryBySlug(slug),
)

/** Fetch paginated products. Cached per request by serialised params. */
export const getCachedProducts = cache(
    async (page: number, limit: number): Promise<ProductsResponse> =>
        fetchProducts({ page, limit }),
)

/** Fetch all categories. Cached per request. */
export const getCachedCategories = cache(
    async (): Promise<Category[]> => fetchCategories(),
)
