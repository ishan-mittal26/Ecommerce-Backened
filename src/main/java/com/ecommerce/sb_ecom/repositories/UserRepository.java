package com.ecommerce.sb_ecom.repositories;

import com.ecommerce.sb_ecom.model.AppRole;
import com.ecommerce.sb_ecom.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User,Long>
{


    Optional<User> findByUserName(String username);

    boolean existsByUserName(@NotBlank String username);

    boolean existsByEmail(@NotBlank @Email String email);

    @Query("SELECT u from User u JOIN u.roles r WHERE r.roleName= :role")
    Page<User> findByRoleName(@Param("role") AppRole appRole, Pageable pageable);
}
