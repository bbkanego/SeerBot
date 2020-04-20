package com.seerlogics.chatbot.exception;

import com.lingoace.model.ExceptionModel;
import com.seerlogics.commons.exception.BaseRuntimeException;
import com.seerlogics.commons.exception.UIDisplayException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler extends com.lingoace.common.GlobalExceptionHandler {

    private final MessageSource messageSource;

    @Autowired
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler({ConversationException.class})
    public ResponseEntity<ExceptionModel> handleEntityNotFoundException(ConversationException ex) {
        ExceptionModel exceptionModel = new ExceptionModel();
        exceptionModel.setStackTrace(ex.getMessage());
        return new ResponseEntity<>(exceptionModel, HttpStatus.BAD_REQUEST);
    }

    // define the catch all here!!!
    @Override
    @ExceptionHandler(Exception.class)
    public Object resolveException(HttpServletRequest httpServletRequest, Exception e) {
        Map<String, String> errorResponse = this.convertException(e);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (errorResponse.keySet().contains("errorCode")) {
            // this is a known error
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(convertException(e), httpStatus);
    }

    private Map<String, String> buildKnownErrorCodeAndMessageResponse(String errorCode,
                                                                      String errorMessage, Exception e) {
        Map<String, String> errorResponse = new HashMap<>();
        String errorId = UUID.randomUUID().toString();
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("errorMessage", errorMessage);
        errorResponse.put("referenceCode", errorId);

        LOGGER.error("Known error occurred. ReferenceId = " + errorId +
                "\n=================================================\n"
                + ExceptionUtils.getStackTrace(e) +
                "\n=================================================\n");

        return errorResponse;
    }

    private Map<String, String> buildUnknownErrorMessageResponse(Exception e) {
        Map<String, String> errorResponse = new HashMap<>();
        String errorId = UUID.randomUUID().toString();
        errorResponse.put("errorMessage", messageSource.getMessage("error_500_message", new Object[]{errorId}, null));
        errorResponse.put("referenceCode", errorId);

        LOGGER.error("Unknown error occurred. ReferenceId = " + errorId +
                "\n=================================================\n"
                + ExceptionUtils.getStackTrace(e) +
                "\n=================================================\n");

        return errorResponse;
    }

    private Map<String, String> convertException(Exception e) {
        if (e instanceof BaseRuntimeException) {
            return buildKnownErrorCodeAndMessageResponse(((BaseRuntimeException) e).getErrorCode(),
                    "This will get message drom DB", e);
        } else {
            return buildUnknownErrorMessageResponse(e);
        }
    }

    @ExceptionHandler(UIDisplayException.class)
    public Object catchUIDisplayException(UIDisplayException e) {
        this.buildUnknownErrorMessageResponse(e);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put(MESSAGE, e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
