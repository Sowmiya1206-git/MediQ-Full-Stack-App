import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, msg } from './core';
import { NavbarComponent, NotificationsComponent } from './shared';

@Component({ selector: 'app-doctor', standalone: true, imports: [FormsModule, NavbarComponent, NotificationsComponent], template: `
<app-navbar title="Doctor" />
<div class="container">
  <ul class="nav nav-tabs mb-3">@for (t of tabs; track t) { <li class="nav-item"><a class="nav-link" [class.active]="tab===t" (click)="tab=t">{{t}}</a></li> }</ul>
  @if (error) { <div class="alert alert-danger">{{error}}</div> }
  @if (ok) { <div class="alert alert-success">{{ok}}</div> }

  @if (tab===tabs[0]) {
    <div class="card p-3">
      <div class="d-flex justify-content-between mb-2"><h5>Queue</h5><button class="btn btn-primary btn-sm" (click)="next()">Call next patient</button></div>
      @if (!queue.length) { <p class="text-muted">No queue entries for today.</p> }
      @for (e of queue; track e.id) {
        <div class="border-bottom py-2 d-flex justify-content-between flex-wrap">
          <div><span class="token">{{e.token}}</span> <b>{{e.appointment.patient.name}}</b> <span class="badge bg-info text-dark">{{e.status}}</span>
            <div class="small text-muted">{{e.appointment.slot.startTime.substring(0,5)}} — {{e.appointment.reason}}</div></div>
          <div class="align-self-center">
            @if (e.status==='WAITING') { <button class="btn btn-sm btn-outline-secondary me-1" (click)="setStatus(e,'SKIPPED')">Skip</button> }
            @if (e.status==='WAITING' || e.status==='CALLED') { <button class="btn btn-sm btn-outline-primary" (click)="start(e)">Start consultation</button> }
            @if (e.status==='IN_PROGRESS') { <button class="btn btn-sm btn-success" (click)="openConsult(e.appointment)">Complete &amp; prescribe</button> }
            @if (e.status==='SKIPPED') { <button class="btn btn-sm btn-outline-secondary" (click)="setStatus(e,'WAITING')">Re-queue</button> }
          </div>
        </div>
      }
    </div>
  }

  @if (tab==='Appointments') {
    <div class="card p-3">
      <div class="row g-2 mb-2"><div class="col-auto"><input type="date" class="form-control" [(ngModel)]="fDate" (change)="loadAppts()"></div>
        <div class="col-auto"><select class="form-select" [(ngModel)]="fStatus" (change)="loadAppts()"><option value="">All status</option><option>BOOKED</option><option>COMPLETED</option><option>CANCELLED</option></select></div></div>
      @if (!appts.length) { <p class="text-muted">No appointments found.</p> }
      @for (a of appts; track a.id) {
        <div class="border-bottom py-2 d-flex justify-content-between">
          <div><b>{{a.patient.name}}</b> <span class="badge bg-secondary">{{a.status}}</span><div class="small text-muted">{{a.slot.date}} {{a.slot.startTime.substring(0,5)}} — {{a.reason}}</div></div>
          @if (a.status==='BOOKED') { <button class="btn btn-sm btn-outline-success align-self-center" (click)="openConsult(a)">Add prescription / notes</button> }
        </div>
      }
    </div>
  }

  @if (tab==='Availability') {
    <div class="card p-3">
      <h5>Add availability</h5>
      <div class="row g-2 mb-3">
        <div class="col-md-3"><input type="date" class="form-control" [(ngModel)]="av.date"></div>
        <div class="col-md-2"><input type="time" class="form-control" [(ngModel)]="av.startTime"></div>
        <div class="col-md-2"><input type="time" class="form-control" [(ngModel)]="av.endTime"></div>
        <div class="col-md-2"><input type="number" class="form-control" [(ngModel)]="av.slotMinutes" placeholder="Slot mins"></div>
        <div class="col-md-3"><button class="btn btn-primary w-100" (click)="addAvail()">Save schedule</button></div>
      </div>
      @for (s of slots; track s.id) {
        <span class="badge me-1 mb-1 p-2" [class.bg-success]="!s.booked" [class.bg-secondary]="s.booked">{{s.date}} {{s.startTime.substring(0,5)}}
          @if (!s.booked) { <a class="text-white ms-1" role="button" (click)="delSlot(s)">✕</a> }</span>
      }
    </div>
  }

  @if (tab==='Notifications') { <app-notifications /> }

  @if (consultFor) {
    <div class="card p-3 mt-3 border-primary"><h5>Consultation — {{consultFor.patient.name}}</h5>
      <input class="form-control mb-2" placeholder="Diagnosis *" [(ngModel)]="c.diagnosis">
      <textarea class="form-control mb-2" placeholder="Consultation notes" [(ngModel)]="c.notes"></textarea>
      <h6>Prescription</h6>
      @for (p of c.prescriptions; track $index) {
        <div class="row g-2 mb-1"><div class="col"><input class="form-control" placeholder="Medicine" [(ngModel)]="p.medicine"></div>
          <div class="col"><input class="form-control" placeholder="Dosage" [(ngModel)]="p.dosage"></div>
          <div class="col"><input class="form-control" placeholder="Duration" [(ngModel)]="p.duration"></div></div>
      }
      <div class="mt-2"><button class="btn btn-sm btn-outline-secondary me-2" (click)="c.prescriptions.push({})">+ Medicine</button>
        <button class="btn btn-success btn-sm me-2" (click)="saveConsult()">Save &amp; complete</button><button class="btn btn-link btn-sm" (click)="consultFor=null">Cancel</button></div>
    </div>
  }
</div>` })
export class DoctorComponent implements OnInit {
  private api = inject(Api);
  tabs = ["Today's Queue", 'Appointments', 'Availability', 'Notifications']; tab = this.tabs[0];
  queue: any[] = []; appts: any[] = []; slots: any[] = []; fDate = ''; fStatus = ''; error = ''; ok = '';
  av: any = { date: '', startTime: '09:00', endTime: '13:00', slotMinutes: 20 };
  consultFor: any; c: any = { diagnosis: '', notes: '', prescriptions: [{}] };
  ngOnInit() { this.loadQueue(); this.loadAppts(); this.loadSlots(); setInterval(() => this.loadQueue(), 10000); }
  private flash(ok: string, err = '') { this.ok = ok; this.error = err; }
  loadQueue() { this.api.get('/doctor/queue').subscribe(r => this.queue = r); }
  loadAppts() { const p: any = {}; if (this.fDate) p.date = this.fDate; if (this.fStatus) p.status = this.fStatus; this.api.get('/doctor/appointments', p).subscribe(r => this.appts = r); }
  loadSlots() { this.api.get('/doctor/slots').subscribe(r => this.slots = r); }
  next() { this.api.post('/doctor/queue/next').subscribe({ next: q => { this.flash(`Called ${q.token}`); this.loadQueue(); }, error: e => this.flash('', msg(e)) }); }
  setStatus(e: any, status: string) { this.api.put(`/doctor/queue/${e.id}/status`, { status }).subscribe({ next: () => { this.flash(''); this.loadQueue(); }, error: er => this.flash('', msg(er)) }); }
  start(e: any) { this.api.put(`/doctor/queue/${e.id}/status`, { status: 'IN_PROGRESS' }).subscribe({ next: () => { this.loadQueue(); this.openConsult(e.appointment); }, error: er => this.flash('', msg(er)) }); }
  openConsult(a: any) { this.consultFor = a; this.c = { diagnosis: '', notes: '', prescriptions: [{}] }; }
  saveConsult() {
    if (!this.c.diagnosis) { this.flash('', 'Diagnosis is mandatory'); return; }
    const body = { ...this.c, prescriptions: this.c.prescriptions.filter((p: any) => p.medicine) };
    this.api.post(`/doctor/appointments/${this.consultFor.id}/consultation`, body).subscribe({
      next: () => { this.flash('Consultation saved and marked completed.'); this.consultFor = null; this.loadQueue(); this.loadAppts(); }, error: e => this.flash('', msg(e)) });
  }
  addAvail() {
    if (!this.av.date) { this.flash('', 'Select a date'); return; }
    this.api.post('/doctor/availability', this.av).subscribe({ next: () => { this.flash('Schedule updated successfully.'); this.loadSlots(); }, error: e => this.flash('', msg(e)) });
  }
  delSlot(s: any) { this.api.del(`/doctor/slots/${s.id}`).subscribe({ next: () => this.loadSlots(), error: e => this.flash('', msg(e)) }); }
}
