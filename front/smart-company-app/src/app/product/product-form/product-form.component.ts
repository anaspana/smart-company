import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProductService } from '../product.service';
import { FileService } from '../../file.service';
import { HttpClient } from '@angular/common/http';
import { Category } from '../product.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnInit {

  form: FormGroup;
  categoryForm: FormGroup;
  categories: Category[] = [];

  isEditMode = false;
  productId: number | null = null;

  selectedFile: File | null = null;
  existingImageKey: string | null = null;
  imagePreviewUrl: string | null = null;
  fileError = '';

  submitting = false;
  formSubmitted = false;
  errorMessage = '';

  // Modal state
  showCategoryModal = false;
  categorySubmitting = false;
  categoryError = '';
  categorySuccess = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private fileService: FileService,
    private http: HttpClient
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      price: [null, [Validators.required, Validators.min(0.01)]],
      weight: [null, [Validators.required, Validators.min(0.01)]],
      categoryId: [null, Validators.required],
      isForSale: [true],
      file: [null]
    });

    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.loadCategories();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.productId = Number(idParam);
      this.loadProduct(this.productId);
    } else {
      this.form.get('file')?.setValidators(Validators.required);
      this.form.get('file')?.updateValueAndValidity();
    }
  }

  loadCategories(): void {
    this.productService.getCategories().then((res: any) => {
      this.categories = res.data;
    });
  }

  loadProduct(id: number): void {
    this.productService.getById(id).then((res: any) => {
      const p = res.data;
      this.form.patchValue({
        name: p.name,
        description: p.description,
        price: p.price,
        weight: p.weight,
        categoryId: p.category.id,
        isForSale: p.forSale
      });
      this.existingImageKey = p.image;
      if (p.image) {
        this.fileService.getDownloadUrl(p.image).subscribe({
          next: (url) => this.imagePreviewUrl = url,
          error: () => {}
        });
      }
    });
  }

  // --- Category modal ---

  openCategoryModal(): void {
    this.categoryForm.reset();
    this.categoryError = '';
    this.categorySuccess = '';
    this.showCategoryModal = true;
  }

  closeCategoryModal(): void {
    this.showCategoryModal = false;
  }

  onCategorySubmit(): void {
    this.categoryForm.markAllAsTouched();
    if (this.categoryForm.invalid) return;

    this.categorySubmitting = true;
    this.categoryError = '';

    const payload = {
      name: this.categoryForm.value.name,
      description: this.categoryForm.value.description || ''
    };

    this.productService.createCategory(payload).then((res: any) => {
      const newCategory: Category = res.data;
      this.categories = [...this.categories, newCategory];
      this.form.patchValue({ categoryId: newCategory.id });
      this.categorySuccess = `Category "${newCategory.name}" created successfully!`;
      setTimeout(() => this.closeCategoryModal(), 1200);
    }).catch((err: any) => {
      this.categoryError = err.response?.data?.message || 'Failed to create category.';
    }).finally(() => {
      this.categorySubmitting = false;
    });
  }

  isCategoryInvalid(field: string): boolean {
    const control = this.categoryForm.get(field);
    return !!(control?.invalid && control.touched);
  }

  // --- Product form ---

  onFileSelected(event: any): void {
    const file: File = event.target.files?.[0];
    if (!file) { this.selectedFile = null; return; }

    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      this.fileError = 'Invalid file type. Please select an image (.jpg, .png, .gif, .webp).';
      this.selectedFile = null;
      this.form.get('file')?.setValue(null);
      return;
    }

    this.fileError = '';
    this.selectedFile = file;

    const reader = new FileReader();
    reader.onload = (e) => this.imagePreviewUrl = e.target?.result as string;
    reader.readAsDataURL(file);
  }

  onSubmit(): void {
    this.formSubmitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    if (!this.isEditMode && !this.selectedFile) return;

    this.submitting = true;

    if (this.selectedFile) {
      this.fileService.getUploadUrl(this.selectedFile.name, 'product-images').subscribe({
        next: (res) => {
          this.fileService.uploadToS3(res.url, this.selectedFile!).subscribe({
            next: () => this.saveProduct(res.filename),
            error: () => {
              this.errorMessage = 'Image upload failed. Please try again.';
              this.submitting = false;
            }
          });
        },
        error: () => {
          this.errorMessage = 'Could not get upload URL. Please try again.';
          this.submitting = false;
        }
      });
    } else {
      this.saveProduct(this.existingImageKey);
    }
  }

  saveProduct(imageKey: string | null): void {
    const payload = {
      name: this.form.value.name,
      description: this.form.value.description,
      price: parseFloat(Number(this.form.value.price).toFixed(2)),
      weight: this.form.value.weight,
      image: imageKey,
      isForSale: this.form.value.isForSale,
      categoryId: Number(this.form.value.categoryId),
    };

    const request = this.isEditMode
      ? this.productService.update(this.productId!, payload)
      : this.productService.create(payload);

    request.then(() => {
      this.router.navigate(['/products']);
    }).catch((err: any) => {
      this.errorMessage = err.response?.data?.message || 'Something went wrong.';
      this.submitting = false;
    });
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control?.invalid && (control.touched || this.formSubmitted));
  }
}