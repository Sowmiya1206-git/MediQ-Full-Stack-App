import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, msg } from './core';

@Component({ selector: 'app-login', standalone: true, imports: [FormsModule, RouterLink], template: `
<div class="container" style="max-width:420px;margin-top:10vh"><div class="card p-4">
  <h3 class="text-primary">MediQ</h3><p class="text-muted">Medical Appointment &amp; Queue Management</p>
  @if (error) { <div class="alert alert-danger py-2">{{error}}</div> }
  <input class="form-control mb-2" placeholder="Email" [(ngModel)]="email" name="email">
  <input class="form-control mb-3" type="password" placeholder="Password" [(ngModel)]="password" name="pw" (keyup.enter)="go()">
  <button class="btn btn-primary w-100" (click)="go()" [disabled]="busy">Login</button>
  <div class="mt-3 text-center">New patient? <a routerLink="/register">Register</a></div>
  <div class="small text-muted mt-3">Demo: admin&#64;mediq.com / admin123 · doctor&#64;mediq.com / doctor123 · patient&#64;mediq.com / patient123</div>
</div></div>` })
export class LoginComponent {
  private auth = inject(AuthService); private router = inject(Router);
  email = ''; password = ''; error = ''; busy = false;
  go() {
    if (!this.email || !this.password) { this.error = 'Enter email and password'; return; }
    this.busy = true; this.error = '';
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: u => this.router.navigate([this.auth.home(u.role)]),
      error: e => { this.error = msg(e); this.busy = false; } });
  }
}

@Component({ selector: 'app-register', standalone: true, imports: [FormsModule, RouterLink], template: `
<div class="container" style="max-width:480px;margin-top:6vh"><div class="card p-4">
  <h4>Patient Registration</h4>
  @if (error) { <div class="alert alert-danger py-2">{{error}}</div> }
  <input class="form-control mb-2" placeholder="Full name" [(ngModel)]="f.name" name="n">
  <input class="form-control mb-2" placeholder="Email" [(ngModel)]="f.email" name="e">
  <input class="form-control mb-2" type="password" placeholder="Password (min 6)" [(ngModel)]="f.password" name="p">
  <input class="form-control mb-2" placeholder="Phone" [(ngModel)]="f.phone" name="ph">
  <div class="row g-2 mb-3"><div class="col"><input class="form-control" type="number" placeholder="Age" [(ngModel)]="f.age" name="a"></div>
    <div class="col"><select class="form-select" [(ngModel)]="f.gender" name="g"><option value="">Gender</option><option>Male</option><option>Female</option><option>Other</option></select></div></div>
  <button class="btn btn-primary w-100" (click)="go()">Create account</button>
  <div class="mt-3 text-center"><a routerLink="/login">Back to login</a></div>
</div></div>` })
export class RegisterComponent {
  private auth = inject(AuthService); private router = inject(Router);
  f: any = { name: '', email: '', password: '', phone: '', age: null, gender: '' }; error = '';
  go() {
    if (!this.f.name || !this.f.email || (this.f.password || '').length < 6) { this.error = 'Name, email and a password of 6+ characters are required'; return; }
    this.auth.register(this.f).subscribe({ next: u => this.router.navigate([this.auth.home(u.role)]), error: e => this.error = msg(e) });
  }
}
