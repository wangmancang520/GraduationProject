package com.example.springboot.exception;

import com.example.springboot.common.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

/*
* 如果抛出的是ServiceException，则调用该方法
* */

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public org.springframework.http.ResponseEntity<Result> handle(ServiceException serviceException) {
        int status = "401".equals(serviceException.getCode()) ? 401 : "403".equals(serviceException.getCode()) ? 403 : 200;
        return org.springframework.http.ResponseEntity.status(status).body(Result.error(serviceException.getCode(), serviceException.getMessage()));
    }
    @ExceptionHandler({java.text.ParseException.class, IllegalArgumentException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    @ResponseBody
    public Result badRequest(Exception exception) {
        return Result.error("400", "请求参数格式不正确");
    }
}
