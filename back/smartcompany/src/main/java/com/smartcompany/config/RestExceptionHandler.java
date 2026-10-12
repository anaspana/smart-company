package com.smartcompany.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smartcompany.dto.error.ErrorDto;
import com.smartcompany.exception.AppException;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(value = { AppException.class })
    @ResponseBody
    public ResponseEntity<ErrorDto> handleException(AppException ex) {
    	System.out.println(ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorDto> handleValidationException(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                                .getFieldErrors()
                                .get(0)
                                .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(new ErrorDto(errorMessage));
    }
}