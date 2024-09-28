package com.ami.notes.exceptions;

import jakarta.annotation.Resource;

public class ResourceNotFoundException extends RuntimeException{

    private Long fieldId;
    private String fieldName;
    private String field;
    private String resourceName;

    public ResourceNotFoundException(){}

    public ResourceNotFoundException(String resourceName,String field,String fieldName){
        super(String.format("%s with %s : %s not found!",resourceName,fieldName,field));
        this.resourceName=resourceName;
        this.field=field;
        this.fieldName=fieldName;
    }

    public ResourceNotFoundException(String resourceName,String field,Long fieldId){
        super(String.format("%s with %s : %d not found!",resourceName,field,fieldId));
        this.resourceName=resourceName;
        this.field=field;
        this.fieldId=fieldId;

    }
}
