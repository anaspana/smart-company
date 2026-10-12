import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  Chart,
  CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  Title, Tooltip, Legend,
  BarController
} from 'chart.js';

import { FactoryService } from '../factory.service';
import { ProductService } from '../../product/product.service';
import { FileService } from '../../file.service';
import { Factory, ProductionDataDto, ProductionPeriod } from '../factory.model';
import { Product } from '../../product/product.model';

Chart.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, BarController, Title, Tooltip, Legend);

@Component({
  selector: 'app-factory-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './factory-detail.component.html',
  styleUrl: './factory-detail.component.css'
})
export class FactoryDetailComponent implements OnInit, OnDestroy {

  factory: Factory | null = null;
  imageUrls: string[] = [];
  products: Product[] = [];
  loadingFactory = true;
  loadingProducts = false;
  error = '';

  currentImageIndex = 0;
  private galleryInterval: any;

  selectedProduct: Product | null = null;

  selectedPeriod: ProductionPeriod = 'WEEK';
  customFrom = '';
  customTo   = '';
  periods: { value: ProductionPeriod; label: string }[] = [
    { value: 'WEEK',         label: 'Last 7 days'   },
    { value: 'MONTH',        label: 'Last month'    },
    { value: 'THREE_MONTHS', label: 'Last 3 months' },
    { value: 'SIX_MONTHS',  label: 'Last 6 months' },
    { value: 'YEAR',         label: 'Last year'     },
    { value: 'CUSTOM',       label: 'Custom'        },
  ];

  private chartInstance: Chart | null = null;
  loadingChart = false;
  chartError = '';
  chartHasData = false;
  @ViewChild('chartCanvas') chartCanvasRef!: ElementRef<HTMLCanvasElement>;
  productImageUrls: Map<number, string> = new Map();

  constructor(
    private route: ActivatedRoute,
    private factoryService: FactoryService,
    private productService: ProductService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadFactory(id);
  }

  ngOnDestroy(): void {
    if (this.galleryInterval) clearInterval(this.galleryInterval);
    this.destroyChart();
  }

  loadFactory(id: number): void {
    this.loadingFactory = true;
    this.factoryService.getById(id).then((res: any) => {
      this.factory = res.data as Factory;
      this.loadImages();
      this.loadProducts();
      setTimeout(() => this.initMap(), 100); // nakon što Angular renderuje div
    }).catch(() => {
      this.error = 'Failed to load factory.';
    }).finally(() => {
      this.loadingFactory = false;
    });
  }

  private async initMap(): Promise<void> {
    if (!this.factory) return;
   
    const leafletModule: any = await import('leaflet');
    const L = leafletModule.default ?? leafletModule;

    const iconDefault = L.icon({
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      iconSize: [25, 41], iconAnchor: [12, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;

    const map = L.map('detail-map', { zoomControl: true, dragging: false, scrollWheelZoom: false })
      .setView([this.factory.latitude, this.factory.longitude], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(map);

    L.marker([this.factory.latitude, this.factory.longitude]).addTo(map);
  }

  loadImages(): void {
    if (!this.factory?.imageKeys?.length) return;
    this.factory.imageKeys.forEach(key => {
      this.fileService.getDownloadUrl(key).subscribe({
        next: (url) => this.imageUrls.push(url),
        error: () => {}
      });
    });
    if (this.factory.imageKeys.length > 1) {
      this.galleryInterval = setInterval(() => {
        this.currentImageIndex = (this.currentImageIndex + 1) % this.imageUrls.length;
      }, 4000);
    }
  }

  loadProducts(): void {
    if (!this.factory?.productIds?.length) return;
    this.loadingProducts = true;
    const fetches = this.factory.productIds.map(pid =>
      this.productService.getById(pid).then((r: any) => r.data as Product)
    );
    Promise.all(fetches).then(products => {
      this.products = products;
      products.forEach(p => {
        if (p.image) {
          this.fileService.getDownloadUrl(p.image).subscribe({
            next: (url) => this.productImageUrls.set(p.id, url),
            error: () => {}
          });
        }
      });
    }).finally(() => {
      this.loadingProducts = false;
    });
  }

  selectProduct(product: Product): void {
    this.selectedProduct = product;
    this.chartHasData = false;
    this.chartError = '';
    this.destroyChart();
    this.loadChart();
  }

  onPeriodChange(): void {
    if (this.selectedProduct) this.loadChart();
  }

  loadChart(): void {
    if (!this.factory || !this.selectedProduct) return;
    if (this.selectedPeriod === 'CUSTOM' && (!this.customFrom || !this.customTo)) return;

    this.loadingChart = true;
    this.chartError = '';
    this.chartHasData = false;
    this.destroyChart();

    const from = this.selectedPeriod === 'CUSTOM' ? this.customFrom : undefined;
    const to   = this.selectedPeriod === 'CUSTOM' ? this.customTo   : undefined;

    this.factoryService.getProductionData(
      this.factory.id, this.selectedProduct.id, this.selectedPeriod, from, to
    ).then((res: any) => {
      const data = res.data as ProductionDataDto;
      if (!data.labels?.length) {
        this.chartHasData = false;
        return;
      }
      this.chartHasData = true;
      setTimeout(() => this.renderChart(data), 50);
    }).catch(() => {
      this.chartError = 'Failed to load production data.';
    }).finally(() => {
      this.loadingChart = false;
    });
  }

  private renderChart(data: ProductionDataDto): void {
    this.destroyChart();
    const canvas = this.chartCanvasRef?.nativeElement;
    if (!canvas) return;

    this.chartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: data.labels,
        datasets: [{
          label: this.selectedProduct?.name ?? 'Quantity',
          data: data.values,
          backgroundColor: 'rgba(79, 70, 229, 0.7)',
          borderColor: 'rgba(79, 70, 229, 1)',
          borderWidth: 1,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          title: { display: true, text: 'Units produced' }
        },
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } }
        }
      }
    });
  }

  private destroyChart(): void {
    if (this.chartInstance) {
      this.chartInstance.destroy();
      this.chartInstance = null;
    }
  }

  prevImage(): void {
    if (this.imageUrls.length === 0) return;
    this.currentImageIndex = (this.currentImageIndex - 1 + this.imageUrls.length) % this.imageUrls.length;
  }

  nextImage(): void {
    if (this.imageUrls.length === 0) return;
    this.currentImageIndex = (this.currentImageIndex + 1) % this.imageUrls.length;
  }

  formatHeartbeat(ts: string | null): string {
    if (!ts) return 'Never';
    const d = new Date(ts);
    if (isNaN(d.getTime())) return 'Unknown';
    return d.toLocaleString();
  }
}