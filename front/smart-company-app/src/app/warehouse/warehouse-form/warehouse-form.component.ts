import { Component, OnInit, AfterViewInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { WarehouseService } from '../warehouse.service';
import { FileService } from '../../file.service';
import { Warehouse } from '../warehouse.model';

@Component({
  selector: 'app-warehouse-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './warehouse-form.component.html',
  encapsulation: ViewEncapsulation.None
})
export class WarehouseFormComponent implements OnInit, AfterViewInit, OnDestroy {

  isEditMode = false;
  warehouseId: number | null = null;
  loading = false;
  error = '';
  uploadingImages = false;

  name = '';
  address = '';
  selectedCountryId: number | null = null;
  selectedCityId: number | null = null;
  latitude: number | null = null;
  longitude: number | null = null;
  imageKeys: string[] = [];
  imageUrls: string[] = [];
  sectors: { name: string }[] = [];

  countries: any[] = [];
  cities: any[] = [];

  private map: any;
  private marker: any = null;
  private mapReady = false;
  private L: any = null;

  constructor(
    private warehouseService: WarehouseService,
    private fileService: FileService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.warehouseService.getCountries().then((res: any) => {
      this.countries = res.data;
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.warehouseId = +id;
        this.loadWarehouse(this.warehouseId);
      }
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

    this.map = L.map('warehouse-map').setView([48.0, 15.0], 4);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      this.latitude = +e.latlng.lat.toFixed(6);
      this.longitude = +e.latlng.lng.toFixed(6);
      this.setMarker(e.latlng.lat, e.latlng.lng);
    });

    this.mapReady = true;
    if (this.latitude && this.longitude) {
      this.focusMap(this.latitude, this.longitude);
    }
  }

  focusMap(lat: number, lng: number): void {
    if (!this.mapReady || !this.L) return;
    this.map.setView([lat, lng], 10);
    this.setMarker(lat, lng);
  }

  setMarker(lat: number, lng: number): void {
    if (this.marker) this.map.removeLayer(this.marker);
    this.marker = this.L.marker([lat, lng]).addTo(this.map);
  }

  loadWarehouse(id: number): void {
    this.loading = true;
    this.warehouseService.getById(id).then((res: any) => {
      const w: Warehouse = res.data;
      this.name = w.name;
      this.address = w.address;
      this.selectedCountryId = w.country.id;
      this.latitude = w.latitude;
      this.longitude = w.longitude;
      this.imageKeys = [...w.imageKeys];
      this.sectors = w.sectors.map(s => ({ name: s.name }));
      this.focusMap(w.latitude, w.longitude);

      this.warehouseService.getCities(w.country.id).then((cityRes: any) => {
        this.cities = cityRes.data;
        this.selectedCityId = w.city.id;
      });

      this.imageKeys.forEach(key => {
        this.fileService.getDownloadUrl(key).subscribe({
          next: (url) => this.imageUrls.push(url),
          error: () => {}
        });
      });
    }).finally(() => this.loading = false);
  }

  onCountryChange(): void {
    this.selectedCityId = null;
    this.cities = [];
    if (this.selectedCountryId) {
      this.warehouseService.getCities(this.selectedCountryId).then((res: any) => {
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

  addSector(): void {
    this.sectors.push({ name: '' });
  }

  removeSector(index: number): void {
    this.sectors.splice(index, 1);
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.uploadingImages = true;
    const files = Array.from(input.files);
    let done = 0;

    files.forEach(file => {
      this.fileService.getUploadUrl(file.name, 'warehouse-images').subscribe({
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

  isValid(): boolean {
    return !!this.name && !!this.selectedCountryId && !!this.selectedCityId
      && this.latitude != null && this.longitude != null;
  }

  submit(): void {
    if (!this.isValid()) {
      this.error = 'Please fill in all required fields and select a location on the map.';
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
      sectors: this.sectors.filter(s => s.name.trim())
    };

    const req = this.isEditMode
      ? this.warehouseService.update(this.warehouseId!, payload)
      : this.warehouseService.create(payload);

    req.then(() => {
      this.router.navigate(['/warehouses']);
    }).catch((err: any) => {
      this.error = err?.response?.data?.message || 'An error occurred.';
    }).finally(() => this.loading = false);
  }
}