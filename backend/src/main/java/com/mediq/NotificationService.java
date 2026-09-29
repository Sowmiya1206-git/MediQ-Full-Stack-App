package com.mediq;
import org.springframework.stereotype.Service;
@Service
public class NotificationService {
  private final NotificationRepo repo; NotificationService(NotificationRepo r){ repo=r; }
  public void notify(User u, String msg){ Notification n = new Notification(); n.setUser(u); n.setMessage(msg); repo.save(n); }
}
