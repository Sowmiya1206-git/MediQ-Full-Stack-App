package com.mediq;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
  public final HttpStatus status;
  public ApiException(HttpStatus s, String m){ super(m); status=s; }
  public static ApiException bad(String m){ return new ApiException(HttpStatus.BAD_REQUEST,m); }
  public static ApiException notFound(String m){ return new ApiException(HttpStatus.NOT_FOUND,m); }
  public static ApiException conflict(String m){ return new ApiException(HttpStatus.CONFLICT,m); }
  public static ApiException forbidden(String m){ return new ApiException(HttpStatus.FORBIDDEN,m); }
}
