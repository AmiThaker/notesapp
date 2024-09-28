package com.ami.notes.payload;

import com.ami.notes.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    Long userId;
    String userName;
    String email;
    boolean accountNonLocked;
    boolean accountNonExpired;
    boolean credentialsNonExpired;
    boolean enabled;
    LocalDate credentialsExpiryDate;
    String twoFactorSecret;
    boolean twoFactorEnabled;
    String signUpMethod;
    Role role;
    LocalDate createdDate;
    LocalDate updatedDate;

}
