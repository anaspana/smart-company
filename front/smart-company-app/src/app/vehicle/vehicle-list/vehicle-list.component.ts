import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { VehicleService } from '../vehicle.service';
import { FileService } from '../../file.service';
import { Vehicle, VehicleBrand, VehicleSearchParams } from '../vehicle.model';

@Component({
  selector: 'app-vehicle-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './vehicle-list.component.html',
  styleUrl: './vehicle-list.component.css'
})
export class VehicleListComponent implements OnInit, OnDestroy {

  vehicles: Vehicle[] = [];
  vehicleImages: Map<number, string> = new Map();
  brands: VehicleBrand[] = [];
  models: any[] = [];

  searchText = '';
  selectedBrandId: number | null = null;
  selectedModelId: number | null = null;
  isOnline: boolean | null = null;
  sortBy = 'licensePlate';
  sortDir = 'asc';

  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  totalElements = 0;

  loading = false;
  deleteError = '';

  private searchSubject = new Subject<string>();

  constructor(
    private vehicleService: VehicleService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    this.loadBrands();
    this.loadVehicles();
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadVehicles();
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  loadBrands(): void {
    this.vehicleService.getBrands().then((res: any) => {
      this.brands = res.data;
    });
  }

  onBrandChange(): void {
    this.selectedModelId = null;
    this.models = [];
    if (this.selectedBrandId) {
      this.vehicleService.getModels(this.selectedBrandId).then((res: any) => {
        this.models = res.data;
      });
    }
    this.currentPage = 0;
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.loading = true;
    const params: VehicleSearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir,
    };
    if (this.searchText)       params.search = this.searchText;
    if (this.selectedBrandId)  params.brandId = this.selectedBrandId;
    if (this.selectedModelId)  params.modelId = this.selectedModelId;
    if (this.isOnline != null) params.isOnline = this.isOnline;

    this.vehicleService.search(params).then((res: any) => {
      this.vehicles = res.data.content;
      this.totalPages = res.data.totalPages;
      this.totalElements = res.data.totalElements;
      this.loadFirstImages();
    }).finally(() => this.loading = false);
  }

  loadFirstImages(): void {
    this.vehicles.forEach(v => {
      const key = v.imageKeys?.[0];
      if (key && !this.vehicleImages.has(v.id)) {
        this.fileService.getDownloadUrl(key).subscribe({
          next: (url) => this.vehicleImages.set(v.id, url),
          error: () => {}
        });
      }
    });
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchText);
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadVehicles();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadVehicles();
  }

  getPages(): (number | '...')[] {
    const total = this.totalPages;
    const cur = this.currentPage;
    const last = total - 1;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const result: (number | '...')[] = [0, 1];
    const mid = Math.max(2, Math.min(last - 2, cur));
    const lo = Math.max(2, mid - 1);
    const hi = Math.min(last - 2, mid + 1);
    if (lo > 2) result.push('...');
    for (let i = lo; i <= hi; i++) result.push(i);
    if (hi < last - 2) result.push('...');
    result.push(last - 1, last);
    return result;
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedBrandId = null;
    this.selectedModelId = null;
    this.models = [];
    this.isOnline = null;
    this.currentPage = 0;
    this.loadVehicles();
  }

  confirmDeleteId: number | null = null;

  deleteVehicle(id: number): void {
    this.confirmDeleteId = id;
  }

  confirmDelete(): void {
    if (this.confirmDeleteId === null) return;
    this.vehicleService.delete(this.confirmDeleteId).then(() => {
      this.confirmDeleteId = null;
      this.loadVehicles();
    }).catch(() => {
      this.confirmDeleteId = null;
      this.deleteError = 'Failed to delete vehicle.';
    });
  }

  cancelDelete(): void {
    this.confirmDeleteId = null;
  }
}