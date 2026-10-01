package com.dayspark.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps exceptions from the HTML pages to the error page with the right status. Every error is
 * logged; unexpected ones with the stack trace. {@link FocusApi} has its own JSON handlers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BAD_REQUEST_TITLE = "요청을 처리할 수 없습니다";

    /** Domain rule violations carry a message written for the user. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String rejected(IllegalArgumentException ex, Model model) {
        log.warn("Rejected request: {}", ex.getMessage());
        return page(model, HttpStatus.BAD_REQUEST, BAD_REQUEST_TITLE, ex.getMessage());
    }

    /** Wrong number/date format or a missing parameter. Spring's own message is not for users. */
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String malformed(Exception ex, Model model) {
        log.warn("Malformed request: {}", ex.getMessage());
        return page(model, HttpStatus.BAD_REQUEST, BAD_REQUEST_TITLE, "날짜, 숫자 형식이나 필수 입력값을 확인해주세요.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String tooLarge(MaxUploadSizeExceededException ex, Model model) {
        log.warn("Upload too large: {}", ex.getMessage());
        return page(model, HttpStatus.BAD_REQUEST, BAD_REQUEST_TITLE, "사진은 8MB 이하만 올릴 수 있습니다.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NoResourceFoundException ex, Model model) {
        log.debug("Not found: {}", ex.getMessage());
        return page(model, HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다", "주소를 확인하거나 메뉴에서 다시 이동해주세요.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String methodNotAllowed(HttpRequestMethodNotSupportedException ex, Model model) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return page(model, HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청입니다", "화면의 버튼으로 다시 시도해주세요.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        return page(model, HttpStatus.INTERNAL_SERVER_ERROR, "오류가 발생했습니다", "잠시 후 다시 시도하거나 입력값을 확인해주세요.");
    }

    private static String page(Model model, HttpStatus status, String title, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "error";
    }
}
