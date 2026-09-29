package com.mediq;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/patient")
public class PatientController {
  private final UserRepo users; private final AppointmentRepo appts; private final AppointmentService svc; private final QueueService queue;
  private final ConsultationRepo consults; private final FeedbackRepo feedback;
  PatientController(UserRepo u, AppointmentRepo a, AppointmentService s, QueueService q, ConsultationRepo c, FeedbackRepo f){ users=u; appts=a; svc=s; queue=q; consults=c; feedback=f; }
  private User me(Authentication a){ return users.findByEmail(a.getName()).orElseThrow(); }

  @GetMapping("/profile") User profile(Authentication a){ return me(a); }
  @PutMapping("/profile") User update(Authentication a, @Valid @RequestBody ProfileReq r){
    User u = me(a); u.setName(r.name()); u.setPhone(r.phone()); u.setAge(r.age()); u.setGender(r.gender()); return users.save(u); }
  @PostMapping("/appointments") @ResponseStatus(HttpStatus.CREATED) Appointment book(Authentication a, @Valid @RequestBody BookReq r){ return svc.book(me(a), r.slotId(), r.reason()); }
  @GetMapping("/appointments") List<Appointment> mine(Authentication a){ return appts.findByPatientIdOrderByCreatedAtDesc(me(a).getId()); }
  @PostMapping("/appointments/{id}/cancel") Appointment cancel(Authentication a, @PathVariable Long id){ return svc.cancel(id, me(a)); }
  @GetMapping("/appointments/{id}/queue") QueueEntry queue(Authentication a, @PathVariable Long id){
    QueueEntry q = queue.byAppointment(id);
    if (!q.getAppointment().getPatient().getId().equals(me(a).getId())) throw ApiException.forbidden("Not your appointment");
    return queue.decorate(q); }
  @GetMapping("/consultations") List<Consultation> history(Authentication a){ return consults.findByAppointmentPatientIdOrderByCreatedAtDesc(me(a).getId()); }
  @PostMapping("/feedback") @ResponseStatus(HttpStatus.CREATED) Feedback feedback(Authentication a, @Valid @RequestBody FeedbackReq r){
    User p = me(a); Appointment ap = appts.findById(r.appointmentId()).orElseThrow(() -> ApiException.notFound("Appointment not found"));
    if (!ap.getPatient().getId().equals(p.getId())) throw ApiException.forbidden("Not your appointment");
    if (ap.getStatus()!=Appointment.Status.COMPLETED) throw ApiException.bad("Feedback allowed only after consultation");
    if (feedback.existsByAppointmentId(ap.getId())) throw ApiException.conflict("Feedback already submitted");
    Feedback f = new Feedback(); f.setAppointment(ap); f.setPatient(p); f.setDoctor(ap.getDoctor()); f.setRating(r.rating()); f.setComments(r.comments());
    return feedback.save(f); }
}
