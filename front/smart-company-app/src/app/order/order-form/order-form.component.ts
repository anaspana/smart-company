import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AxiosService } from '../../auth/axios.service';
import { FileService } from '../../file.service';
import { Router } from '@angular/router';

interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  weight: number;
  image: string;
  isForSale: boolean;
  totalStock: number;
  category: { id: number; name: string };
}

interface Company {
  id: number;
  name: string;
  city: string;
  country: string;
}

interface CartItem {
  product: Product;
  quantity: number;
}

interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
}

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-form.component.html',
  styleUrl: './order-form.component.css'
})
export class OrderFormComponent implements OnInit, OnDestroy {

  products: Product[] = [];
  companies: Company[] = [];
  cart: CartItem[] = [];
  productImages: { [productId: number]: string } = {};
  categories: { id: number; name: string }[] = [];

  selectedCompanyId: number | null = null;
  selectedCategoryId: number | null = null;
  searchQuery = '';
  sortBy = 'name';
  sortDir = 'asc';
  minPrice: number | null = null;
  maxPrice: number | null = null;

  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  isLoadingProducts = false;
  isLoadingCompanies = false;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private axiosService: AxiosService,
    private fileService: FileService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCompanies();
    this.loadCategories();
    this.loadProducts();

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadProducts();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(): void {
    this.searchSubject.next(this.searchQuery);
  }

  onCategoryChange(): void {
    this.currentPage = 0;
    this.loadProducts();
  }

  onForSaleChange(): void {
    this.currentPage = 0;
    this.loadProducts();
  }

  onSortChange(field: string): void {
    if (this.sortBy === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDir = 'asc';
    }
    this.currentPage = 0;
    this.loadProducts();
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.selectedCategoryId = null;
    this.minPrice = null;
    this.maxPrice = null;
    this.sortBy = 'name';
    this.sortDir = 'asc';
    this.currentPage = 0;
    this.loadProducts();
  }

  loadCategories(): void {
    this.axiosService.request('GET', '/orders/categories', null)
      .then((res: any) => this.categories = res.data)
      .catch(() => {});
  }

  loadCompanies(): void {
    this.isLoadingCompanies = true;
    this.axiosService.request('GET', '/orders/my-companies', null)
      .then((res: any) => this.companies = res.data)
      .catch(() => this.errorMessage = 'Greška pri učitavanju firmi.')
      .finally(() => this.isLoadingCompanies = false);
  }

  loadProducts(): void {
    this.isLoadingProducts = true;
    const params = new URLSearchParams({
      page: this.currentPage.toString(),
      size: '10',
      sortBy: this.sortBy,
      sortDir: this.sortDir,
    });
    if (this.searchQuery) params.set('name', this.searchQuery);
    if (this.selectedCategoryId) params.set('categoryId', this.selectedCategoryId.toString());
    if (this.minPrice != null) params.set('minPrice', this.minPrice.toString());
    if (this.maxPrice != null) params.set('maxPrice', this.maxPrice.toString());

    this.axiosService.request('GET', `/orders/products?${params}`, null)
      .then((res: any) => {
        const page: Page<Product> = res.data;
        this.products = page.content;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.loadProductImages();
      })
      .catch(() => this.errorMessage = 'Greška pri učitavanju proizvoda.')
      .finally(() => this.isLoadingProducts = false);
  }

  loadProductImages(): void {
    this.products.forEach(product => {
      if (product.image && !this.productImages[product.id]) {
        this.fileService.getDownloadUrl(product.image).subscribe(url => {
          this.productImages[product.id] = url;
        });
      }
    });
  }

  addToCart(product: Product): void {
    if (product.totalStock === 0) return;
    const existing = this.cart.find(i => i.product.id === product.id);
    if (existing) {
      if (existing.quantity < product.totalStock) {
        existing.quantity++;
      }
    } else {
      this.cart.push({ product, quantity: 1 });
    }
  }

  removeFromCart(productId: number): void {
    this.cart = this.cart.filter(i => i.product.id !== productId);
  }

  updateQuantity(item: CartItem, quantity: number): void {
    if (quantity < 1) {
      this.removeFromCart(item.product.id);
      return;
    }
    item.quantity = Math.min(quantity, item.product.totalStock);
  }

  getCartQuantity(productId: number): number {
    return this.cart.find(i => i.product.id === productId)?.quantity ?? 0;
  }

  getCartItem(productId: number): CartItem | undefined {
    return this.cart.find(i => i.product.id === productId);
  }

  get totalPrice(): number {
    return this.cart.reduce((sum, i) => sum + i.product.price * i.quantity, 0);
  }

  submitOrder(): void {
    this.errorMessage = '';
    if (!this.selectedCompanyId) {
      this.errorMessage = 'Izaberite firmu za dostavu.';
      return;
    }
    if (this.cart.length === 0) {
      this.errorMessage = 'Korpa je prazna.';
      return;
    }

    this.isSubmitting = true;

    const payload = {
      companyId: this.selectedCompanyId,
      items: this.cart.map(i => ({ productId: i.product.id, quantity: i.quantity })),
    };

    this.axiosService.request('POST', '/orders', payload)
      .then(() => {
        this.successMessage = 'Porudžbina je uspešno kreirana! Faktura je poslata na vaš email.';
        this.cart = [];
        this.selectedCompanyId = null;
      })
      .catch((err: any) => {
        this.errorMessage = err?.response?.data?.message ?? 'Greška pri kreiranju porudžbine.';
      })
      .finally(() => this.isSubmitting = false);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadProducts();
  }

  getPageNumbers(): number[] {
    const pages = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }
}