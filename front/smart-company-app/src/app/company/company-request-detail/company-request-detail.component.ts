import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CompanyService } from '../company.service';
import { FileService } from '../../file.service';
import { CompanyRequestSummary } from '../company.model';

@Component({
  selector: 'app-company-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './company-request-detail.component.html',
})
export class CompanyRequestDetailComponent implements OnInit {

  company: CompanyRequestSummary | null = null;
  imageUrls: string[] = [];
  documentUrls: { url: string; fileType: string }[] = [];

  loading = true;
  submitting = false;
  error = '';

  rejectionReason = '';
  showRejectForm = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private companyService: CompanyService,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.companyService.getByIdForManager(id).then((res: any) => {
      this.company = res.data as CompanyRequestSummary;
      this.loadImages();
      this.loadDocuments();
    }).catch(() => {
      this.error = 'Failed to load company request.';
    }).finally(() => this.loading = false);
  }

  loadImages(): void {
    this.company?.imageKeys.forEach(key => {
      this.fileService.getDownloadUrl(key).subscribe({
        next: (url) => this.imageUrls.push(url),
        error: () => {}
      });
    });
  }

  loadDocuments(): void {
    this.company?.documents.forEach(doc => {
      this.fileService.getDownloadUrl(doc.fileKey).subscribe({
        next: (url) => this.documentUrls.push({ url, fileType: doc.fileType }),
        error: () => {}
      });
    });
  }

  approve(): void {
    if (!this.company) return;
    this.submitting = true;
    this.error = '';
    this.companyService.decide(this.company.id, true).then(() => {
      this.router.navigate(['/companies/manage']);
    }).catch((err: any) => {
      this.error = err?.response?.data?.message || 'Failed to approve request.';
    }).finally(() => this.submitting = false);
  }

  reject(): void {
    if (!this.company) return;
    if (!this.rejectionReason.trim()) {
      this.error = 'Please provide a rejection reason.';
      return;
    }
    this.submitting = true;
    this.error = '';
    this.companyService.decide(this.company.id, false, this.rejectionReason).then(() => {
      this.router.navigate(['/companies/manage']);
    }).catch((err: any) => {
      this.error = err?.response?.data?.message || 'Failed to reject request.';
    }).finally(() => this.submitting = false);
  }
}