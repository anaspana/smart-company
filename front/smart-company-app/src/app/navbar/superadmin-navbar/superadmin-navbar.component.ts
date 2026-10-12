import { Component, ViewEncapsulation } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-superadmin-navbar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './superadmin-navbar.component.html',
  styleUrl: './superadmin-navbar.component.css',
  encapsulation: ViewEncapsulation.None
})
export class SuperadminNavbarComponent {

}