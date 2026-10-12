import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AxiosService } from '../axios.service';

@Component({
  selector: 'app-activation',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activation.component.html',
  styleUrl: './activation.component.css'
})
export class ActivationComponent implements OnInit {
  activationMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private axios: AxiosService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const params = this.route.snapshot.queryParams;
    const token = params['token'];
    const username = params['username'];

    if (token && username) {
      this.activateAccount(token, username);
    } else {
      this.activationMessage = 'Invalid activation link.';
    }
  }

  activateAccount(token: string, username: string): void {
    this.axios.request('POST', '/activate/' + username + '?token=' + token, null)
      .then(response => {
        this.activationMessage = response.data;
        setTimeout(() => this.router.navigate(['login']), 2000);
      })
      .catch(() => {
        this.activationMessage = 'Activation failed. Link may have already been used.';
      });
  }
}