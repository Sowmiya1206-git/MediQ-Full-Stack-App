package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Entity
public class Notification {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional=false) private User user;
  private String message; private boolean seen; private LocalDateTime createdAt = LocalDateTime.now();
}
