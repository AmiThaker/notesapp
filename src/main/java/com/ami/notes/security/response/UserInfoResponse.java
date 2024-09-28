package com.ami.notes.security.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UserInfoResponse {

    private Long id;
    private String userName;
    private String email;
    private boolean accountNonLocked;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;
    private boolean enabled;
    private LocalDate credentialsExpiryDate;
    private LocalDate accountExpiryDate;
    private boolean isTwoFactorEnabled;
    private List<String> roles;

    public UserInfoResponse(Long id, String userName, String email, boolean accountNonLocked,
                            boolean accountNonExpired, boolean credentialsNonExpired, boolean enabled,
                            LocalDate credentialsExpiryDate, LocalDate accountExpiryDate, boolean isTwoFactorEnabled,
                            List<String> roles) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.accountNonLocked = accountNonLocked;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.credentialsExpiryDate = credentialsExpiryDate;
        this.accountExpiryDate = accountExpiryDate;
        this.isTwoFactorEnabled = isTwoFactorEnabled;
        this.roles=roles;
    }
}
