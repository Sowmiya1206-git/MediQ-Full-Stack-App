package com.mediq;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class) ResponseEntity<Map<String,String>> api(ApiException e){ return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage())); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,String>> valid(MethodArgumentNotValidException e){
    String m = e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+" "+f.getDefaultMessage()).findFirst().orElse("Invalid input");
    return ResponseEntity.badRequest().body(Map.of("message",m)); }
  @ExceptionHandler(Exception.class) ResponseEntity<Map<String,String>> other(Exception e){ e.printStackTrace(); return ResponseEntity.status(500).body(Map.of("message","Server error. Please try again.")); }
}
