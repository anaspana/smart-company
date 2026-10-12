import { Component, OnInit, OnDestroy, AfterViewInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { FactoryService } from '../factory.service';
import { FileService } from '../../file.service';
import { Country, City, Factory } from '../factory.model';

@Component({
  selector: 'app-factory-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './factory-form.component.html',
  styleUrl: './factory-form.component.css',
  encapsulation: ViewEncapsulation.None  // potrebno za Leaflet stilove
})
export class FactoryFormComponent implements OnInit, AfterViewInit, OnDestroy {

  isEditMode = false;
  factoryId: number | null = null;
  loading = false;
  error = '';
  uploadingImages = false;

  // Forma
  name = '';
  address = '';
  selectedCountryId: number | null = null;
  selectedCityId: number | null = null;
  latitude: number | null = null;
  longitude: number | null = null;
  imageKeys: string[] = [];
  productIds: number[] = [];

  countries: Country[] = [];
  cities: City[] = [];
  imageUrls: string[] = []; // presigned URLs za prikaz

  products: any[] = [];
  loadingProducts = false;

  private map!: L.Map;
  private marker: L.Marker | null = null;

  constructor(
    private factoryService: FactoryService,
    private fileService: FileService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.factoryService.getCountries().then((res: any) => {
      this.countries = res.data;
    });

    this.loadProducts();

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.factoryId = +id;
        this.loadFactory(this.factoryId);
      }
    });
  }

  loadProducts(): void {
    this.loadingProducts = true;
    this.factoryService.getProducts().then((res: any) => {
      this.products = res.data.content;
    }).finally(() => this.loadingProducts = false);
  }

  isProductSelected(productId: number): boolean {
    return this.productIds.includes(productId);
  }

  toggleProduct(productId: number): void {
    const idx = this.productIds.indexOf(productId);
    if (idx === -1) {
      this.productIds.push(productId);
    } else {
      this.productIds.splice(idx, 1);
    }
  }

  async ngAfterViewInit(): Promise<void> {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }

  private mapReady = false;
  private L: any = null;

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

    this.map = L.map('factory-map').setView([48.0, 15.0], 4);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: any) => {
      this.latitude = +e.latlng.lat.toFixed(6);
      this.longitude = +e.latlng.lng.toFixed(6);
      this.setMarker(L, e.latlng.lat, e.latlng.lng);
    });

    this.mapReady = true;

    // Ako su koordinate već učitane pre mape
    if (this.latitude && this.longitude) {
      this.focusMap(this.latitude, this.longitude);
    }
  }

  focusMap(lat: number, lng: number): void {
    if (!this.mapReady || !this.L) return;
    this.map.setView([lat, lng], 10);
    this.setMarker(this.L, lat, lng);
  }

  setMarker(L: any, lat: number, lng: number): void {
    if (this.marker) this.map.removeLayer(this.marker);
    this.marker = L.marker([lat, lng]).addTo(this.map);
  }

  loadFactory(id: number): void {
    this.loading = true;
    this.factoryService.getById(id).then((res: any) => {
      const f: Factory = res.data;
      this.name = f.name;
      this.address = f.address;
      this.selectedCountryId = f.country.id;
      this.latitude = f.latitude;
      this.longitude = f.longitude;
      this.focusMap(f.latitude, f.longitude);
      this.imageKeys = [...f.imageKeys];
      this.productIds = [...f.productIds];

      this.factoryService.getCities(f.country.id).then((cityRes: any) => {
        this.cities = cityRes.data;
        this.selectedCityId = f.city.id;
      });

      this.imageUrls = [];
      this.imageKeys.forEach(key => {
        this.fileService.getDownloadUrl(key).subscribe({
          next: (url) => this.imageUrls.push(url),
          error: () => {}
        });
      });
    }).finally(() => {
      this.loading = false;
    });
  }

  onCountryChange(): void {
    this.selectedCityId = null;
    this.cities = [];
    if (this.selectedCountryId) {
      this.factoryService.getCities(this.selectedCountryId).then((res: any) => {
        this.cities = res.data;
      });
    }
  }

  onCityChange(): void {
    if (!this.selectedCityId) return;
    const city = this.cities.find(c => c.id === this.selectedCityId);
    const country = this.countries.find(c => c.id === this.selectedCountryId);
    if (!city || !country) return;

    // besplatan OSM API
    const query = encodeURIComponent(`${city.name}, ${country.name}`);
    fetch(`https://nominatim.openstreetmap.org/search?q=${query}&format=json&limit=1`)
      .then(res => res.json())
      .then((results: any[]) => {
        if (results.length > 0) {
          const lat = parseFloat(results[0].lat);
          const lon = parseFloat(results[0].lon);
          this.map.setView([lat, lon], 11);
        }
      });
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    this.uploadingImages = true;
    const files = Array.from(input.files);
    let done = 0;

    files.forEach(file => {
      // 1. Traži presigned PUT URL
      this.fileService.getUploadUrl(file.name, 'factory-images').subscribe({
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
            error: () => { 
              this.error = 'Image upload failed.'; 
            },
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
      productIds: this.productIds
    };

    const req = this.isEditMode
      ? this.factoryService.update(this.factoryId!, payload)
      : this.factoryService.create(payload);

    req.then(() => {
      this.router.navigate(['/factories']);
    }).catch((err: any) => {
      this.error = err?.response?.data?.message || 'An error occurred.';
    }).finally(() => {
      this.loading = false;
    });
  }
}