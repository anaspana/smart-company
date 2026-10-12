import { Injectable } from '@angular/core';
import { AxiosService } from '../auth/axios.service';

@Injectable({ providedIn: 'root' })
export class CompanyService {

  constructor(private axios: AxiosService) {}

  create(payload: any) {
    return this.axios.request('POST', '/companies', payload);
  }

  getCountries() {
    return this.axios.request('GET', '/companies/countries', null);
  }

  getCities(countryId: number) {
    return this.axios.request('GET', `/companies/cities?countryId=${countryId}`, null);
  }

  search(search: string, status: string | null, page: number, size: number) {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy: 'id',
      sortDir: 'desc',
    });
    if (search) params.set('search', search);
    if (status)  params.set('status', status);
    return this.axios.request('GET', `/companies?${params}`, null);
  }

  getByIdForManager(id: number) {
    return this.axios.request('GET', `/companies/${id}/manage`, null);
  }

  decide(id: number, approve: boolean, rejectionReason?: string) {
    return this.axios.request('PUT', `/companies/${id}/decision`, { approve, rejectionReason });
  }
}