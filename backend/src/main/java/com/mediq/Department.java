package com.mediq;
import jakarta.persistence.*;
import lombok.Data;
@Data @Entity
public class Department {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(unique=true) private String name; private String description;
}
