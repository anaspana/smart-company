export interface Country {
  id: number;
  name: string;
  code: string;
}

export interface City {
  id: number;
  name: string;
  countryId: number;
}

export interface Factory {
  id: number;
  name: string;
  address: string;
  country: Country;
  city: City;
  latitude: number;
  longitude: number;
  isOnline: boolean;
  lastHeartbeat: string | null;
  imageKeys: string[];
  productIds: number[];
}

export interface FactoryRequest {
  name: string;
  address: string;
  countryId: number;
  cityId: number;
  latitude: number;
  longitude: number;
  imageKeys: string[];
  productIds: number[];
}

export interface FactorySearchParams {
  search?: string;
  countryId?: number;
  cityId?: number;
  isOnline?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export type ProductionPeriod = 'WEEK' | 'MONTH' | 'THREE_MONTHS' | 'SIX_MONTHS' | 'YEAR' | 'CUSTOM';
 
export interface ProductionDataDto {
  granularity: 'day' | 'week' | 'month';
  labels: string[];
  values: number[];
}