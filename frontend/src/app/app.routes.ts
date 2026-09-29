import { Routes } from '@angular/router';
import { roleGuard } from './core';
import { LoginComponent, RegisterComponent } from './auth.components';
import { PatientComponent } from './patient.component';
import { DoctorComponent } from './doctor.component';
import { AdminComponent } from './admin.component';
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'patient', component: PatientComponent, canActivate: [roleGuard], data: { role: 'PATIENT' } },
  { path: 'doctor', component: DoctorComponent, canActivate: [roleGuard], data: { role: 'DOCTOR' } },
  { path: 'admin', component: AdminComponent, canActivate: [roleGuard], data: { role: 'ADMIN' } },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
