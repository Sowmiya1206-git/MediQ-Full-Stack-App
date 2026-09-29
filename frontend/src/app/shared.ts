import { Component, Input, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Api, AuthService } from './core';

@Component({ selector: 'app-navbar', standalone: true, template: `
<nav class="navbar navbar-dark bg-primary px-3 mb-3"><span class="navbar-brand">MediQ · {{title}}</span>
  <span class="text-white">{{auth.user?.name}} <button class="btn btn-sm btn-light ms-2" (click)="auth.logout()">Logout</button></span></nav>` })
export class NavbarComponent { @Input() title = ''; auth = inject(AuthService); }

@Component({ selector: 'app-notifications', standalone: true, imports: [DatePipe], template: `
<div class="card p-3"><h5>Notifications</h5>
  @if (!items.length) { <p class="text-muted mb-0">No notifications.</p> }
  @for (n of items; track n.id) {
    <div class="d-flex justify-content-between border-bottom py-2" [class.fw-bold]="!n.seen">
      <div>{{n.message}}<div class="small text-muted fw-normal">{{n.createdAt | date:'medium'}}</div></div>
      @if (!n.seen) { <button class="btn btn-sm btn-outline-secondary align-self-start" (click)="read(n)">Mark read</button> }
    </div>
  }</div>` })
export class NotificationsComponent implements OnInit {
  private api = inject(Api); items: any[] = [];
  ngOnInit() { this.load(); setInterval(() => this.load(), 15000); }
  load() { this.api.get('/notifications').subscribe(r => this.items = r); }
  read(n: any) { this.api.post(`/notifications/${n.id}/read`).subscribe(() => n.seen = true); }
}
