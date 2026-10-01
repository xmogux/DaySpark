package com.dayspark.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(Exception ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("title", "요청을 처리할 수 없습니다");
        model.addAttribute("message", friendlyMessage(ex));
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(Exception ex, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("title", "오류가 발생했습니다");
        model.addAttribute("message", "잠시 후 다시 시도하거나 입력값을 확인해주세요.");
        return "error";
    }

    private String friendlyMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "입력값이나 주소가 올바르지 않습니다.";
        if (ex instanceof MethodArgumentTypeMismatchException) return "날짜, 숫자 형식이 올바르지 않습니다.";
        return message;
    }
}
