import { Injectable } from '@angular/core';
import { Observable, from, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AxiosService } from './auth/axios.service';

@Injectable({
  providedIn: 'root',
})
export class FileService {

  private urlCache = new Map<string, string>();

  constructor(private axiosService: AxiosService) {}

  // Returns { url: string, filename: string } (PreSignedUrlDto)
  getUploadUrl(filename: string, folder: string): Observable<{ url: string; filename: string }> {
    return from(
      this.axiosService.request(
        'PUT',
        `/files/presigned-url/put?filename=${encodeURIComponent(filename)}&folder=${encodeURIComponent(folder)}`,
        null
      ).then(response => {
        const data = response.data;
        if (typeof data === 'string') {
          return { url: data, filename: new URL(data).pathname.substring(1) };
        }
        return { url: data.url, filename: data.filename };
      })
    );
  }

  uploadToS3(presignedUrl: string, file: File): Observable<any> {
    // Ovo ide DIREKTNO na S3 (presigned URL), ne kroz naš backend/AxiosService —
    // ostaje čist axios/fetch poziv bez Authorization headera i bez /api prefiksa
    return from(
      fetch(presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': file.type },
        body: file
      })
    );
  }

  getDownloadUrl(key: string): Observable<string> {
    if (this.urlCache.has(key)) {
      return of(this.urlCache.get(key)!);
    }
    return from(
      this.axiosService.request(
        'GET',
        `/files/presigned-url/get?key=${encodeURIComponent(key)}`,
        null
      ).then(response => response.data)
    ).pipe(
      tap(url => this.urlCache.set(key, url))
    );
  }
}