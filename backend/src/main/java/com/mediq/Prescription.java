package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
@Data @Entity
public class Prescription {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String medicine; private String dosage; private String duration;
}
