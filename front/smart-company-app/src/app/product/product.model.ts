export interface Category {
  id: number;
  name: string;
  description: string;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  weight: number;
  image: string | null;
  forSale: boolean;
  category: Category;
  factories: FactorySummary[];
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  weight: number;
  image: string | null;
  isForSale: boolean;
  categoryId: number;
}

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ProductSearchParams {
  search?: string;
  categoryId?: number;
  isForSale?: boolean;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface FactorySummary {
  id: number;
  name: string;
}