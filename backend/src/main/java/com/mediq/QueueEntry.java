package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
import java.time.*;
@Data @Entity
public class QueueEntry {
  public enum Status { WAITING, CALLED, IN_PROGRESS, COMPLETED, SKIPPED, CANCELLED }
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @OneToOne(optional=false) private Appointment appointment;
  @ManyToOne(optional=false) private User doctor;
  private LocalDate queueDate; private String token;
  @Enumerated(EnumType.STRING) private Status status = Status.WAITING;
  private LocalDateTime updatedAt = LocalDateTime.now();
  @Transient private Integer position; @Transient private Integer estimatedWaitMinutes;
}
