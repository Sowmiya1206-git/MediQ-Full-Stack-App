package com.mediq;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
@RestController @RequestMapping("/api/admin")
public class AdminController {
  private final UserRepo users; private final DepartmentRepo depts; private final AppointmentRepo appts; private final AppointmentService svc;
  private final QueueService queue; private final FeedbackRepo feedback; private final QueueRepo queueRepo; private final SlotRepo slots; private final PasswordEncoder enc;
  AdminController(UserRepo u, DepartmentRepo d, AppointmentRepo a, AppointmentService s, QueueService q, FeedbackRepo f, QueueRepo qr, SlotRepo sl, PasswordEncoder e){ users=u; depts=d; appts=a; svc=s; queue=q; feedback=f; queueRepo=qr; slots=sl; enc=e; }

  // ----- Dashboard -----
  @GetMapping("/dashboard") Map<String,Object> dashboard(){
    LocalDate t = LocalDate.now(); var today = appts.findBySlotDateBetween(t,t);
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("doctors", users.findByRoleAndActiveTrue(User.Role.DOCTOR).size()); m.put("patients", users.findByRoleAndActiveTrue(User.Role.PATIENT).size());
    m.put("departments", depts.count()); m.put("appointmentsToday", today.size());
    m.put("waitingNow", queue.forDate(t).stream().filter(q -> q.getStatus()==QueueEntry.Status.WAITING).count());
    return m; }

  // ----- Doctors (REQ-ADM-01) -----
  @GetMapping("/doctors") List<User> doctors(){ return users.findByRoleAndActiveTrue(User.Role.DOCTOR); }
  @PostMapping("/doctors") @ResponseStatus(HttpStatus.CREATED) User addDoctor(@Valid @RequestBody DoctorReq r){
    String email = r.email().trim().toLowerCase();
    if (users.existsByEmail(email)) throw ApiException.conflict("Duplicate doctor ID / email");
    if (r.password()==null || r.password().length()<6) throw ApiException.bad("Password (min 6 chars) is required");
    User u = new User(); u.setRole(User.Role.DOCTOR); u.setEmail(email); u.setPassword(enc.encode(r.password())); return users.save(apply(u,r)); }
  @PutMapping("/doctors/{id}") User editDoctor(@PathVariable Long id, @Valid @RequestBody DoctorReq r){
    User u = users.findById(id).filter(x -> x.getRole()==User.Role.DOCTOR).orElseThrow(() -> ApiException.notFound("Doctor not found"));
    if (r.password()!=null && !r.password().isBlank()) { if (r.password().length()<6) throw ApiException.bad("Password too short"); u.setPassword(enc.encode(r.password())); }
    return users.save(apply(u,r)); }
  private User apply(User u, DoctorReq r){ u.setName(r.name()); u.setPhone(r.phone()); u.setSpecialization(r.specialization()); u.setExperienceYears(r.experienceYears());
    u.setDepartment(r.departmentId()==null ? null : depts.findById(r.departmentId()).orElseThrow(() -> ApiException.bad("Invalid department"))); return u; }
  @DeleteMapping("/doctors/{id}") void delDoctor(@PathVariable Long id){ deactivate(id, User.Role.DOCTOR); }

  // ----- Patients (REQ-ADM-02) -----
  @GetMapping("/patients") List<User> patients(@RequestParam(required=false) String q){
    var l = users.findByRoleAndActiveTrue(User.Role.PATIENT);
    if (q!=null && !q.isBlank()) { String s=q.toLowerCase(); l = l.stream().filter(p -> p.getName().toLowerCase().contains(s) || p.getEmail().contains(s)).toList(); } return l; }
  @PutMapping("/patients/{id}") User editPatient(@PathVariable Long id, @Valid @RequestBody PatientReq r){
    User u = users.findById(id).filter(x -> x.getRole()==User.Role.PATIENT).orElseThrow(() -> ApiException.notFound("Patient record not found"));
    u.setName(r.name()); u.setPhone(r.phone()); u.setAge(r.age()); u.setGender(r.gender()); return users.save(u); }
  @DeleteMapping("/patients/{id}") void delPatient(@PathVariable Long id){ deactivate(id, User.Role.PATIENT); }
  private void deactivate(Long id, User.Role role){
    User u = users.findById(id).filter(x -> x.getRole()==role).orElseThrow(() -> ApiException.notFound("Record not found")); u.setActive(false); users.save(u); }

