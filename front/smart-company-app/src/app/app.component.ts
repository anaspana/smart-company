import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './navbar/navbar/navbar.component';
import { CommonModule } from '@angular/common';
import { AxiosService } from './auth/axios.service';
import { ChangePasswordComponent } from './auth/change-password/change-password.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CommonModule, ChangePasswordComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'SmartCompany';

  constructor(public axios: AxiosService) {
    
  }
}
