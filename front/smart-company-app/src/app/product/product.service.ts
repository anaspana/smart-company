import { Injectable } from '@angular/core';
import { AxiosService } from '../auth/axios.service';
import { ProductRequest, ProductSearchParams } from './product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private base = '/products';
  private categoriesBase = '/categories';

  constructor(private axios: AxiosService) {}

  search(params: ProductSearchParams) {
    const query = new URLSearchParams();
    if (params.search)              query.set('search', params.search);
    if (params.categoryId != null)  query.set('categoryId', String(params.categoryId));
    if (params.isForSale != null)   query.set('isForSale', String(params.isForSale));
    if (params.minPrice != null)    query.set('minPrice', String(params.minPrice));
    if (params.maxPrice != null)    query.set('maxPrice', String(params.maxPrice));
    if (params.page != null)        query.set('page', String(params.page));
    if (params.size != null)        query.set('size', String(params.size));
    if (params.sortBy)              query.set('sortBy', params.sortBy);
    if (params.sortDir)             query.set('sortDir', params.sortDir);

    return this.axios.request('GET', `${this.base}?${query.toString()}`, null);
  }

  getById(id: number) {
    return this.axios.request('GET', `${this.base}/${id}`, null);
  }

  create(product: ProductRequest) {
    return this.axios.request('POST', this.base, product);
  }

  update(id: number, product: ProductRequest) {
    return this.axios.request('PUT', `${this.base}/${id}`, product);
  }

  delete(id: number) {
    return this.axios.request('DELETE', `${this.base}/${id}`, null);
  }

  getCategories() {
    return this.axios.request('GET', this.categoriesBase, null);
  }
  
  createCategory(payload: { name: string; description: string }) {
    return this.axios.request('POST', this.categoriesBase, payload);
  }

  getFactories() {
    return this.axios.request('GET', `/factories?page=0&size=1000&sortBy=name&sortDir=asc`, null);
  }
}