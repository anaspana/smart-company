import { Injectable } from '@angular/core';
import { AxiosService } from '../auth/axios.service';
import { VehicleSearchParams } from './vehicle.model';

@Injectable({ providedIn: 'root' })
export class VehicleService {

  constructor(private axios: AxiosService) {}

  search(params: VehicleSearchParams) {
    const p = new URLSearchParams({
      page: params.page.toString(),
      size: params.size.toString(),
      sortBy: params.sortBy,
      sortDir: params.sortDir,
    });
    if (params.search)  p.set('search', params.search);
    if (params.brandId) p.set('brandId', params.brandId.toString());
    if (params.modelId) p.set('modelId', params.modelId.toString());
    if (params.isOnline != null) p.set('isOnline', params.isOnline.toString());
    return this.axios.request('GET', `/vehicles?${p}`, null);
  }

  getById(id: number) {
    return this.axios.request('GET', `/vehicles/${id}`, null);
  }

  create(payload: any) {
    return this.axios.request('POST', '/vehicles', payload);
  }

  update(id: number, payload: any) {
    return this.axios.request('PUT', `/vehicles/${id}`, payload);
  }

  delete(id: number) {
    return this.axios.request('DELETE', `/vehicles/${id}`, null);
  }

  getBrands() {
    return this.axios.request('GET', '/vehicles/brands', null);
  }

  getModels(brandId: number) {
    return this.axios.request('GET', `/vehicles/models?brandId=${brandId}`, null);
  }

  getDistanceData(vehicleId: number, period: string, from?: string, to?: string) {
    const p = new URLSearchParams({ period });
    if (from) p.set('from', from);
    if (to)   p.set('to', to);
    return this.axios.request('GET', `/vehicles/${vehicleId}/distance?${p}`, null);
  }
}