import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { VehicleService } from '../vehicle.service';
import { FileService } from '../../file.service';
import { Vehicle } from '../vehicle.model';

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './vehicle-form.component.html',
})
export class VehicleFormComponent implements OnInit {

  isEditMode = false;
  vehicleId: number | null = null;
  loading = false;
  error = '';
  uploadingImages = false;

  licensePlate = '';
  weightLimit: number | null = null;
  selectedBrandId: number | null = null;
  selectedModelId: number | null = null;
  imageKeys: string[] = [];
  imageUrls: string[] = [];

  brands: any[] = [];
  models: any[] = [];

  constructor(
    private vehicleService: VehicleService,
    private fileService: FileService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.vehicleService.getBrands().then((res: any) => {
      this.brands = res.data;
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.vehicleId = +id;
        this.loadVehicle(this.vehicleId);
      }
    });
  }

  loadVehicle(id: number): void {
    this.loading = true;
    this.vehicleService.getById(id).then((res: any) => {
      const v: Vehicle = res.data;
      this.licensePlate = v.licensePlate;
      this.weightLimit = v.weightLimit;
      this.selectedBrandId = v.brand.id;
      this.imageKeys = [...v.imageKeys];

      this.vehicleService.getModels(v.brand.id).then((modelRes: any) => {
        this.models = modelRes.data;
        this.selectedModelId = v.model.id;
      });

      this.imageKeys.forEach(key => {
        this.fileService.getDownloadUrl(key).subscribe({
          next: (url) => this.imageUrls.push(url),
          error: () => {}
        });
      });
    }).finally(() => this.loading = false);
  }

  onBrandChange(): void {
    this.selectedModelId = null;
    this.models = [];
    if (this.selectedBrandId) {
      this.vehicleService.getModels(this.selectedBrandId).then((res: any) => {
        this.models = res.data;
      });
    }
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.uploadingImages = true;
    const files = Array.from(input.files);
    let done = 0;

    files.forEach(file => {
      this.fileService.getUploadUrl(file.name, 'vehicle-images').subscribe({
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
    return !!this.licensePlate && this.weightLimit != null
      && !!this.selectedBrandId && !!this.selectedModelId;
  }

  submit(): void {
    if (!this.isValid()) {
      this.error = 'Please fill in all required fields.';
      return;
    }
    this.loading = true;
    this.error = '';

    const payload = {
      licensePlate: this.licensePlate,
      weightLimit: this.weightLimit!,
      brandId: this.selectedBrandId!,
      modelId: this.selectedModelId!,
      imageKeys: this.imageKeys
    };

    const req = this.isEditMode
      ? this.vehicleService.update(this.vehicleId!, payload)
      : this.vehicleService.create(payload);

    req.then(() => {
      this.router.navigate(['/vehicles']);
    }).catch((err: any) => {
      this.error = err?.response?.data?.message || 'An error occurred.';
    }).finally(() => this.loading = false);
  }
}