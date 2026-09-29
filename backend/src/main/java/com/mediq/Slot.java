package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
import java.time.*;
@Data @Entity
public class Slot {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional=false) private User doctor;
  @Column(name="slot_date") private LocalDate date; private LocalTime startTime; private LocalTime endTime;
  private boolean booked;
}
