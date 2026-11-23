package com.ecommerce.sb_ecom.security.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse
{

   private Long id;
   private String jwtToken;

   private String username;
   private String email;
   private List<String> roles;


   // this constructor was made for cookie setup only,
   public UserInfoResponse(Long id, String username,String email, List<String> roles) {
      this.id = id;
      this.username = username;
      this.email=email;
      this.roles = roles;
   }
}
