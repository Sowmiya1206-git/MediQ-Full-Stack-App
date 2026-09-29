import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api, msg } from './core';
import { NavbarComponent } from './shared';

@Component({ selector: 'app-admin', standalone: true, imports: [FormsModule, NavbarComponent], template: `
<app-navbar title="Administrator" />
<div class="container">
  <ul class="nav nav-tabs mb-3">@for (t of tabs; track t) { <li class="nav-item"><a class="nav-link" [class.active]="tab===t" (click)="tab=t; loadTab()">{{t}}</a></li> }</ul>
  @if (error) { <div class="alert alert-danger">{{error}}</div> }
  @if (ok) { <div class="alert alert-success">{{ok}}</div> }

  @if (tab==='Dashboard') {
    <div class="row g-3">
      @for (kv of dashKeys(); track kv) { <div class="col-md-2 col-6"><div class="card p-3 text-center"><div class="fs-4">{{dash[kv]}}</div><div class="small text-muted">{{kv}}</div></div></div> }
    </div>
  }

  @if (tab==='Doctors') {
    <div class="card p-3">
      <h6>Add / Edit doctor</h6>
      <div class="row g-2 mb-3">
        <div class="col-md-3"><input class="form-control" placeholder="Name" [(ngModel)]="d.name"></div>
        <div class="col-md-3"><input class="form-control" placeholder="Email" [(ngModel)]="d.email"></div>
        <div class="col-md-2"><input class="form-control" placeholder="Password" [(ngModel)]="d.password"></div>
        <div class="col-md-2"><select class="form-select" [(ngModel)]="d.departmentId"><option [ngValue]="null">Dept</option>@for (dp of depts; track dp.id) {<option [ngValue]="dp.id">{{dp.name}}</option>}</select></div>
        <div class="col-md-2"><input class="form-control" placeholder="Specialization" [(ngModel)]="d.specialization"></div>
      </div>
      <button class="btn btn-primary btn-sm mb-3" (click)="saveDoctor()">{{d.id?'Update':'Add'}} doctor</button>
      @for (x of doctors; track x.id) { <div class="border-bottom py-2 d-flex justify-content-between">
        <div><b>{{x.name}}</b> — {{x.specialization}} ({{x.department?.name}})<div class="small text-muted">{{x.email}}</div></div>
        <div><button class="btn btn-sm btn-outline-secondary me-1" (click)="editDoctor(x)">Edit</button><button class="btn btn-sm btn-outline-danger" (click)="delDoctor(x)">Remove</button></div></div> }
    </div>
  }

  @if (tab==='Patients') {
    <div class="card p-3">
      <input class="form-control mb-3" placeholder="Search patients" [(ngModel)]="pq" (input)="loadPatients()">
      @for (x of patients; track x.id) { <div class="border-bottom py-2 d-flex justify-content-between">
        <div><b>{{x.name}}</b><div class="small text-muted">{{x.email}} · {{x.phone}}</div></div>
        <button class="btn btn-sm btn-outline-danger" (click)="delPatient(x)">Remove</button></div> }
    </div>
  }

  @if (tab==='Departments') {
    <div class="card p-3">
      <div class="row g-2 mb-3"><div class="col-md-4"><input class="form-control" placeholder="Name" [(ngModel)]="dep.name"></div>
        <div class="col-md-5"><input class="form-control" placeholder="Description" [(ngModel)]="dep.description"></div>
        <div class="col-md-3"><button class="btn btn-primary w-100" (click)="saveDept()">Add department</button></div></div>
      @for (x of depts; track x.id) { <div class="border-bottom py-2 d-flex justify-content-between"><div><b>{{x.name}}</b> — {{x.description}}</div>
        <button class="btn btn-sm btn-outline-danger" (click)="delDept(x)">Remove</button></div> }
    </div>
  }

  @if (tab==='Appointments') {
    <div class="card p-3">
      @for (a of appts; track a.id) { <div class="border-bottom py-2 d-flex justify-content-between">
        <div>{{a.patient.name}} → {{a.doctor.name}} <span class="badge bg-secondary">{{a.status}}</span><div class="small text-muted">{{a.slot.date}} {{a.slot.startTime.substring(0,5)}}</div></div>
        @if (a.status==='BOOKED') { <button class="btn btn-sm btn-outline-danger" (click)="cancelAppt(a)">Cancel</button> }</div> }
    </div>
  }

  @if (tab==='Queue') {
    <div class="card p-3">
      <input type="date" class="form-control mb-3" style="max-width:220px" [(ngModel)]="qDate" (change)="loadQueue()">
      @for (q of queueList; track q.id) { <div class="border-bottom py-2"><span class="token">{{q.token}}</span> {{q.appointment.patient.name}} → Dr. {{q.doctor.name}} <span class="badge bg-info text-dark">{{q.status}}</span></div> }
    </div>
  }

  @if (tab==='Feedback') {
    <div class="card p-3">
      @for (f of feedback; track f.id) { <div class="border-bottom py-2">⭐ {{f.rating}}/5 — {{f.patient.name}} on Dr. {{f.doctor.name}}<div class="small text-muted">{{f.comments}}</div></div> }
    </div>
  }

  @if (tab==='Reports') {
    <div class="card p-3">
      <div class="row g-2 mb-3"><div class="col-auto"><input type="date" class="form-control" [(ngModel)]="rFrom"></div>
        <div class="col-auto"><input type="date" class="form-control" [(ngModel)]="rTo"></div>
        <div class="col-auto"><button class="btn btn-primary" (click)="loadReport()">Generate report</button></div></div>
      @if (report) {
        <p>Total: <b>{{report.totalAppointments}}</b> · Completion rate: <b>{{report.completionRate}}%</b> · Avg rating: <b>{{report.averageRating | number:'1.1-1'}}</b></p>
        <div class="row"><div class="col-md-4"><h6>By status</h6>@for (k of keys(report.byStatus); track k){<div>{{k}}: {{report.byStatus[k]}}</div>}</div>
          <div class="col-md-4"><h6>By doctor</h6>@for (k of keys(report.byDoctor); track k){<div>{{k}}: {{report.byDoctor[k]}}</div>}</div>
          <div class="col-md-4"><h6>By department</h6>@for (k of keys(report.byDepartment); track k){<div>{{k}}: {{report.byDepartment[k]}}</div>}</div></div>
      }
    </div>
  }
</div>` })
export class AdminComponent implements OnInit {
  private api = inject(Api);
  tabs = ['Dashboard', 'Doctors', 'Patients', 'Departments', 'Appointments', 'Queue', 'Feedback', 'Reports']; tab = this.tabs[0];
  dash: any = {}; doctors: any[] = []; patients: any[] = []; depts: any[] = []; appts: any[] = []; queueList: any[] = []; feedback: any[] = []; report: any;
  d: any = {}; dep: any = {}; pq = ''; qDate = new Date().toISOString().substring(0, 10); rFrom = ''; rTo = ''; error = ''; ok = '';
  ngOnInit() { this.api.get('/public/departments').subscribe(r => this.depts = r); this.loadTab(); }
  keys(o: any) { return Object.keys(o || {}); }
  dashKeys() { return Object.keys(this.dash); }
  private flash(ok: string, err = '') { this.ok = ok; this.error = err; }
  loadTab() {
    if (this.tab === 'Dashboard') this.api.get('/admin/dashboard').subscribe(r => this.dash = r);
    if (this.tab === 'Doctors') this.api.get('/admin/doctors').subscribe(r => this.doctors = r);
    if (this.tab === 'Patients') this.loadPatients();
    if (this.tab === 'Departments') this.api.get('/public/departments').subscribe(r => this.depts = r);
    if (this.tab === 'Appointments') this.api.get('/admin/appointments').subscribe(r => this.appts = r);
    if (this.tab === 'Queue') this.loadQueue();
    if (this.tab === 'Feedback') this.api.get('/admin/feedback').subscribe(r => this.feedback = r);
  }
  loadPatients() { this.api.get('/admin/patients', this.pq ? { q: this.pq } : {}).subscribe(r => this.patients = r); }
  loadQueue() { this.api.get('/admin/queue', { date: this.qDate }).subscribe(r => this.queueList = r); }
  saveDoctor() {
    const call = this.d.id ? this.api.put(`/admin/doctors/${this.d.id}`, this.d) : this.api.post('/admin/doctors', this.d);
    call.subscribe({ next: () => { this.flash(this.d.id ? 'Doctor updated.' : 'Doctor added.'); this.d = {}; this.loadTab(); }, error: e => this.flash('', msg(e)) });
  }
  editDoctor(x: any) { this.d = { ...x, departmentId: x.department?.id, password: '' }; }
  delDoctor(x: any) { if (confirm('Remove this doctor?')) this.api.del(`/admin/doctors/${x.id}`).subscribe({ next: () => this.loadTab(), error: e => this.flash('', msg(e)) }); }
  delPatient(x: any) { if (confirm('Remove this patient?')) this.api.del(`/admin/patients/${x.id}`).subscribe({ next: () => this.loadTab(), error: e => this.flash('', msg(e)) }); }
  saveDept() { this.api.post('/admin/departments', this.dep).subscribe({ next: () => { this.dep = {}; this.flash('Department added.'); this.ngOnInit(); }, error: e => this.flash('', msg(e)) }); }
  delDept(x: any) { if (confirm('Remove this department?')) this.api.del(`/admin/departments/${x.id}`).subscribe({ next: () => this.ngOnInit(), error: e => this.flash('', msg(e)) }); }
  cancelAppt(a: any) { if (confirm('Cancel this appointment?')) this.api.post(`/admin/appointments/${a.id}/cancel`).subscribe({ next: () => this.loadTab(), error: e => this.flash('', msg(e)) }); }
  loadReport() { if (!this.rFrom || !this.rTo) { this.flash('', 'Select both dates'); return; } this.api.get('/admin/reports', { from: this.rFrom, to: this.rTo }).subscribe({ next: r => this.report = r, error: e => this.flash('', msg(e)) }); }
}
