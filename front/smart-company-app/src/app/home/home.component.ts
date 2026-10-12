import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AxiosService } from '../auth/axios.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {

  constructor(private axiosService: AxiosService, private router: Router) {}

  ngOnInit(): void {
    if (this.axiosService.getAuthToken()) {
      this.router.navigate(['products']);
    } else {
      this.router.navigate(['login']);
    }
  }
}