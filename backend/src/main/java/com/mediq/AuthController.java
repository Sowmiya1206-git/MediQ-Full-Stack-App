package com.mediq;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final UserRepo users; private final PasswordEncoder enc; private final JwtUtil jwt;
  AuthController(UserRepo u, PasswordEncoder e, JwtUtil j){ users=u; enc=e; jwt=j; }
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
  AuthRes register(@Valid @RequestBody RegisterReq r){
    String email = r.email().trim().toLowerCase();
    if (users.existsByEmail(email)) throw ApiException.conflict("Email already registered");
    User u = new User(); u.setName(r.name().trim()); u.setEmail(email); u.setPassword(enc.encode(r.password())); u.setPhone(r.phone());
    u.setAge(r.age()); u.setGender(r.gender()); u.setRole(User.Role.PATIENT); users.save(u);
    return res(u);
  }
  @PostMapping("/login")
  AuthRes login(@Valid @RequestBody LoginReq r){
    User u = users.findByEmail(r.email().trim().toLowerCase()).filter(User::isActive)
      .filter(x -> enc.matches(r.password(), x.getPassword())).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    return res(u);
  }
  private AuthRes res(User u){ return new AuthRes(jwt.generate(u), u.getId(), u.getName(), u.getEmail(), u.getRole().name()); }
}
