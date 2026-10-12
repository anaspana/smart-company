import { Injectable } from '@angular/core';
import { AxiosService } from '../auth/axios.service';
import { WarehouseSearchParams } from './warehouse.model';

@Injectable({ providedIn: 'root' })
export class WarehouseService {

  constructor(private axios: AxiosService) {}

  search(params: WarehouseSearchParams) {
    const p = new URLSearchParams({
      page: params.page.toString(),
      size: params.size.toString(),
      sortBy: params.sortBy,
      sortDir: params.sortDir,
    });
    if (params.search)    p.set('search', params.search);
    if (params.countryId) p.set('countryId', params.countryId.toString());
    if (params.cityId)    p.set('cityId', params.cityId.toString());
    if (params.isOnline != null) p.set('isOnline', params.isOnline.toString());
    return this.axios.request('GET', `/warehouses?${p}`, null);
  }

  getById(id: number) {
    return this.axios.request('GET', `/warehouses/${id}`, null);
  }

  create(payload: any) {
    return this.axios.request('POST', '/warehouses', payload);
  }

  update(id: number, payload: any) {
    return this.axios.request('PUT', `/warehouses/${id}`, payload);
  }

  delete(id: number) {
    return this.axios.request('DELETE', `/warehouses/${id}`, null);
  }

  getCountries() {
    return this.axios.request('GET', '/warehouses/countries', null);
  }

  getCities(countryId: number) {
    return this.axios.request('GET', `/warehouses/cities?countryId=${countryId}`, null);
  }

  getTemperatureData(warehouseId: number, sectorId: number, period: string, from?: string, to?: string) {
    const p = new URLSearchParams({ period });
    if (from) p.set('from', from);
    if (to)   p.set('to', to);
    return this.axios.request('GET', `/warehouses/${warehouseId}/sectors/${sectorId}/temperature?${p}`, null);
  }
}