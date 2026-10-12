import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AxiosService } from '../../auth/axios.service';
import { FileService } from '../../file.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Router } from '@angular/router';

interface Manager {
  username: string;
  email: string;
  image: string;
  isActive: boolean;
  mustChangePassword: boolean;
  type: string;
  resolvedImageUrl?: string;
}

interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
}

@Component({
  selector: 'app-manager-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './manager-list.component.html',
  styleUrl: './manager-list.component.css'
})
export class ManagerListComponent implements OnInit, OnDestroy {

  managers: Manager[] = [];
  search: string = '';
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  pageSize: number = 9;
  loading: boolean = false;
  errorMessage: string = '';


  private searchSubject = new Subject<string>();

  constructor(private axios: AxiosService, private fileService: FileService, private router: Router) {}

  ngOnInit(): void {
    
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadManagers();
    });

    this.loadManagers();
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  onSearchChange(): void {
    this.searchSubject.next(this.search);
  }

  loadManagers(): void {
    this.loading = true;
    this.errorMessage = '';

    this.axios.request('GET', `/users/managers?search=${encodeURIComponent(this.search)}&page=${this.currentPage}&size=${this.pageSize}`, null)
      .then((response: any) => {
        const page: Page<Manager> = response.data;
        this.managers = page.content;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.currentPage = page.number;

        // Učitaj presigned URL za svaku sliku
        this.managers.forEach(manager => {
          if (manager.image && manager.image !== 'null' && manager.image !== 'profile.jpg') {
            this.fileService.getDownloadUrl(manager.image).subscribe({
              next: (url: string) => { manager.resolvedImageUrl = url; },
              error: () => { manager.resolvedImageUrl = 'assets/default-avatar.png'; }
            });
          } else {
            manager.resolvedImageUrl = 'assets/default-avatar.png';
          }
        });
      })
      .catch(() => {
        this.errorMessage = 'Greška pri učitavanju menadžera.';
      })
      .finally(() => {
        this.loading = false;
      });
  }

  toggleBlock(manager: Manager): void {
    const endpoint = manager.isActive
      ? `/users/${manager.username}/block`
      : `/users/${manager.username}/unblock`;

    this.axios.request('PUT', endpoint, null)
      .then((response: any) => {
        manager.isActive = response.data.isActive;
      })
      .catch(() => {
        this.errorMessage = 'Greška pri blokiranju/odblokiravanju menadžera.';
      });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadManagers();
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

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }
}