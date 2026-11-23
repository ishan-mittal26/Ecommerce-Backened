package com.ecommerce.sb_ecom.service;

import com.ecommerce.sb_ecom.model.AppRole;
import com.ecommerce.sb_ecom.model.Role;
import com.ecommerce.sb_ecom.model.User;
import com.ecommerce.sb_ecom.payload.AuthenticationResult;
import com.ecommerce.sb_ecom.payload.UserDTO;
import com.ecommerce.sb_ecom.payload.UserResponse;
import com.ecommerce.sb_ecom.repositories.RoleRepository;
import com.ecommerce.sb_ecom.repositories.UserRepository;
import com.ecommerce.sb_ecom.security.jwt.JwtUtils;
import com.ecommerce.sb_ecom.security.request.LoginRequest;
import com.ecommerce.sb_ecom.security.request.SignupRequest;
import com.ecommerce.sb_ecom.security.response.MessageResponse;
import com.ecommerce.sb_ecom.security.response.UserInfoResponse;
import com.ecommerce.sb_ecom.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService
{
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public AuthenticationResult login(LoginRequest loginRequest) {
        Authentication authentication= authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),loginRequest.getPassword()));


        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails  = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        //String jwtToken = jwtUtils.generateTokenFromUsername(String.valueOf(userDetails));

        List<String> roles = userDetails.getAuthorities().stream().map(item->item.getAuthority()).collect(Collectors.toList());

        //  UserInfoResponse response = new UserInfoResponse(userDetails.getId(),jwtToken,userDetails.getUsername(),roles);
        UserInfoResponse response= new  UserInfoResponse(userDetails.getId(),jwtCookie.toString(),  userDetails.getUsername(),userDetails.getEmail(),roles);

        return new AuthenticationResult(response,jwtCookie);

    }

    @Override
    public ResponseEntity<MessageResponse> register(SignupRequest signupRequest) {
        if(userRepository.existsByUserName(signupRequest.getUsername()))
        {
            return ResponseEntity.badRequest().body(new MessageResponse("error:username is already taken"));
        }

        if(userRepository.existsByEmail(signupRequest.getEmail()))
        {
            return ResponseEntity.badRequest().body(new MessageResponse("error:email is already taken"));
        }


        User user = new User(signupRequest.getEmail(),signupRequest.getUsername(),passwordEncoder.encode(signupRequest.getPassword()));

        Set<String> strRoles = signupRequest.getRole();

        Set<Role> roles = new HashSet<>();

        if(strRoles==null)
        {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow(()-> new RuntimeException("error:role is not found"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role ->
            {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("error:role is not found"));
                        roles.add(adminRole);
                        break;

                    case "seller":
                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow(() -> new RuntimeException("error:role is not found"));
                        roles.add(sellerRole);
                        break;

                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow(() -> new RuntimeException("error:role is not found"));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("user registered successfully"));
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication, HttpServletRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream().map(item->item.getAuthority()).collect(Collectors.toList());

        String jwtToken = jwtUtils.getJwtFromCookies(request);


        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),jwtToken,userDetails.getUsername(),userDetails.getEmail(),roles);


        return response;
    }

    @Override
    public ResponseCookie logoutUser() {
        return jwtUtils.getCleanJwtCookie();
    }

    @Override
    public UserResponse getAllSellers(Pageable pageable) {
        Page<User> allUser= userRepository.findByRoleName(AppRole.ROLE_SELLER,pageable);
        List<UserDTO> userDTOs = allUser.getContent().stream().map(p-> modelMapper.map(p, UserDTO.class) ).toList();

        UserResponse response = new UserResponse();
        response.setContent(userDTOs);
        response.setPageNumber(allUser.getNumber());
        response.setPageSize(allUser.getSize());
        response.setTotalElements(allUser.getTotalElements());
        response.setTotalPages(allUser.getTotalPages());
        response.setLastPage(allUser.isLast());

        return response;
    }
}
