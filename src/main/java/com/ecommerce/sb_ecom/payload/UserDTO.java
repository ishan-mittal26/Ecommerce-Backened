package com.ecommerce.sb_ecom.payload;


import com.ecommerce.sb_ecom.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

// THIS HAS BEEN MADE WHEN WE HAVE IMPLEMENTED  CONTROLLERS FOR GETTING SELLERS AS SELLER IS ALSO A USER
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private long userId;
    private String username;
    private String email;
    private String password;
    private Set<Role> roles =new HashSet<>();
    private AddressDTO addressDTO;
    private CartDTO cartDTO;
}
