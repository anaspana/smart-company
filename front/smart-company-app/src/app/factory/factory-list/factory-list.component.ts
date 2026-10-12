import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { FactoryService } from '../factory.service';
import { FileService } from '../../file.service';
import { Factory, Country, City, FactorySearchParams } from '../factory.model';

@Component({
  selector: 'app-factory-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './factory-list.component.html',
  styleUrl: './factory-list.component.css'
})
export class FactoryListComponent implements OnInit, OnDestroy {

  factories: Factory[] = [];
  factoryImages: Map<number, string> = new Map(); // factoryId -> prva slika URL
  countries: Country[] = [];
  cities: City[] = [];

  // Filteri
  searchText = '';
  selectedCountryId: number | null = null;
  selectedCityId: number | null = null;
  isOnline: boolean | null = null;
  sortBy = 'name';
  sortDir = 'asc';

  // Paginacija
  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  totalElements = 0;

  loading = false;
  deleteError = '';

  private searchSubject = new Subject<string>();

  constructor(
    private factoryService: FactoryService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    this.loadCountries();
    this.loadFactories();

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadFactories();
    });
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  loadCountries(): void {
    this.factoryService.getCountries().then((res: any) => {
      this.countries = res.data;
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
    this.currentPage = 0;
    this.loadFactories();
  }

  loadFactories(): void {
    this.loading = true;
    const params: FactorySearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sortBy: this.sortBy,
      sortDir: this.sortDir
    };
    if (this.searchText)            params.search = this.searchText;
    if (this.selectedCountryId)     params.countryId = this.selectedCountryId;
    if (this.selectedCityId)        params.cityId = this.selectedCityId;
    if (this.isOnline != null)      params.isOnline = this.isOnline;

    this.factoryService.search(params).then((res: any) => {
      this.factories = res.data.content;
      this.totalPages = res.data.totalPages;
      this.totalElements = res.data.totalElements;
      this.loadFirstImages();
    }).finally(() => {
      this.loading = false;
    });
  }

  loadFirstImages(): void {
    this.factories.forEach(factory => {
      const key = factory.imageKeys?.[0];
      if (key && !this.factoryImages.has(factory.id)) {
        this.fileService.getDownloadUrl(key).subscribe({
          next: (url) => this.factoryImages.set(factory.id, url),
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
    this.loadFactories();
  }

  onSortChange(field: string): void {
    if (this.sortBy === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDir = 'asc';
    }
    this.currentPage = 0;
    this.loadFactories();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadFactories();
  }

  getPages(): (number | '...')[] {
    const total = this.totalPages;
    const cur = this.currentPage;
    const last = total - 1;
    
    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }

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
    this.sortBy = 'name';
    this.sortDir = 'asc';
    this.currentPage = 0;
    this.loadFactories();
  }

  confirmDeleteId: number | null = null;

  deleteFactory(id: number): void {
    this.confirmDeleteId = id;
  }

  confirmDelete(): void {
    if (this.confirmDeleteId === null) return;
    this.factoryService.delete(this.confirmDeleteId).then(() => {
      this.confirmDeleteId = null;
      this.loadFactories();
    }).catch(() => {
      this.confirmDeleteId = null;
      this.deleteError = 'Failed to delete factory.';
    });
  }

  cancelDelete(): void {
    this.confirmDeleteId = null;
  }
}