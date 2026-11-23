package com.ecommerce.sb_ecom.exceptions;

public class ResourceNotFoundException extends  RuntimeException
{

    String rsourceName;
    String field;
    String fieldName;
    Long fieldId;


    public ResourceNotFoundException() {
    }

    public  ResourceNotFoundException(String rsourceName , String field, Long fieldId)
    {
        super(String.format("%s not found with %s: %d" , rsourceName,field,fieldId));

        this.rsourceName= rsourceName;
        this.field=field;
        this.fieldId=fieldId;
    }
          public  ResourceNotFoundException(String rsourceName ,String field,String fieldName )
          {
              super(String.format("%s not found with %s: %s" , rsourceName,field,fieldName));
              this.rsourceName= rsourceName;
              this.field=field;
              this.fieldName=fieldName;
          }


}
