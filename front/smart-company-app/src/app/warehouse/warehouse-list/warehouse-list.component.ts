import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { WarehouseService } from '../warehouse.service';
import { FileService } from '../../file.service';
import { Warehouse, WarehouseSearchParams } from '../warehouse.model';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './warehouse-list.component.html',
  styleUrl: './warehouse-list.component.css'
})
export class WarehouseListComponent implements OnInit, OnDestroy {

  warehouses: Warehouse[] = [];
  warehouseImages: Map<number, string> = new Map();
  countries: any[] = [];
  cities: any[] = [];

  searchText = '';
  selectedCountryId: number | null = null;
  selectedCityId: number | null = null;
  isOnline: boolean | null = null;
  sortBy = 'name';
  sortDir = 'asc';

  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  totalElements = 0;

  loading = false;
  deleteError = '';

  private searchSubject = new Subject<string>();

  constructor(
    private warehouseService: WarehouseService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    this.loadCountries();
    this.loadWarehouses();
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadWarehouses();
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  loadCountries(): void {
    this.warehouseService.getCountries().then((res: any) => {
      this.countries = res.data;
    });
  }

  onCountryChange(): void {
    this.selectedCityId = null;
    this.cities = [];
    if (this.selectedCountryId) {
      this.warehouseService.getCities(this.selectedCountryId).then((res: any) => {
        this.cities = res.data;
      });
    }
    this.currentPage = 0;
    this.loadWarehouses();
  }

  loadWarehouses(): void {
    this.loading = true;
    const params: WarehouseSearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir,
    };
    if (this.searchText)        params.search = this.searchText;
    if (this.selectedCountryId) params.countryId = this.selectedCountryId;
    if (this.selectedCityId)    params.cityId = this.selectedCityId;
    if (this.isOnline != null)  params.isOnline = this.isOnline;

    this.warehouseService.search(params).then((res: any) => {
      this.warehouses = res.data.content;
      this.totalPages = res.data.totalPages;
      this.totalElements = res.data.totalElements;
      this.loadFirstImages();
    }).finally(() => this.loading = false);
  }

  loadFirstImages(): void {
    this.warehouses.forEach(w => {
      const key = w.imageKeys?.[0];
      if (key && !this.warehouseImages.has(w.id)) {
        this.fileService.getDownloadUrl(key).subscribe({
          next: (url) => this.warehouseImages.set(w.id, url),
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
    this.loadWarehouses();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadWarehouses();
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
    this.selectedCountryId = null;
    this.selectedCityId = null;
    this.cities = [];
    this.isOnline = null;
    this.currentPage = 0;
    this.loadWarehouses();
  }

  confirmDeleteId: number | null = null;

  deleteWarehouse(id: number): void {
    this.confirmDeleteId = id;
  }

  confirmDelete(): void {
    if (this.confirmDeleteId === null) return;
    this.warehouseService.delete(this.confirmDeleteId).then(() => {
      this.confirmDeleteId = null;
      this.loadWarehouses();
    }).catch(() => {
      this.confirmDeleteId = null;
      this.deleteError = 'Failed to delete warehouse.';
    });
  }

  cancelDelete(): void {
    this.confirmDeleteId = null;
  }
}