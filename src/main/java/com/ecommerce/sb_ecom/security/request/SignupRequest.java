package com.ecommerce.sb_ecom.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest
{
    // use whichever validation you want
    @NotBlank
    private String username;


    @NotBlank
    @Email
    private String email;


    private Set<String> role;

    @NotBlank
    private String password;
}

