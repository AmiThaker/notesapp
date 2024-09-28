package com.ami.notes.services;

import com.ami.notes.exceptions.ResourceNotFoundException;
import com.ami.notes.model.AppRole;
import com.ami.notes.model.PasswordResetToken;
import com.ami.notes.model.Role;
import com.ami.notes.model.User;
import com.ami.notes.payload.UserDTO;
import com.ami.notes.repositories.PasswordResetTokenRepository;
import com.ami.notes.repositories.RoleRepository;
import com.ami.notes.repositories.UserRepository;
import com.ami.notes.utils.EmailService;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotpService totpService;

    @Override
    public void updateUserRole(Long userId, String roleName) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User","userId",userId));
        AppRole appRole= AppRole.valueOf(roleName);
        Role role=roleRepository.findByRoleName(appRole)
                .orElseThrow(()->new ResourceNotFoundException("Role","roleName", appRole.name()));
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user=userRepository.findById(id).orElseThrow();

        return convertToUserDTO(user);
    }

    @Override
    public User findByUserName(String userName) {
        Optional<User> user=userRepository.findByUserName(userName);
        return user.orElseThrow(()->new RuntimeException("User not found!"));
    }

    private UserDTO convertToUserDTO(User user){
        return new UserDTO(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isEnabled(),
                user.getCredentialsExpiryDate(),
                user.getTwoFactorSecret(),
                user.isTwoFactorEnabled(),
                user.getSignUpMethod(),
                user.getRole(),
                user.getCreatedDate(),
                user.getUpdatedDate()
        );
    }

    @Override
    public void generatePasswordResetToken(String email){
        User user=userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("User not found!"));
        String token= UUID.randomUUID().toString();
        Instant expiryDate=Instant.now().plus(24, ChronoUnit.HOURS);
        PasswordResetToken resetToken=new PasswordResetToken(token,expiryDate,user);
        passwordResetTokenRepository.save(resetToken);

        String resetUrl=frontendUrl+"/reset-password?token="+token;
        emailService.sendPasswordResetEmail(user.getEmail(),resetUrl);

    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken passwordResetToken=passwordResetTokenRepository.findByToken(token)
                .orElseThrow(()->new RuntimeException("Invalid Password Reset Token!"));

        if(passwordResetToken.isUsed()){
            throw new RuntimeException("Password Reset Token has already been used!");
        }
        if(passwordResetToken.getExpiryDate().isBefore(Instant.now())){
            throw new RuntimeException("Password Reset Token has expired!");
        }
        User user=passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetToken.setUsed(true);
        passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        Optional<User> user=userRepository.findByEmail(email);
        return user;
    }

    @Override
    public User registerUser(User user) {
        if(user.getPassword()!=null)
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public GoogleAuthenticatorKey generate2FASecret(Long userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found!"));
        GoogleAuthenticatorKey key= totpService.generateSecret();
        user.setTwoFactorSecret(key.getKey());
        userRepository.save(user);
        return key;
    }

    @Override
    public boolean validate2FACode(Long userId, int code){
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found!"));
        return totpService.verifyCode(user.getTwoFactorSecret(),code);
    }

    @Override
    public void enable2FA(Long userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found!"));
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void disable2FA(Long userId){
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found!"));
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
    }
}
