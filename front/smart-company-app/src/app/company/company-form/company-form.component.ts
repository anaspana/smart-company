import { Component, OnInit, AfterViewInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CompanyService } from '../company.service';
import { FileService } from '../../file.service';

@Component({
  selector: 'app-company-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './company-form.component.html',
  encapsulation: ViewEncapsulation.None // potrebno za Leaflet stilove
})
export class CompanyFormComponent implements OnInit, AfterViewInit, OnDestroy {

  loading = false;
  error = '';
  uploadingImages = false;
  uploadingDocuments = false;

  // Forma
  name = '';
  address = '';
  selectedCountryId: number | null = null;
  selectedCityId: number | null = null;
  latitude: number | null = null;
  longitude: number | null = null;

  imageKeys: string[] = [];
  imageUrls: string[] = [];

  documents: { fileKey: string; fileType: string }[] = [];
  documentUrls: string[] = [];

  countries: any[] = [];
  cities: any[] = [];

  private map: any;
  private marker: any = null;
  private mapReady = false;
  private L: any = null;

  constructor(
    private companyService: CompanyService,
    private fileService: FileService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.companyService.getCountries().then((res: any) => {
      this.countries = res.data;
    });
  }

  async ngAfterViewInit(): Promise<void> {
    await this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }

  async initMap(): Promise<void> {
    
    const leafletModule: any = await import('leaflet');
    const L = leafletModule.default ?? leafletModule;
    this.L = L;

    const iconDefault = L.icon({
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      iconSize: [25, 41], iconAnchor: [12, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;

    this.map = L.map('company-map').setView([48.0, 15.0], 4);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      this.latitude = +e.latlng.lat.toFixed(6);
      this.longitude = +e.latlng.lng.toFixed(6);
      this.setMarker(e.latlng.lat, e.latlng.lng);
    });

    this.mapReady = true;
  }

  setMarker(lat: number, lng: number): void {
    if (this.marker) this.map.removeLayer(this.marker);
    this.marker = this.L.marker([lat, lng]).addTo(this.map);
  }

  onCountryChange(): void {
    this.selectedCityId = null;
    this.cities = [];
    if (this.selectedCountryId) {
      this.companyService.getCities(this.selectedCountryId).then((res: any) => {
        this.cities = res.data;
      });
    }
  }

  onCityChange(): void {
    if (!this.selectedCityId) return;
    const city = this.cities.find(c => c.id === this.selectedCityId);
    const country = this.countries.find(c => c.id === this.selectedCountryId);
    if (!city || !country) return;
    const query = encodeURIComponent(`${city.name}, ${country.name}`);
    fetch(`https://nominatim.openstreetmap.org/search?q=${query}&format=json&limit=1`)
      .then(res => res.json())
      .then((results: any[]) => {
        if (results.length > 0) {
          this.map.setView([parseFloat(results[0].lat), parseFloat(results[0].lon)], 11);
        }
      });
  }

  // ─── Slike firme ────────────────────────────────────────────────

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    this.uploadingImages = true;
    const files = Array.from(input.files);
    let done = 0;

    files.forEach(file => {
      this.fileService.getUploadUrl(file.name, 'company-images').subscribe({
        next: (uploadResponse) => {
          const { url: uploadUrl, filename: s3Key } = uploadResponse;
          this.fileService.uploadToS3(uploadUrl, file).subscribe({
            next: () => {
              this.imageKeys.push(s3Key);
              this.fileService.getDownloadUrl(s3Key).subscribe({
                next: (url: string) => this.imageUrls.push(url),
                error: () => {}
              });
            },
            error: () => { this.error = 'Image upload failed.'; },
            complete: () => {
              done++;
              if (done === files.length) this.uploadingImages = false;
            }
          });
        },
        error: () => {
          this.error = 'Could not get upload URL.';
          done++;
          if (done === files.length) this.uploadingImages = false;
        }
      });
    });
  }

  removeImage(index: number): void {
    this.imageKeys.splice(index, 1);
    this.imageUrls.splice(index, 1);
  }

  // ─── Dokumenti (slike i/ili PDF) ────────────────────────────────

  onDocumentsSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    this.uploadingDocuments = true;
    const files = Array.from(input.files);
    let done = 0;

    files.forEach(file => {
      const fileType = file.type === 'application/pdf' ? 'pdf' : 'image';

      this.fileService.getUploadUrl(file.name, 'company-documents').subscribe({
        next: (uploadResponse) => {
          const { url: uploadUrl, filename: s3Key } = uploadResponse;
          this.fileService.uploadToS3(uploadUrl, file).subscribe({
            next: () => {
              this.documents.push({ fileKey: s3Key, fileType });
              if (fileType === 'image') {
                this.fileService.getDownloadUrl(s3Key).subscribe({
                  next: (url: string) => this.documentUrls.push(url),
                  error: () => {}
                });
              } else {
                this.documentUrls.push(''); // placeholder za PDF
              }
            },
            error: () => { this.error = 'Document upload failed.'; },
            complete: () => {
              done++;
              if (done === files.length) this.uploadingDocuments = false;
            }
          });
        },
        error: () => {
          this.error = 'Could not get upload URL.';
          done++;
          if (done === files.length) this.uploadingDocuments = false;
        }
      });
    });
  }

  removeDocument(index: number): void {
    this.documents.splice(index, 1);
    this.documentUrls.splice(index, 1);
  }

  // ─── Submit ──────────────────────────────────────────────────────

  isValid(): boolean {
    return !!this.name && !!this.selectedCountryId && !!this.selectedCityId
      && this.latitude != null && this.longitude != null
      && this.documents.length > 0;
  }

  submit(): void {
    if (!this.isValid()) {
      this.error = 'Please fill in all required fields, select a location, and attach at least one ownership document.';
      return;
    }

    this.loading = true;
    this.error = '';

    const payload = {
      name: this.name,
      address: this.address,
      countryId: this.selectedCountryId!,
      cityId: this.selectedCityId!,
      latitude: this.latitude!,
      longitude: this.longitude!,
      imageKeys: this.imageKeys,
      documents: this.documents
    };

    this.companyService.create(payload).then(() => {
      this.router.navigate(['/products']);
    }).catch((err: any) => {
      this.error = err?.response?.data?.message || 'An error occurred.';
    }).finally(() => this.loading = false);
  }
}