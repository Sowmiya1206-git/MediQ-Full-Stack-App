import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Api, msg } from './core';
import { NavbarComponent, NotificationsComponent } from './shared';

@Component({ selector: 'app-patient', standalone: true, imports: [FormsModule, DatePipe, NavbarComponent, NotificationsComponent], template: `
<app-navbar title="Patient" />
<div class="container">
  <ul class="nav nav-tabs mb-3">
    @for (t of tabs; track t) { <li class="nav-item"><a class="nav-link" [class.active]="tab===t" (click)="tab=t">{{t}}</a></li> }
  </ul>
  @if (error) { <div class="alert alert-danger">{{error}}</div> }
  @if (ok) { <div class="alert alert-success">{{ok}}</div> }

  @if (tab==='Book Appointment') {
    <div class="card p-3">
      <div class="row g-2 mb-3">
        <div class="col-md-4"><select class="form-select" [(ngModel)]="deptId" (change)="loadDoctors()"><option [ngValue]="null">All departments</option>
          @for (d of depts; track d.id) { <option [ngValue]="d.id">{{d.name}}</option> }</select></div>
        <div class="col-md-5"><input class="form-control" placeholder="Search doctor / specialization" [(ngModel)]="q" (input)="loadDoctors()"></div>
      </div>
      <div class="row">
        <div class="col-md-5">
          @for (d of doctors; track d.id) {
            <div class="border rounded p-2 mb-2" [class.border-primary]="doctor?.id===d.id" role="button" (click)="pick(d)">
              <b>{{d.name}}</b><div class="small text-muted">{{d.specialization}} · {{d.department?.name}} · {{d.experienceYears}} yrs</div></div>
          }
          @if (!doctors.length) { <p class="text-muted">No doctors found.</p> }
        </div>
        <div class="col-md-7">
          @if (doctor) {
            <h6>Available slots — {{doctor.name}}</h6>
            @if (!slots.length) { <p class="text-muted">No schedule available.</p> }
            @for (s of slots; track s.id) {
              <button class="btn btn-sm me-1 mb-1" [class.btn-primary]="slot?.id===s.id" [class.btn-outline-primary]="slot?.id!==s.id" (click)="slot=s">{{s.date}} {{s.startTime.substring(0,5)}}</button>
            }
            @if (slot) {
              <input class="form-control my-2" placeholder="Reason for visit (optional)" [(ngModel)]="reason">
              <button class="btn btn-success" (click)="book()">Confirm booking for {{slot.date}} {{slot.startTime.substring(0,5)}}</button>
            }
          }
        </div>
      </div>
    </div>
  }

  @if (tab==='My Appointments') {
    <div class="card p-3">
      @if (!appts.length) { <p class="text-muted mb-0">No appointments yet.</p> }
      @for (a of appts; track a.id) {
        <div class="border-bottom py-2 d-flex justify-content-between flex-wrap">
          <div><b>{{a.doctor.name}}</b> <span class="badge bg-secondary">{{a.status}}</span>
            <div class="small text-muted">{{a.slot.date}} {{a.slot.startTime.substring(0,5)}} — {{a.reason}}</div>
            @if (queues[a.id]; as q) {
              <div>Token <span class="token">{{q.token}}</span> · <span class="badge bg-info text-dark">{{q.status}}</span>
                @if (q.position) { · Position {{q.position}} }
                @if (q.estimatedWaitMinutes !== null && q.estimatedWaitMinutes !== undefined) { · Est. wait {{q.estimatedWaitMinutes}} min }</div>
            }
          </div>
          <div class="align-self-center">
            @if (a.status==='BOOKED') { <button class="btn btn-sm btn-outline-danger" (click)="cancel(a)">Cancel</button> }
            @if (a.status==='COMPLETED') { <button class="btn btn-sm btn-outline-primary" (click)="fbFor=a">Feedback</button> }
          </div>
        </div>
      }
      @if (fbFor) {
        <div class="mt-3 border rounded p-3"><b>Feedback for {{fbFor.doctor.name}}</b>
          <select class="form-select my-2" [(ngModel)]="fb.rating"><option [ngValue]="5">5 - Excellent</option><option [ngValue]="4">4</option><option [ngValue]="3">3</option><option [ngValue]="2">2</option><option [ngValue]="1">1 - Poor</option></select>
          <textarea class="form-control mb-2" placeholder="Comments" [(ngModel)]="fb.comments"></textarea>
          <button class="btn btn-primary btn-sm" (click)="sendFeedback()">Submit</button> <button class="btn btn-link btn-sm" (click)="fbFor=null">Close</button></div>
      }
    </div>
  }

  @if (tab==='Medical History') {
    <div class="card p-3">
      @if (!history.length) { <p class="text-muted mb-0">No consultation records.</p> }
      @for (c of history; track c.id) {
        <div class="border-bottom py-2"><b>{{c.appointment.doctor.name}}</b> — {{c.createdAt | date:'mediumDate'}}
          <div>Diagnosis: {{c.diagnosis}}</div><div class="text-muted">Notes: {{c.notes}}</div>
          @for (p of c.prescriptions; track p.id) { <div class="small">💊 {{p.medicine}} — {{p.dosage}} ({{p.duration}})</div> }</div>
      }
    </div>
  }

  @if (tab==='Notifications') { <app-notifications /> }

  @if (tab==='Profile') {
    <div class="card p-3" style="max-width:480px">
      <input class="form-control mb-2" [(ngModel)]="profile.name" placeholder="Name">
      <input class="form-control mb-2" [(ngModel)]="profile.phone" placeholder="Phone">
      <input class="form-control mb-2" type="number" [(ngModel)]="profile.age" placeholder="Age">
      <select class="form-select mb-2" [(ngModel)]="profile.gender"><option value="">Gender</option><option>Male</option><option>Female</option><option>Other</option></select>
      <button class="btn btn-primary" (click)="saveProfile()">Save</button></div>
  }
</div>` })
export class PatientComponent implements OnInit {
  private api = inject(Api);
  tabs = ['Book Appointment', 'My Appointments', 'Medical History', 'Notifications', 'Profile']; tab = this.tabs[0];
  depts: any[] = []; doctors: any[] = []; slots: any[] = []; appts: any[] = []; history: any[] = []; queues: any = {};
  deptId: any = null; q = ''; doctor: any; slot: any; reason = ''; error = ''; ok = '';
  fbFor: any; fb: any = { rating: 5, comments: '' }; profile: any = {};
  ngOnInit() {
    this.api.get('/public/departments').subscribe(r => this.depts = r); this.loadDoctors(); this.loadAppts();
    this.api.get('/patient/profile').subscribe(r => this.profile = r);
    this.api.get('/patient/consultations').subscribe(r => this.history = r);
    setInterval(() => this.refreshQueues(), 10000);   // REQ-QUEUE-04: live queue status
  }
  private flash(ok: string, err = '') { this.ok = ok; this.error = err; }
  loadDoctors() { const p: any = {}; if (this.deptId) p.departmentId = this.deptId; if (this.q) p.q = this.q; this.api.get('/public/doctors', p).subscribe(r => this.doctors = r); }
  pick(d: any) { this.doctor = d; this.slot = null; this.api.get(`/public/doctors/${d.id}/slots`).subscribe(r => this.slots = r); }
  book() {
    this.api.post('/patient/appointments', { slotId: this.slot.id, reason: this.reason }).subscribe({
      next: () => { this.flash('Appointment booked. A confirmation notification has been sent.'); this.slot = null; this.reason = ''; this.pick(this.doctor); this.loadAppts(); this.tab = 'My Appointments'; },
      error: e => { this.flash('', msg(e)); this.pick(this.doctor); } });
  }
  loadAppts() { this.api.get('/patient/appointments').subscribe(r => { this.appts = r; this.refreshQueues(); }); }
  refreshQueues() { this.appts.filter(a => a.status === 'BOOKED' || a.status === 'COMPLETED').forEach(a =>
    this.api.get(`/patient/appointments/${a.id}/queue`).subscribe(q => this.queues[a.id] = q)); }
  cancel(a: any) {
    if (!confirm('Cancel this appointment?')) return;
    this.api.post(`/patient/appointments/${a.id}/cancel`).subscribe({ next: () => { this.flash('Appointment cancelled.'); this.loadAppts(); }, error: e => this.flash('', msg(e)) });
  }
  sendFeedback() {
    this.api.post('/patient/feedback', { appointmentId: this.fbFor.id, ...this.fb }).subscribe({ next: () => { this.flash('Thank you for your feedback!'); this.fbFor = null; }, error: e => this.flash('', msg(e)) });
  }
  saveProfile() { this.api.put('/patient/profile', this.profile).subscribe({ next: () => this.flash('Profile updated.'), error: e => this.flash('', msg(e)) }); }
}
