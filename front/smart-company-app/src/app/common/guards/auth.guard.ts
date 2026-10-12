import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AxiosService } from '../../auth/axios.service';

export const authGuard: CanActivateFn = () => {
  const axios = inject(AxiosService);
  const router = inject(Router);

  // Nije ulogovan
  if (!axios.getAuthToken()) {
    router.navigate(['login']);
    return false;
  }

  // Mora promeniti lozinku blokira pristup svemu osim /change-password
  if (axios.getMustChangePassword()) {
    router.navigate(['change-password']);
    return false;
  }

  return true;
};

export const mustChangePasswordGuard: CanActivateFn = () => {
  const axios = inject(AxiosService);
  const router = inject(Router);

  // Ako je ulogovan i ne mora menjati lozinku, nema razloga da bude na /change-password
  if (axios.getAuthToken() && !axios.getMustChangePassword()) {
    router.navigate(['products']);
    return false;
  }

  return true;
};