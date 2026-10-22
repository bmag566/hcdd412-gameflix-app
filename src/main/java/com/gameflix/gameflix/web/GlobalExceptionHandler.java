package com.gameflix.gameflix.web;

import com.gameflix.gameflix.auth.DisplayNameTakenException;
import com.gameflix.gameflix.auth.EmailAlreadyRegisteredException;
import com.gameflix.gameflix.auth.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return Map.of("error", "Invalid input", "fields", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> unreadable() {
        return Map.of("error", "Malformed request body");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicate() {
        return Map.of("error", "Email already registered");
    }

    @ExceptionHandler(DisplayNameTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> displayNameTaken() {
        return Map.of("error", "Display name already taken");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> badLogin() {
        return Map.of("error", "Invalid email or password");
    }
}
