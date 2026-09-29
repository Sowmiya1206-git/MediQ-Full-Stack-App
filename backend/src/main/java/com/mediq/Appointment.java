package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Entity
public class Appointment {
  public enum Status { BOOKED, CANCELLED, COMPLETED }
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional=false) private User patient;
  @ManyToOne(optional=false) private User doctor;
  @ManyToOne(optional=false) private Slot slot;
  @Enumerated(EnumType.STRING) private Status status = Status.BOOKED;
  private String reason; private LocalDateTime createdAt = LocalDateTime.now();
}
