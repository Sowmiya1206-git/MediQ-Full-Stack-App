package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Entity
public class Feedback {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @OneToOne(optional=false) private Appointment appointment;
  @ManyToOne(optional=false) private User patient;
  @ManyToOne(optional=false) private User doctor;
  private int rating; @Column(length=1000) private String comments;
  private LocalDateTime createdAt = LocalDateTime.now();
}
