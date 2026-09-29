package com.mediq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class AppointmentService {
  private final AppointmentRepo appts; private final SlotRepo slots; private final QueueService queue; private final QueueRepo queueRepo; private final NotificationService notes;
  private final long minHours;
  AppointmentService(AppointmentRepo a, SlotRepo s, QueueService q, QueueRepo qr, NotificationService n, @Value("${app.cancel.min-hours-before}") long h){ appts=a; slots=s; queue=q; queueRepo=qr; notes=n; minHours=h; }

  /** REQ-APT-03/04: pessimistic lock on the slot prevents double booking. */
  @Transactional public Appointment book(User patient, Long slotId, String reason){
    Slot s = slots.lockById(slotId).orElseThrow(() -> ApiException.notFound("Slot not found"));
    if (s.isBooked()) throw ApiException.conflict("Selected time slot is unavailable");
    if (LocalDateTime.of(s.getDate(), s.getStartTime()).isBefore(LocalDateTime.now())) throw ApiException.bad("Cannot book a past slot");
    s.setBooked(true); slots.save(s);
    Appointment a = new Appointment(); a.setPatient(patient); a.setDoctor(s.getDoctor()); a.setSlot(s); a.setReason(reason); a = appts.save(a);
    QueueEntry q = queue.create(a);
    notes.notify(patient, "Appointment confirmed with Dr. " + s.getDoctor().getName() + " on " + s.getDate() + " at " + s.getStartTime() + ". Queue token: " + q.getToken());
    notes.notify(s.getDoctor(), "New appointment: " + patient.getName() + " on " + s.getDate() + " at " + s.getStartTime());
    return a;
  }
  /** REQ-APT-07/08: only BOOKED appointments outside the cut-off window can be cancelled. */
  @Transactional public Appointment cancel(Long id, User actor){
    Appointment a = appts.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
    if (actor.getRole()==User.Role.PATIENT && !a.getPatient().getId().equals(actor.getId())) throw ApiException.forbidden("Not your appointment");
    if (a.getStatus()!=Appointment.Status.BOOKED) throw ApiException.bad("Only booked appointments can be cancelled");
    if (actor.getRole()==User.Role.PATIENT && LocalDateTime.of(a.getSlot().getDate(), a.getSlot().getStartTime()).minusHours(minHours).isBefore(LocalDateTime.now()))
      throw ApiException.bad("Appointments cannot be cancelled within " + minHours + " hour(s) of the start time");
    a.setStatus(Appointment.Status.CANCELLED); appts.save(a);
    Slot s = a.getSlot(); s.setBooked(false); slots.save(s);
    queueRepo.findByAppointmentId(id).ifPresent(q -> { q.setStatus(QueueEntry.Status.CANCELLED); queueRepo.save(q); });
    notes.notify(a.getPatient(), "Your appointment on " + s.getDate() + " at " + s.getStartTime() + " was cancelled.");
    notes.notify(a.getDoctor(), "Appointment cancelled: " + a.getPatient().getName() + " on " + s.getDate() + " at " + s.getStartTime());
    return a;
  }
  /** REQ-SCH-01/03: generate slots; reject overlapping ranges. */
  @Transactional public List<Slot> addAvailability(User doctor, AvailabilityReq r){
    if (r.date().isBefore(LocalDate.now())) throw ApiException.bad("Date cannot be in the past");
    if (!r.endTime().isAfter(r.startTime())) throw ApiException.bad("Invalid time slot: end must be after start");
    List<Slot> existing = slots.findByDoctorIdAndDate(doctor.getId(), r.date());
    List<Slot> created = new ArrayList<>();
    for (LocalTime t = r.startTime(); !t.plusMinutes(r.slotMinutes()).isAfter(r.endTime()); t = t.plusMinutes(r.slotMinutes())) {
      final LocalTime st = t, en = t.plusMinutes(r.slotMinutes());
      if (existing.stream().anyMatch(e -> st.isBefore(e.getEndTime()) && en.isAfter(e.getStartTime()))) throw ApiException.conflict("Schedule conflicts with an existing slot at " + st);
      Slot s = new Slot(); s.setDoctor(doctor); s.setDate(r.date()); s.setStartTime(st); s.setEndTime(en); created.add(s);
      if (t.isAfter(LocalTime.of(23,0))) break;
    }
    if (created.isEmpty()) throw ApiException.bad("Time range is shorter than one slot");
    return slots.saveAll(created);
  }
  public void deleteSlot(User doctor, Long id){
    Slot s = slots.findById(id).orElseThrow(() -> ApiException.notFound("Slot not found"));
    if (!s.getDoctor().getId().equals(doctor.getId())) throw ApiException.forbidden("Not your slot");
    if (s.isBooked()) throw ApiException.conflict("Slot has an appointment; cancel it first");
    slots.delete(s);
  }
}
