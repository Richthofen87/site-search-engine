package ru.vladimirsazonov.SiteSearchEngine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.vladimirsazonov.SiteSearchEngine.dto.ErrorResponse;

@ControllerAdvice
@ResponseBody
public class ExceptionAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RunApplicationException.class)
    public ErrorResponse handleException(RunApplicationException ex) {
        return getResponse(ex);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ServerStateException.class)
    public ErrorResponse handleException(ServerStateException ex) {
        return getResponse(ex);
    }

    private ErrorResponse getResponse(RuntimeException ex) {
        return new ErrorResponse(ex.getMessage());
    }
}
