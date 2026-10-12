export interface Warehouse {
  id: number;
  name: string;
  address: string;
  country: { id: number; name: string; code: string };
  city: { id: number; name: string; countryId: number };
  latitude: number;
  longitude: number;
  isOnline: boolean;
  lastHeartbeat: string | null;
  imageKeys: string[];
  sectors: Sector[];
}

export interface Sector {
  id: number;
  name: string;
  lastTemperature: number | null;
  lastMeasuredAt: string | null;
}

export interface WarehouseSearchParams {
  page: number;
  size: number;
  sortBy: string;
  sortDir: string;
  search?: string;
  countryId?: number;
  cityId?: number;
  isOnline?: boolean;
}

export interface TemperatureDataDto {
  granularity: string;
  labels: string[];
  values: number[];
}