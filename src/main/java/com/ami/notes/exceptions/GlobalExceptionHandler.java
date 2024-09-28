package com.ami.notes.exceptions;

import com.ami.notes.payload.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> methodArgumentNotValidException(MethodArgumentNotValidException e){

       Map<String,String> map=new HashMap<>();

       e.getBindingResult().getAllErrors().forEach(err->{
           String fieldName=((FieldError)err).getField();
           String message=err.getDefaultMessage();
           map.put(fieldName,message);
       });
       return new ResponseEntity<>(map,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> apiException(APIException e){
        String message=e.getMessage();
        APIResponse apiResponse=new APIResponse(message,false);
        return new ResponseEntity<>(apiResponse,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> resourceNotFoundException(ResourceNotFoundException e){
        String message=e.getMessage();
        APIResponse apiResponse=new APIResponse(message,false);
        return new ResponseEntity<>(apiResponse,HttpStatus.NOT_FOUND);
    }

}
