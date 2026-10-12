import { Injectable } from '@angular/core';
import { AxiosService } from '../auth/axios.service';
import { FactoryRequest, FactorySearchParams } from './factory.model';

@Injectable({ providedIn: 'root' })
export class FactoryService {

  private base = '/factories';

  constructor(private axios: AxiosService) {}

  search(params: FactorySearchParams) {
    const query = new URLSearchParams();
    if (params.search)              query.set('search', params.search);
    if (params.countryId != null)   query.set('countryId', String(params.countryId));
    if (params.cityId != null)      query.set('cityId', String(params.cityId));
    if (params.isOnline != null)    query.set('isOnline', String(params.isOnline));
    if (params.page != null)        query.set('page', String(params.page));
    if (params.size != null)        query.set('size', String(params.size));
    if (params.sortBy)              query.set('sortBy', params.sortBy);
    if (params.sortDir)             query.set('sortDir', params.sortDir);
    return this.axios.request('GET', `${this.base}?${query.toString()}`, null);
  }

  getById(id: number) {
    return this.axios.request('GET', `${this.base}/${id}`, null);
  }

  create(factory: FactoryRequest) {
    return this.axios.request('POST', this.base, factory);
  }

  update(id: number, factory: FactoryRequest) {
    return this.axios.request('PUT', `${this.base}/${id}`, factory);
  }

  delete(id: number) {
    return this.axios.request('DELETE', `${this.base}/${id}`, null);
  }

  getCountries() {
    return this.axios.request('GET', `${this.base}/countries`, null);
  }

  getCities(countryId: number) {
    return this.axios.request('GET', `${this.base}/cities?countryId=${countryId}`, null);
  }

  getProducts() {
    return this.axios.request('GET', '/products?page=0&size=100&sortBy=name&sortDir=asc', null);
  }

  getProductionData(
    factoryId: number,
    productId: number,
    period: string,
    from?: string,
    to?: string
  ) {
    const query = new URLSearchParams({ period });
    if (from) query.set('from', from);
    if (to)   query.set('to', to);
    return this.axios.request(
      'GET',
      `${this.base}/${factoryId}/products/${productId}/production?${query.toString()}`,
      null
    );
  }
}