  // ----- Departments (REQ-ADM-03) -----
  @PostMapping("/departments") @ResponseStatus(HttpStatus.CREATED) Department addDept(@Valid @RequestBody DeptReq r){
    if (depts.existsByName(r.name())) throw ApiException.conflict("Department already exists");
    Department d = new Department(); d.setName(r.name()); d.setDescription(r.description()); return depts.save(d); }
  @PutMapping("/departments/{id}") Department editDept(@PathVariable Long id, @Valid @RequestBody DeptReq r){
    Department d = depts.findById(id).orElseThrow(() -> ApiException.notFound("Department not found")); d.setName(r.name()); d.setDescription(r.description()); return depts.save(d); }
  @DeleteMapping("/departments/{id}") void delDept(@PathVariable Long id){
    if (users.findByRoleAndActiveTrueAndDepartmentId(User.Role.DOCTOR, id).size()>0) throw ApiException.conflict("Department still has doctors assigned");
    depts.deleteById(id); }

  // ----- Appointments / schedules / queue (REQ-ADM-04..06) -----
  @GetMapping("/appointments") List<Appointment> appointments(){ return appts.findAll(); }
  @PostMapping("/appointments/{id}/cancel") Appointment cancel(@PathVariable Long id){ User admin = new User(); admin.setRole(User.Role.ADMIN); return svc.cancel(id, admin); }
  @GetMapping("/schedules") List<Slot> schedules(@RequestParam Long doctorId){ return slots.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(doctorId, LocalDate.now()); }
  @GetMapping("/queue") List<QueueEntry> queue(@RequestParam(required=false) LocalDate date){ return queue.forDate(date==null ? LocalDate.now() : date); }
  @PutMapping("/queue/{id}/status") QueueEntry qs(@PathVariable Long id, @Valid @RequestBody StatusReq r){ User admin = new User(); admin.setRole(User.Role.ADMIN); return queue.update(id, r.status(), admin); }
  @GetMapping("/feedback") List<Feedback> feedback(){ return feedback.findAllByOrderByCreatedAtDesc(); }
  @GetMapping("/users") List<User> allUsers(){ return users.findAll(); }

  // ----- Reports (REQ-ADM-07) -----
  @GetMapping("/reports") Map<String,Object> report(@RequestParam LocalDate from, @RequestParam LocalDate to){
    if (to.isBefore(from)) throw ApiException.bad("Invalid date range selected");
    var list = appts.findBySlotDateBetween(from, to);
    if (list.isEmpty()) throw ApiException.notFound("No data available for the selected range");
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("from", from); m.put("to", to); m.put("totalAppointments", list.size());
    m.put("byStatus", list.stream().collect(Collectors.groupingBy(a -> a.getStatus().name(), TreeMap::new, Collectors.counting())));
    m.put("byDoctor", list.stream().collect(Collectors.groupingBy(a -> a.getDoctor().getName(), TreeMap::new, Collectors.counting())));
    m.put("byDepartment", list.stream().collect(Collectors.groupingBy(a -> a.getDoctor().getDepartment()==null ? "Unassigned" : a.getDoctor().getDepartment().getName(), TreeMap::new, Collectors.counting())));
    m.put("byDay", list.stream().collect(Collectors.groupingBy(a -> a.getSlot().getDate().toString(), TreeMap::new, Collectors.counting())));
    Set<Long> ids = list.stream().map(Appointment::getId).collect(Collectors.toSet());
    m.put("averageRating", feedback.findAll().stream().filter(f -> ids.contains(f.getAppointment().getId())).mapToInt(Feedback::getRating).average().orElse(0));
    long comp = list.stream().filter(a -> a.getStatus()==Appointment.Status.COMPLETED).count();
    m.put("completionRate", Math.round(100.0 * comp / list.size()));
    return m; }
}
