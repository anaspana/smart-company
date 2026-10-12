import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { AxiosService } from '../../auth/axios.service';
import { CommonModule } from '@angular/common';
import { SuperadminNavbarComponent } from '../superadmin-navbar/superadmin-navbar.component';
import { ManagerNavbarComponent } from '../manager-navbar/manager-navbar.component';
import { FileService } from '../../file.service';
import { CustomerNavbarComponent } from '../customer-navbar/customer-navbar.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, CommonModule, SuperadminNavbarComponent, ManagerNavbarComponent, CustomerNavbarComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit {

  preSignedUrl: string | null = null;

  constructor(public axios: AxiosService, private router: Router, public fileService: FileService, private route: ActivatedRoute) {
    this.fetchImage();
  }

  ngOnInit(): void {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.fetchImage();
      }
    });
  }

  fetchImage(): void {
    if (this.axios.getUser() != null) {
      this.fileService.getDownloadUrl(this.axios.getUser()!.image).subscribe({
        next: (response: string) => { this.preSignedUrl = response; },
        error: (err: any) => { console.log(err); }
      });
    }
  }

  logOut(): void {
    this.axios.setAuthToken(null);
    this.router.navigate(['login']);
    this.preSignedUrl = null;
  }
}