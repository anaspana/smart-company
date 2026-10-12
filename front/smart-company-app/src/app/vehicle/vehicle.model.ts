export interface VehicleBrand {
  id: number;
  name: string;
}

export interface VehicleModel {
  id: number;
  name: string;
  brandId: number;
}

export interface Vehicle {
  id: number;
  licensePlate: string;
  weightLimit: number;
  brand: VehicleBrand;
  model: VehicleModel;
  latitude: number | null;
  longitude: number | null;
  isOnline: boolean;
  lastHeartbeat: string | null;
  imageKeys: string[];
}

export interface VehicleSearchParams {
  page: number;
  size: number;
  sortBy: string;
  sortDir: string;
  search?: string;
  brandId?: number;
  modelId?: number;
  isOnline?: boolean;
}

export interface DistanceDataDto {
  granularity: string;
  labels: string[];
  values: number[];
}