import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { tap } from 'rxjs';

export const API = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient); private router = inject(Router);
  get user(): any { const s = localStorage.getItem('mediq_user'); return s ? JSON.parse(s) : null; }
  get token(): string | null { return this.user?.token ?? null; }
  login(b: any) { return this.http.post<any>(`${API}/auth/login`, b).pipe(tap(u => localStorage.setItem('mediq_user', JSON.stringify(u)))); }
  register(b: any) { return this.http.post<any>(`${API}/auth/register`, b).pipe(tap(u => localStorage.setItem('mediq_user', JSON.stringify(u)))); }
  logout() { localStorage.removeItem('mediq_user'); this.router.navigate(['/login']); }
  home(role: string) { return '/' + role.toLowerCase(); }
}

/** Attach JWT; log out on 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const t = auth.token;
  return next(t ? req.clone({ setHeaders: { Authorization: `Bearer ${t}` } }) : req);
};

/** Role guard: route data { role: 'ADMIN' | 'DOCTOR' | 'PATIENT' } */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService); const router = inject(Router); const u = auth.user;
  if (!u) return router.parseUrl('/login');
  if (route.data['role'] && u.role !== route.data['role']) return router.parseUrl(auth.home(u.role));
  return true;
};

@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  get = (p: string, params?: any) => this.http.get<any>(`${API}${p}`, { params });
  post = (p: string, b: any = {}) => this.http.post<any>(`${API}${p}`, b);
  put = (p: string, b: any = {}) => this.http.put<any>(`${API}${p}`, b);
  del = (p: string) => this.http.delete<any>(`${API}${p}`);
}

export const msg = (e: any) => e?.error?.message || 'Network or server error. Please try again.';
