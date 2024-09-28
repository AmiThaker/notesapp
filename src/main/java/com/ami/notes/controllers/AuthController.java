package com.ami.notes.controllers;

import com.ami.notes.model.AppRole;
import com.ami.notes.model.Role;
import com.ami.notes.model.User;
import com.ami.notes.repositories.RoleRepository;
import com.ami.notes.repositories.UserRepository;
import com.ami.notes.security.jwt.JwtUtils;
import com.ami.notes.security.request.LoginRequest;
import com.ami.notes.security.request.SignUpRequest;
import com.ami.notes.security.response.LoginResponse;
import com.ami.notes.security.response.MessageResponse;
import com.ami.notes.security.response.UserInfoResponse;
import com.ami.notes.security.services.UserDetailsImpl;
import com.ami.notes.services.TotpService;
import com.ami.notes.services.UserService;
import com.ami.notes.utils.AuthUtil;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private TotpService totpService;

    private static final Logger logger= LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/public/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest){
        Authentication authentication;
        logger.debug("User name : {}",loginRequest.getUsername());
        try{
            authentication=authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),loginRequest.getPassword()));
        }catch(AuthenticationException e){
            Map<String,Object> map=new HashMap<>();
            map.put("message","Bad Credentials");
            map.put("status",false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails=(UserDetailsImpl)authentication.getPrincipal();
        String jwtToken=jwtUtils.generateTokenFromUsername(userDetails);
        List<String> roles=userDetails.getAuthorities().stream()
                .map(item->item.getAuthority())
                .collect(Collectors.toList());
        LoginResponse loginResponse=new LoginResponse(jwtToken, userDetails.getUsername(), roles);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/public/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest){
        if(userRepository.existsByUserName(signUpRequest.getUsername())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error : Username already exists!"));
        }
        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error : Email already exists!"));
        }

        User user=new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        
        Set<String> roles=signUpRequest.getRoles();
        Role role;

        if(roles==null || roles.isEmpty()){
            role=roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(()->new RuntimeException("Error : Role is not found!"));
        }else{
            String roleStr=roles.iterator().next();
            if(roleStr.equals("admin")){
                role=roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                        .orElseThrow(()->new RuntimeException("Error : Role is not found!"));
            }else{
                role=roleRepository.findByRoleName(AppRole.ROLE_USER)
                        .orElseThrow(()->new RuntimeException("Error : Role is not found!"));
            }

            user.setAccountNonLocked(true);
            user.setAccountNonExpired(true);
            user.setCredentialsNonExpired(true);
            user.setEnabled(true);
            user.setCredentialsExpiryDate(LocalDate.now().plusYears(1));
            user.setAccountExpiryDate(LocalDate.now().plusYears(1));
            user.setTwoFactorEnabled(false);
            user.setSignUpMethod("email");
        }
        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.ok().body("User has been registered successfully!");
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetails userDetails){
        User user=userService.findByUserName(userDetails.getUsername());

        List<String> roles=userDetails.getAuthorities().stream()
                .map(item->item.getAuthority())
                .toList();

        UserInfoResponse userInfoResponse=new UserInfoResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isTwoFactorEnabled(),
                user.getCredentialsExpiryDate(),
                user.getAccountExpiryDate(),
                user.isTwoFactorEnabled(),
                roles
        );

        return ResponseEntity.ok().body(userInfoResponse);
    }

    @GetMapping("/username")
    public String getUserName(@AuthenticationPrincipal UserDetails userDetails){
        return userDetails!=null?userDetails.getUsername():"";
    }

    @PostMapping("/public/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email){
        try {
            userService.generatePasswordResetToken(email);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new MessageResponse("Password Reset Email sent successfully!"));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error sending password reset email!"));
        }
    }

    @PostMapping("/public/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token,
                                           @RequestParam String newPassword){
        try{
            userService.resetPassword(token,newPassword);
            return ResponseEntity.ok(new MessageResponse("Password reset successful!"));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/enable-2fa")
    public ResponseEntity<String> enable2FA(){
        Long userId= authUtil.loggedInUserId();
        GoogleAuthenticatorKey secret=userService.generate2FASecret(userId);
        String qrCodeUrl= totpService.getQrCodeUrl(secret, authUtil.loggedInUserName());
        return ResponseEntity.ok(qrCodeUrl);
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<String> disable2FA(){
        Long userId= authUtil.loggedInUserId();
        userService.disable2FA(userId);
        return ResponseEntity.ok("2FA disabled");
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<String> verify2FA(@RequestParam int code){
        Long userId= authUtil.loggedInUserId();
        boolean isValid=userService.validate2FACode(userId,code);
        if(isValid){
            userService.enable2FA(userId);
            return ResponseEntity.ok("2FA Verified");
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid 2FA code");
        }
    }

    @GetMapping("/user/2fa-status")
    public ResponseEntity<?> get2FAStatus(){
        User user= authUtil.loggedInUser();
        if(user!=null){
            return ResponseEntity.ok().body(Map.of("is2faEnabled",user.isTwoFactorEnabled()));
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found!");
        }
    }

    @PostMapping("/public/verify-2fa-login")
    public ResponseEntity<String> verify2FALogin(@RequestParam int code,
                                                 @RequestParam String jwtToken){
        String username=jwtUtils.getUsernameFromToken(jwtToken);
        User user=userService.findByUserName(username);
        boolean isValid=userService.validate2FACode(user.getUserId(),code);
        if(isValid){
            userService.enable2FA(user.getUserId());
            return ResponseEntity.ok("2FA Verified");
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid 2FA code");
        }
    }
}
