export interface CompanyDocument {
  fileKey: string;
  fileType: string; // "image" ili "pdf"
}

export interface Company {
  id: number;
  name: string;
  address: string;
  country: { id: number; name: string; code: string };
  city: { id: number; name: string; countryId: number };
  latitude: number;
  longitude: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason: string | null;
  ownerUsername: string;
  imageKeys: string[];
  documents: CompanyDocument[];
}

export interface CompanyRequestSummary {
  id: number;
  name: string;
  address: string;
  country: { id: number; name: string; code: string };
  city: { id: number; name: string; countryId: number };
  latitude: number;
  longitude: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason: string | null;
  ownerUsername: string;
  imageKeys: string[];
  documents: CompanyDocument[];
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
}