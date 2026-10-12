import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { RegisterManagerComponent } from './superadmin/register-manager/register-manager.component';
import { ManagerListComponent } from './superadmin/manager-list/manager-list.component';
import { HomeComponent } from './home/home.component';
import { ActivationComponent } from './auth/activation/activation.component';
import { ProductListComponent } from './product/product-list/product-list.component';
import { ProductFormComponent } from './product/product-form/product-form.component';
import { ChangePasswordComponent } from './auth/change-password/change-password.component';
import { FactoryListComponent } from './factory/factory-list/factory-list.component';
import { FactoryFormComponent } from './factory/factory-form/factory-form.component';
import { FactoryDetailComponent } from './factory/factory-detail/factory-detail.component';
import { authGuard, mustChangePasswordGuard } from './common/guards/auth.guard';
import { OrderFormComponent } from './order/order-form/order-form.component';
import { WarehouseListComponent } from './warehouse/warehouse-list/warehouse-list.component';
import { WarehouseDetailComponent } from './warehouse/warehouse-detail/warehouse-detail.component';
import { WarehouseFormComponent } from './warehouse/warehouse-form/warehouse-form.component';
import { CompanyFormComponent } from './company/company-form/company-form.component';
import { CompanyRequestListComponent } from './company/company-request-list/company-request-list.component';
import { CompanyRequestDetailComponent } from './company/company-request-detail/company-request-detail.component';
import { VehicleDetailComponent } from './vehicle/vehicle-detail/vehicle-detail.component';
import { VehicleFormComponent } from './vehicle/vehicle-form/vehicle-form.component';
import { VehicleListComponent } from './vehicle/vehicle-list/vehicle-list.component';

export const routes: Routes = [
  { path: '',                   component: HomeComponent },

  // Javne rute
  { path: 'login',              component: LoginComponent },
  { path: 'register',           component: RegisterComponent },
  { path: 'activate',           component: ActivationComponent },

  { path: 'change-password',    component: ChangePasswordComponent, canActivate: [mustChangePasswordGuard] },

  // Proizvodi
  { path: 'products',           component: ProductListComponent,     canActivate: [authGuard] },
  { path: 'products/new',       component: ProductFormComponent,     canActivate: [authGuard] },
  { path: 'products/edit/:id',  component: ProductFormComponent,     canActivate: [authGuard] },

  // Menadžeri
  { path: 'register-manager',   component: RegisterManagerComponent, canActivate: [authGuard] },
  { path: 'managers',           component: ManagerListComponent,     canActivate: [authGuard] },

  // Fabrike
  { path: 'factories',          component: FactoryListComponent,     canActivate: [authGuard] },
  { path: 'factories/new',      component: FactoryFormComponent,     canActivate: [authGuard] },
  { path: 'factories/edit/:id', component: FactoryFormComponent,     canActivate: [authGuard] },
  { path: 'factories/:id',      component: FactoryDetailComponent,   canActivate: [authGuard] },

  // Narudžbine
  { path: 'orders/new', component: OrderFormComponent, canActivate: [authGuard] },

  // Firme
  { path: 'companies/new', component: CompanyFormComponent, canActivate: [authGuard] },
  { path: 'companies/manage',     component: CompanyRequestListComponent,   canActivate: [authGuard] },
  { path: 'companies/manage/:id', component: CompanyRequestDetailComponent, canActivate: [authGuard] },

  // Magacini
  { path: 'warehouses',          component: WarehouseListComponent,   canActivate: [authGuard] },
  { path: 'warehouses/new',      component: WarehouseFormComponent,   canActivate: [authGuard] },
  { path: 'warehouses/edit/:id', component: WarehouseFormComponent,   canActivate: [authGuard] },
  { path: 'warehouses/:id',      component: WarehouseDetailComponent, canActivate: [authGuard] },

  // Vozila
  { path: 'vehicles',          component: VehicleListComponent,   canActivate: [authGuard] },
  { path: 'vehicles/new',      component: VehicleFormComponent,   canActivate: [authGuard] },
  { path: 'vehicles/edit/:id', component: VehicleFormComponent,   canActivate: [authGuard] },
  { path: 'vehicles/:id',      component: VehicleDetailComponent, canActivate: [authGuard] },

  { path: '**',                 redirectTo: '' }
];