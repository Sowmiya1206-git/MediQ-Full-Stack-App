package com.mediq;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/public")
public class PublicController {
  private final DepartmentRepo depts; private final UserRepo users; private final SlotRepo slots;
  PublicController(DepartmentRepo d, UserRepo u, SlotRepo s){ depts=d; users=u; slots=s; }
  @GetMapping("/departments") List<Department> departments(){ return depts.findAll(); }
  @GetMapping("/doctors") List<User> doctors(@RequestParam(required=false) Long departmentId, @RequestParam(required=false) String q){
    List<User> l = departmentId==null ? users.findByRoleAndActiveTrue(User.Role.DOCTOR) : users.findByRoleAndActiveTrueAndDepartmentId(User.Role.DOCTOR, departmentId);
    if (q != null && !q.isBlank()) { String s = q.toLowerCase(); l = l.stream().filter(d -> d.getName().toLowerCase().contains(s) || (d.getSpecialization()!=null && d.getSpecialization().toLowerCase().contains(s))).toList(); }
    return l;
  }
  /** REQ-APT-02 / REQ-SCH-04: only future, unbooked slots. */
  @GetMapping("/doctors/{id}/slots") List<Slot> slots(@PathVariable Long id){
    return slots.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(id, LocalDate.now()).stream().filter(s -> !s.isBooked()).toList();
  }
}
