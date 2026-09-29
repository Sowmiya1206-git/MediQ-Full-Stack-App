package com.mediq;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.*;
@Component
public class DataSeeder implements CommandLineRunner {
  private final UserRepo users; private final DepartmentRepo depts; private final AppointmentService svc; private final PasswordEncoder enc;
  DataSeeder(UserRepo u, DepartmentRepo d, AppointmentService s, PasswordEncoder e){ users=u; depts=d; svc=s; enc=e; }
  @Override public void run(String... args){
    if (users.count() > 0) return;
    Department gen = dept("General Medicine","Primary care"), card = dept("Cardiology","Heart care"), ped = dept("Pediatrics","Child care");
    mk("System Admin","admin@mediq.com","admin123",User.Role.ADMIN,null,null);
    User d1 = mk("Dr. Asha Menon","doctor@mediq.com","doctor123",User.Role.DOCTOR,gen,"General Physician");
    User d2 = mk("Dr. Rahul Iyer","rahul@mediq.com","doctor123",User.Role.DOCTOR,card,"Cardiologist");
    mk("Dr. Priya Nair","priya@mediq.com","doctor123",User.Role.DOCTOR,ped,"Pediatrician");
    mk("Demo Patient","patient@mediq.com","patient123",User.Role.PATIENT,null,null);
    LocalDate t = LocalDate.now();
    for (User d : new User[]{d1,d2}) for (int i=0;i<3;i++) svc.addAvailability(d, new AvailabilityReq(t.plusDays(i), LocalTime.of(9,0), LocalTime.of(13,0), 20));
    System.out.println("Seeded demo data: admin@mediq.com/admin123, doctor@mediq.com/doctor123, patient@mediq.com/patient123");
  }
  private Department dept(String n, String d){ Department x = new Department(); x.setName(n); x.setDescription(d); return depts.save(x); }
  private User mk(String n,String e,String p,User.Role r,Department d,String spec){
    User u = new User(); u.setName(n); u.setEmail(e); u.setPassword(enc.encode(p)); u.setRole(r); u.setDepartment(d); u.setSpecialization(spec); if (r==User.Role.DOCTOR) u.setExperienceYears(8); return users.save(u); }
}
