import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { CompanyService } from '../company.service';
import { CompanyRequestSummary, Page } from '../company.model';

@Component({
  selector: 'app-company-request-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './company-request-list.component.html',
  styleUrl: './company-request-list.component.css'
})
export class CompanyRequestListComponent implements OnInit, OnDestroy {

  requests: CompanyRequestSummary[] = [];

  search = '';
  status: string = 'PENDING';

  currentPage = 0;
  pageSize = 9;
  totalPages = 0;
  totalElements = 0;

  loading = false;
  errorMessage = '';

  private searchSubject = new Subject<string>();

  constructor(private companyService: CompanyService) {}

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadRequests();
    });

    this.loadRequests();
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  onSearchChange(): void {
    this.searchSubject.next(this.search);
  }

  onStatusChange(): void {
    this.currentPage = 0;
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.errorMessage = '';

    const statusParam = this.status === 'ALL' ? null : this.status;

    this.companyService.search(this.search, statusParam, this.currentPage, this.pageSize)
      .then((res: any) => {
        const page: Page<CompanyRequestSummary> = res.data;
        this.requests = page.content;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
      })
      .catch(() => this.errorMessage = 'Greška pri učitavanju zahteva.')
      .finally(() => this.loading = false);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadRequests();
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
}