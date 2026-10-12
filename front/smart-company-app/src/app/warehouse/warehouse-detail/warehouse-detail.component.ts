import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  Chart, CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  BarController, Title, Tooltip, Legend
} from 'chart.js';
import { WarehouseService } from '../warehouse.service';
import { FileService } from '../../file.service';
import { Warehouse, Sector, TemperatureDataDto } from '../warehouse.model';

Chart.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, BarController, Title, Tooltip, Legend);

type TemperaturePeriod = 'WEEK' | 'MONTH' | 'THREE_MONTHS' | 'SIX_MONTHS' | 'YEAR' | 'CUSTOM';

@Component({
  selector: 'app-warehouse-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './warehouse-detail.component.html',
  styleUrl: './warehouse-detail.component.css'
})
export class WarehouseDetailComponent implements OnInit, OnDestroy {

  warehouse: Warehouse | null = null;
  imageUrls: string[] = [];
  loadingWarehouse = true;
  error = '';

  currentImageIndex = 0;
  private galleryInterval: any;

  selectedSector: Sector | null = null;

  selectedPeriod: TemperaturePeriod = 'WEEK';
  customFrom = '';
  customTo = '';
  periods: { value: TemperaturePeriod; label: string }[] = [
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

  constructor(
    private route: ActivatedRoute,
    private warehouseService: WarehouseService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadWarehouse(id);
  }

  ngOnDestroy(): void {
    if (this.galleryInterval) clearInterval(this.galleryInterval);
    this.destroyChart();
  }

  loadWarehouse(id: number): void {
    this.loadingWarehouse = true;
    this.warehouseService.getById(id).then((res: any) => {
      this.warehouse = res.data as Warehouse;
      this.loadImages();
      setTimeout(() => this.initMap(), 100);
    }).catch(() => {
      this.error = 'Failed to load warehouse.';
    }).finally(() => this.loadingWarehouse = false);
  }

  private async initMap(): Promise<void> {
    if (!this.warehouse) return;

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
      .setView([this.warehouse.latitude, this.warehouse.longitude], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(map);
    L.marker([this.warehouse.latitude, this.warehouse.longitude]).addTo(map);
  }

  loadImages(): void {
    if (!this.warehouse?.imageKeys?.length) return;
    this.warehouse.imageKeys.forEach(key => {
      this.fileService.getDownloadUrl(key).subscribe({
        next: (url) => this.imageUrls.push(url),
        error: () => {}
      });
    });
    if (this.warehouse.imageKeys.length > 1) {
      this.galleryInterval = setInterval(() => {
        this.currentImageIndex = (this.currentImageIndex + 1) % this.imageUrls.length;
      }, 4000);
    }
  }

  selectSector(sector: Sector): void {
    this.selectedSector = sector;
    this.chartHasData = false;
    this.chartError = '';
    this.destroyChart();
    this.loadChart();
  }

  onPeriodChange(): void {
    if (this.selectedSector) this.loadChart();
  }

  loadChart(): void {
    if (!this.warehouse || !this.selectedSector) return;
    if (this.selectedPeriod === 'CUSTOM' && (!this.customFrom || !this.customTo)) return;

    this.loadingChart = true;
    this.chartError = '';
    this.chartHasData = false;
    this.destroyChart();

    const from = this.selectedPeriod === 'CUSTOM' ? this.customFrom : undefined;
    const to   = this.selectedPeriod === 'CUSTOM' ? this.customTo   : undefined;

    this.warehouseService.getTemperatureData(
      this.warehouse.id, this.selectedSector.id, this.selectedPeriod, from, to
    ).then((res: any) => {
      const data = res.data as TemperatureDataDto;
      if (!data.labels?.length) {
        this.chartHasData = false;
        return;
      }
      this.chartHasData = true;
      setTimeout(() => this.renderChart(data), 50);
    }).catch(() => {
      this.chartError = 'Failed to load temperature data.';
    }).finally(() => this.loadingChart = false);
  }

  private renderChart(data: TemperatureDataDto): void {
    this.destroyChart();
    const canvas = this.chartCanvasRef?.nativeElement;
    if (!canvas) return;

    this.chartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: data.labels,
        datasets: [{
          label: `Avg temperature (°C) — ${this.selectedSector?.name}`,
          data: data.values,
          backgroundColor: 'rgba(14, 165, 233, 0.7)',
          borderColor: 'rgba(14, 165, 233, 1)',
          borderWidth: 1,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          title: { display: true, text: 'Average temperature (°C)' }
        },
        scales: {
          y: { beginAtZero: false, ticks: { callback: (v) => v + '°C' } }
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
    if (!this.imageUrls.length) return;
    this.currentImageIndex = (this.currentImageIndex - 1 + this.imageUrls.length) % this.imageUrls.length;
  }

  nextImage(): void {
    if (!this.imageUrls.length) return;
    this.currentImageIndex = (this.currentImageIndex + 1) % this.imageUrls.length;
  }

  formatHeartbeat(ts: string | null): string {
    if (!ts) return 'Never';
    const d = new Date(ts);
    return isNaN(d.getTime()) ? 'Unknown' : d.toLocaleString();
  }
}