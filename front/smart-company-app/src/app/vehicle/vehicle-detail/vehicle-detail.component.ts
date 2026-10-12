import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import {
  Chart, CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  BarController, Title, Tooltip, Legend
} from 'chart.js';
import { VehicleService } from '../vehicle.service';
import { FileService } from '../../file.service';
import { Vehicle, DistanceDataDto } from '../vehicle.model';

Chart.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, BarController, Title, Tooltip, Legend);

type DistancePeriod = 'WEEK' | 'MONTH' | 'THREE_MONTHS' | 'SIX_MONTHS' | 'YEAR' | 'CUSTOM';

@Component({
  selector: 'app-vehicle-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './vehicle-detail.component.html',
  styleUrl: './vehicle-detail.component.css'
})
export class VehicleDetailComponent implements OnInit, OnDestroy {

  vehicle: Vehicle | null = null;
  imageUrls: string[] = [];
  loading = true;
  error = '';

  currentImageIndex = 0;
  private galleryInterval: any;

  selectedPeriod: DistancePeriod = 'WEEK';
  customFrom = '';
  customTo = '';
  periods: { value: DistancePeriod; label: string }[] = [
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
    private vehicleService: VehicleService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  ngOnDestroy(): void {
    if (this.galleryInterval) clearInterval(this.galleryInterval);
    this.destroyChart();
  }

  load(id: number): void {
    this.loading = true;
    this.vehicleService.getById(id).then((res: any) => {
      this.vehicle = res.data as Vehicle;
      this.loadImages();
      if (this.vehicle.latitude && this.vehicle.longitude) {
        setTimeout(() => this.initMap(), 100);
      }
      this.loadChart();
    }).catch(() => {
      this.error = 'Failed to load vehicle.';
    }).finally(() => this.loading = false);
  }

  private async initMap(): Promise<void> {
    if (!this.vehicle?.latitude || !this.vehicle?.longitude) return;
    
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
      .setView([this.vehicle.latitude, this.vehicle.longitude], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(map);
    L.marker([this.vehicle.latitude, this.vehicle.longitude]).addTo(map);
  }

  loadImages(): void {
    if (!this.vehicle?.imageKeys?.length) return;
    this.vehicle.imageKeys.forEach(key => {
      this.fileService.getDownloadUrl(key).subscribe({
        next: (url) => this.imageUrls.push(url),
        error: () => {}
      });
    });
    if (this.vehicle.imageKeys.length > 1) {
      this.galleryInterval = setInterval(() => {
        this.currentImageIndex = (this.currentImageIndex + 1) % this.imageUrls.length;
      }, 4000);
    }
  }

  onPeriodChange(): void {
    this.loadChart();
  }

  loadChart(): void {
    if (!this.vehicle) return;
    if (this.selectedPeriod === 'CUSTOM' && (!this.customFrom || !this.customTo)) return;

    this.loadingChart = true;
    this.chartError = '';
    this.chartHasData = false;
    this.destroyChart();

    const from = this.selectedPeriod === 'CUSTOM' ? this.customFrom : undefined;
    const to   = this.selectedPeriod === 'CUSTOM' ? this.customTo   : undefined;

    this.vehicleService.getDistanceData(
      this.vehicle.id, this.selectedPeriod, from, to
    ).then((res: any) => {
      const data = res.data as DistanceDataDto;
      if (!data.labels?.length) {
        this.chartHasData = false;
        return;
      }
      this.chartHasData = true;
      setTimeout(() => this.renderChart(data), 50);
    }).catch(() => {
      this.chartError = 'Failed to load distance data.';
    }).finally(() => this.loadingChart = false);
  }

  private renderChart(data: DistanceDataDto): void {
    this.destroyChart();
    const canvas = this.chartCanvasRef?.nativeElement;
    if (!canvas) return;

    this.chartInstance = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: data.labels,
        datasets: [{
          label: `Distance traveled (km) — ${this.vehicle?.licensePlate}`,
          data: data.values,
          backgroundColor: 'rgba(99, 153, 34, 0.7)',
          borderColor: 'rgba(99, 153, 34, 1)',
          borderWidth: 1,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          title: { display: true, text: 'Total distance traveled (km)' }
        },
        scales: {
          y: { beginAtZero: true, ticks: { callback: (v) => v + ' km' } }
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

  formatHeartbeat(ts: string | null): string {
    if (!ts) return 'Never';
    const d = new Date(ts);
    return isNaN(d.getTime()) ? 'Unknown' : d.toLocaleString();
  }
}