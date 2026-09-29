package com.mediq;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationRepo repo; private final UserRepo users;
  NotificationController(NotificationRepo r, UserRepo u){ repo=r; users=u; }
  @GetMapping List<Notification> list(Authentication a){ return repo.findByUserIdOrderByCreatedAtDesc(users.findByEmail(a.getName()).orElseThrow().getId()); }
  @PostMapping("/{id}/read") Notification read(Authentication a, @PathVariable Long id){
    Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
    if (!n.getUser().getEmail().equals(a.getName())) throw ApiException.forbidden("Not your notification");
    n.setSeen(true); return repo.save(n); }
}
