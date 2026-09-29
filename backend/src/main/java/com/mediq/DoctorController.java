package com.mediq;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/doctor")
public class DoctorController {
  private final UserRepo users; private final AppointmentRepo appts; private final SlotRepo slots; private final AppointmentService svc;
  private final QueueService queue; private final QueueRepo queueRepo; private final ConsultationRepo consults; private final NotificationService notes;
  DoctorController(UserRepo u, AppointmentRepo a, SlotRepo s, AppointmentService sv, QueueService q, QueueRepo qr, ConsultationRepo c, NotificationService n){ users=u; appts=a; slots=s; svc=sv; queue=q; queueRepo=qr; consults=c; notes=n; }
  private User me(Authentication a){ return users.findByEmail(a.getName()).orElseThrow(); }

  @GetMapping("/profile") User profile(Authentication a){ return me(a); }
  @PutMapping("/profile") User update(Authentication a, @Valid @RequestBody ProfileReq r){ User u = me(a); u.setName(r.name()); u.setPhone(r.phone()); return users.save(u); }
  @GetMapping("/slots") List<Slot> mySlots(Authentication a){ return slots.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(me(a).getId(), LocalDate.now()); }
  @PostMapping("/availability") @ResponseStatus(HttpStatus.CREATED) List<Slot> add(Authentication a, @Valid @RequestBody AvailabilityReq r){ return svc.addAvailability(me(a), r); }
  @DeleteMapping("/slots/{id}") void del(Authentication a, @PathVariable Long id){ svc.deleteSlot(me(a), id); }

  @GetMapping("/appointments") List<Appointment> appointments(Authentication a, @RequestParam(required=false) LocalDate date, @RequestParam(required=false) Appointment.Status status){
    return appts.findByDoctorIdOrderBySlotDateAscSlotStartTimeAsc(me(a).getId()).stream()
      .filter(x -> date==null || x.getSlot().getDate().equals(date)).filter(x -> status==null || x.getStatus()==status).toList(); }
  @GetMapping("/queue") List<QueueEntry> queue(Authentication a, @RequestParam(required=false) LocalDate date){ return queue.forDoctor(me(a).getId(), date==null ? LocalDate.now() : date); }
  @PostMapping("/queue/next") QueueEntry next(Authentication a){ return queue.callNext(me(a)); }
  @PutMapping("/queue/{id}/status") QueueEntry status(Authentication a, @PathVariable Long id, @Valid @RequestBody StatusReq r){ return queue.update(id, r.status(), me(a)); }

  /** REQ-CON-01..07: doctor records consultation + prescription; appointment & queue marked completed. */
  @PostMapping("/appointments/{id}/consultation") @Transactional
  Consultation consult(Authentication a, @PathVariable Long id, @Valid @RequestBody ConsultReq r){
    User d = me(a); Appointment ap = appts.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
    if (!ap.getDoctor().getId().equals(d.getId())) throw ApiException.forbidden("Not your appointment");
    if (ap.getStatus()!=Appointment.Status.BOOKED) throw ApiException.bad("Appointment is not eligible for consultation");
    Consultation c = consults.findByAppointmentId(id).orElseGet(Consultation::new);
    c.setAppointment(ap); c.setDiagnosis(r.diagnosis()); c.setNotes(r.notes());
    c.getPrescriptions().clear();
    if (r.prescriptions()!=null) for (RxItem i : r.prescriptions()) {
      if (i.medicine()==null || i.medicine().isBlank()) throw ApiException.bad("Medicine name is required for each prescription line");
      Prescription p = new Prescription(); p.setMedicine(i.medicine()); p.setDosage(i.dosage()); p.setDuration(i.duration()); c.getPrescriptions().add(p); }
    consults.save(c);
    ap.setStatus(Appointment.Status.COMPLETED); appts.save(ap);
    queueRepo.findByAppointmentId(id).ifPresent(q -> { q.setStatus(QueueEntry.Status.COMPLETED); q.setUpdatedAt(java.time.LocalDateTime.now()); queueRepo.save(q); });
    notes.notify(ap.getPatient(), "Your consultation with Dr. " + d.getName() + " is complete. Prescription and notes are available in your history.");
    return c; }
  @GetMapping("/patients/{patientId}/history") List<Consultation> history(Authentication a, @PathVariable Long patientId){
    User d = me(a);
    boolean treated = appts.findByDoctorIdOrderBySlotDateAscSlotStartTimeAsc(d.getId()).stream().anyMatch(x -> x.getPatient().getId().equals(patientId));
    if (!treated) throw ApiException.forbidden("No appointment with this patient");
    return consults.findByAppointmentPatientIdOrderByCreatedAtDesc(patientId); }
}
