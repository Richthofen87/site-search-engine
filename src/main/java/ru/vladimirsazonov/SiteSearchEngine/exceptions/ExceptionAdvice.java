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

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(RunApplicationException.class)
    public ErrorResponse handleException(RunApplicationException ex) {
        return getResponse(ex);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(IndexPageException.class)
    public ErrorResponse handleException(IndexPageException ex) {
        return getResponse(ex);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({EmptySearchQueryException.class, DisableSiteSearchException.class})
    public ErrorResponse handleException(RuntimeException ex) {
        return getResponse(ex);
    }

    private ErrorResponse getResponse(RuntimeException ex) {
        return new ErrorResponse(ex.getMessage());
    }
}
