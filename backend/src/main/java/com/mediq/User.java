package com.mediq;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
@Data @Entity @Table(name="users")
public class User {
  public enum Role { ADMIN, DOCTOR, PATIENT }
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String name;
  @Column(unique=true, nullable=false) private String email;
  @JsonIgnore private String password;
  private String phone;
  @Enumerated(EnumType.STRING) private Role role;
  private boolean active = true;
  // patient fields
  private Integer age; private String gender;
  // doctor fields
  @ManyToOne private Department department;
  private String specialization; private Integer experienceYears;
}
