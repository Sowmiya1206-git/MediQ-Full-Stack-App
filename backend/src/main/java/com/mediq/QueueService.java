package com.mediq;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.mediq.QueueEntry.Status.*;
@Service
public class QueueService {
  private final QueueRepo queue; private final AppointmentRepo appts; private final NotificationService notes;
  QueueService(QueueRepo q, AppointmentRepo a, NotificationService n){ queue=q; appts=a; notes=n; }

  /** Create queue entry + unique token when an appointment is booked (REQ-QUEUE-01/02). */
  QueueEntry create(Appointment a){
    QueueEntry q = new QueueEntry(); q.setAppointment(a); q.setDoctor(a.getDoctor()); q.setQueueDate(a.getSlot().getDate());
    long n = queue.countByQueueDate(a.getSlot().getDate()) + 1;
    q.setToken("Q" + a.getSlot().getDate().toString().replace("-","").substring(4) + "-" + String.format("%03d", n));
    return queue.save(q);
  }
  /** Fill position and estimated wait (REQ-QUEUE-03/08). Position counts active entries ahead for the same doctor/day. */
  public QueueEntry decorate(QueueEntry q){
    List<QueueEntry> list = queue.findByDoctorIdAndQueueDateOrderByAppointmentSlotStartTimeAsc(q.getDoctor().getId(), q.getQueueDate());
    int ahead = 0;
    for (QueueEntry e : list) { if (e.getId().equals(q.getId())) break; if (e.getStatus()==WAITING || e.getStatus()==CALLED || e.getStatus()==IN_PROGRESS) ahead++; }
    boolean active = q.getStatus()==WAITING || q.getStatus()==CALLED || q.getStatus()==IN_PROGRESS;
    Slot s = q.getAppointment().getSlot();
    int mins = Math.max(5, (int) Duration.between(s.getStartTime(), s.getEndTime()).toMinutes());
    q.setPosition(active ? ahead + 1 : null); q.setEstimatedWaitMinutes(q.getStatus()==WAITING ? ahead * mins : null);
    return q;
  }
  public List<QueueEntry> forDoctor(Long doctorId, LocalDate d){ return queue.findByDoctorIdAndQueueDateOrderByAppointmentSlotStartTimeAsc(doctorId,d).stream().map(this::decorate).toList(); }
  public List<QueueEntry> forDate(LocalDate d){ return queue.findByQueueDateOrderByAppointmentSlotStartTimeAsc(d).stream().map(this::decorate).toList(); }

  @Transactional public QueueEntry callNext(User doctor){
    QueueEntry next = queue.findByDoctorIdAndQueueDateOrderByAppointmentSlotStartTimeAsc(doctor.getId(), LocalDate.now()).stream()
      .filter(e -> e.getStatus()==WAITING).findFirst().orElseThrow(() -> ApiException.notFound("No patients waiting"));
    return set(next, CALLED);
  }
  @Transactional public QueueEntry update(Long id, QueueEntry.Status st, User actor){
    QueueEntry q = queue.findById(id).orElseThrow(() -> ApiException.notFound("Queue entry not found"));
    if (actor.getRole()==User.Role.DOCTOR && !q.getDoctor().getId().equals(actor.getId())) throw ApiException.forbidden("Not your queue");
    QueueEntry.Status cur = q.getStatus();
    boolean ok = switch (st) {
      case CALLED, SKIPPED -> cur==WAITING;
      case IN_PROGRESS -> cur==CALLED || cur==WAITING;
      case COMPLETED -> cur==IN_PROGRESS;
      case CANCELLED -> cur==WAITING || cur==CALLED;
      case WAITING -> cur==SKIPPED;
    };
    if (!ok) throw ApiException.bad("Invalid transition " + cur + " -> " + st);
    return set(q, st);
  }
  QueueEntry set(QueueEntry q, QueueEntry.Status st){
    q.setStatus(st); q.setUpdatedAt(LocalDateTime.now()); queue.save(q);
    User p = q.getAppointment().getPatient();
    if (st==CALLED) notes.notify(p, "Token " + q.getToken() + ": it is your turn. Please meet Dr. " + q.getDoctor().getName() + ".");
    if (st==SKIPPED) notes.notify(p, "Token " + q.getToken() + " was skipped. Please contact the front desk.");
    if (st==WAITING) notes.notify(p, "Token " + q.getToken() + " is back in the queue.");
    return decorate(q);
  }
  QueueEntry byAppointment(Long apptId){ return queue.findByAppointmentId(apptId).orElseThrow(() -> ApiException.notFound("Queue entry not found")); }
}
