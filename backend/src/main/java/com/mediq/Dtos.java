package com.mediq;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
record RegisterReq(@NotBlank String name, @Email @NotBlank String email, @Size(min=6) String password, String phone, Integer age, String gender) {}
record LoginReq(@NotBlank String email, @NotBlank String password) {}
record AuthRes(String token, Long id, String name, String email, String role) {}
record BookReq(@NotNull Long slotId, String reason) {}
record AvailabilityReq(@NotNull LocalDate date, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @Min(5) @Max(120) int slotMinutes) {}
record StatusReq(@NotNull QueueEntry.Status status) {}
record RxItem(String medicine, String dosage, String duration) {}
record ConsultReq(@NotBlank String diagnosis, String notes, List<RxItem> prescriptions) {}
record FeedbackReq(@NotNull Long appointmentId, @Min(1) @Max(5) int rating, String comments) {}
record DoctorReq(@NotBlank String name, @Email @NotBlank String email, String password, String phone, Long departmentId, String specialization, Integer experienceYears) {}
record PatientReq(@NotBlank String name, String phone, Integer age, String gender) {}
record DeptReq(@NotBlank String name, String description) {}
record ProfileReq(@NotBlank String name, String phone, Integer age, String gender) {}
