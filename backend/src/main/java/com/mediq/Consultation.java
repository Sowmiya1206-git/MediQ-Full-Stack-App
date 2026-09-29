package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
@Data @Entity
public class Consultation {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @OneToOne(optional=false) private Appointment appointment;
  @Column(length=2000) private String diagnosis; @Column(length=4000) private String notes;
  @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true, fetch=FetchType.EAGER) @JoinColumn(name="consultation_id")
  private List<Prescription> prescriptions = new ArrayList<>();
  private LocalDateTime createdAt = LocalDateTime.now();
}
