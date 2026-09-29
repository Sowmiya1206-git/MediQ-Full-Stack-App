package com.mediq;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.*;
interface UserRepo extends JpaRepository<User,Long> {
  Optional<User> findByEmail(String email); boolean existsByEmail(String email);
  List<User> findByRoleAndActiveTrue(User.Role role);
  List<User> findByRoleAndActiveTrueAndDepartmentId(User.Role role, Long deptId);
}
interface DepartmentRepo extends JpaRepository<Department,Long> { boolean existsByName(String n); }
interface SlotRepo extends JpaRepository<Slot,Long> {
  List<Slot> findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(Long doctorId, LocalDate from);
  List<Slot> findByDoctorIdAndDate(Long doctorId, LocalDate date);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from Slot s where s.id=:id") Optional<Slot> lockById(Long id);
}
interface AppointmentRepo extends JpaRepository<Appointment,Long> {
  List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long id);
  List<Appointment> findByDoctorIdOrderBySlotDateAscSlotStartTimeAsc(Long id);
  List<Appointment> findBySlotDateBetween(LocalDate a, LocalDate b);
}
interface QueueRepo extends JpaRepository<QueueEntry,Long> {
  Optional<QueueEntry> findByAppointmentId(Long id);
  List<QueueEntry> findByDoctorIdAndQueueDateOrderByAppointmentSlotStartTimeAsc(Long doctorId, LocalDate d);
  List<QueueEntry> findByQueueDateOrderByAppointmentSlotStartTimeAsc(LocalDate d);
  long countByQueueDate(LocalDate d);
}
interface ConsultationRepo extends JpaRepository<Consultation,Long> {
  Optional<Consultation> findByAppointmentId(Long id);
  List<Consultation> findByAppointmentPatientIdOrderByCreatedAtDesc(Long patientId);
}
interface NotificationRepo extends JpaRepository<Notification,Long> { List<Notification> findByUserIdOrderByCreatedAtDesc(Long id); }
interface FeedbackRepo extends JpaRepository<Feedback,Long> { boolean existsByAppointmentId(Long id); List<Feedback> findAllByOrderByCreatedAtDesc(); }
