package com.ami.notes.exceptions;


public class APIException extends RuntimeException{

    private static final Long serialVersionUID=1L;

    private String message;

    public APIException(String message){
        super(message);
    }

}
