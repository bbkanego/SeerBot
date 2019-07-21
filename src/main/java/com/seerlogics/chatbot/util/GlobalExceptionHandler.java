package com.seerlogics.chatbot.util;

import com.lingoace.model.ExceptionModel;
import com.seerlogics.chatbot.exception.ConversationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({ConversationException.class})
    public ResponseEntity<ExceptionModel> handleEntityNotFoundException(ConversationException ex) {
        ExceptionModel exceptionModel = new ExceptionModel();
        exceptionModel.setStackTrace(ex.getMessage());
        return new ResponseEntity<>(exceptionModel, HttpStatus.BAD_REQUEST);
    }
}
