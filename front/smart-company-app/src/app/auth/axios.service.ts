import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import axios, { Method } from 'axios';
import { JwtHelperService } from '@auth0/angular-jwt';
import { User, UserType } from './model/user';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AxiosService {

  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
    axios.defaults.baseURL = this.isBrowser ? '/api' : 'http://localhost:8080/api';
    axios.defaults.headers.post['Content-Type'] = 'application/json';
    axios.interceptors.response.use(
      response => response,
      error => {
        if (error.response?.status === 401) {
          localStorage.removeItem('auth_token');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  getAuthToken(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem('auth_token');
  }

  setAuthToken(token: string | null): void {
    if (!this.isBrowser) return;
    if (token) {
      localStorage.setItem('auth_token', token);
    } else {
      localStorage.removeItem('auth_token');
    }
  }

  getUser(): User | null {
    const token = this.getAuthToken();
    if (!token) return null;

    const helper = new JwtHelperService();
    const decodedToken: any = helper.decodeToken(token);
    if (!decodedToken) return null;

    return new User(
      decodedToken['sub'],
      '',
      decodedToken['type'] as UserType,
      decodedToken['isActive'] === 'true',
      decodedToken['mustChangePassword'] === 'true',
      decodedToken['email'],
      decodedToken['image']
    );
  }

  getRole(): UserType | null {
    return this.getUser()?.type ?? null;
  }

  getUsername(): string {
    return this.getUser()?.username ?? '';
  }

  getIsActive(): boolean {
    return this.getUser()?.isActive ?? false;
  }

  getMustChangePassword(): boolean {
    return this.getUser()?.mustChangePassword ?? false;
  }

  request(method: string, url: string, data: any): Promise<any> {
    let headers: any = {};
    if (this.getAuthToken() !== null) {
      headers = { "Authorization": "Bearer " + this.getAuthToken() };
    }
    return axios({ method: method as Method, url, data, headers });
  }
}