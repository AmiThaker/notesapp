package com.ami.notes.config;

import com.ami.notes.model.AppRole;
import com.ami.notes.model.Role;
import com.ami.notes.model.User;
import com.ami.notes.repositories.RoleRepository;
import com.ami.notes.security.jwt.JwtUtils;
import com.ami.notes.security.services.UserDetailsImpl;
import com.ami.notes.services.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger logger= LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Autowired
    private final UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    RoleRepository roleRepository;

    @Value("${frontend.url}")
    private String frontendUrl;
    
    String username;
    String idAttributeKey;

    public OAuth2LoginSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        OAuth2AuthenticationToken oAuth2AuthenticationToken=(OAuth2AuthenticationToken) authentication;
        if("github".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())
        || "google".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())){
            DefaultOAuth2User principal=(DefaultOAuth2User) authentication.getPrincipal();
            Map<String,Object> attributes=principal.getAttributes();
            String email=attributes.getOrDefault("email","").toString();
            String name=attributes.getOrDefault("name","").toString();
            if("github".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())){
                username=attributes.getOrDefault("login","").toString();
                idAttributeKey="id";
            }else if("google".equals(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId())){
                username=email.split("@")[0];
                idAttributeKey="sub";
            }else{
                username="";
                idAttributeKey="id";
            }

            System.out.println("Hello OAuth :  "+email+", Name : "+name);

            userService.findByEmail(email)
                    .ifPresentOrElse(user->{
                       DefaultOAuth2User oAuth2User=new DefaultOAuth2User(
                               List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                               attributes,
                               idAttributeKey
                       );
                       Authentication securityAuth=new OAuth2AuthenticationToken(
                         oAuth2User,
                         List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name())),
                         oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
                       );
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    },()->{
                        User newUser=new User();
                        Optional<Role> userRole=roleRepository.findByRoleName(AppRole.ROLE_USER);
                        if(userRole.isPresent()){
                            newUser.setRole(userRole.get());
                        }else{
                            throw new RuntimeException("Default Role Not Found!");
                        }
                        newUser.setEmail(email);
                        newUser.setUserName(username);
                        newUser.setPassword("ami");
                        newUser.setSignUpMethod(oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
                        userService.registerUser(newUser);
                        DefaultOAuth2User oAuth2User=new DefaultOAuth2User(
                          List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                                attributes,
                                idAttributeKey
                        );
                        Authentication securityAuth=new OAuth2AuthenticationToken(
                          oAuth2User,
                          List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleName().name())),
                                oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
                        );
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    });
        }
        this.setAlwaysUseDefaultTargetUrl(true);

        DefaultOAuth2User oAuth2User=(DefaultOAuth2User) authentication.getPrincipal();
        Map<String,Object> attributes=oAuth2User.getAttributes();

        String email=(String)attributes.getOrDefault("email","").toString();
        System.out.println("OAuth2LoginSuccessHandler : "+username+" : "+email);

        Set<SimpleGrantedAuthority> authorities=new HashSet<>(oAuth2User.getAuthorities().stream()
                .map(authority->new SimpleGrantedAuthority(authority.getAuthority()))
                .collect(Collectors.toList()));

        User user=userService.findByEmail(email)
                        .orElseThrow(()->new RuntimeException("User not found!"));
        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName().name()));

        UserDetailsImpl userDetails=new UserDetailsImpl(
                null,
                username,
                email,
                "ami",
                false,
                authorities
        );

        String jwtToken=jwtUtils.generateTokenFromUsername(userDetails);

        String targetUrl= UriComponentsBuilder.fromUriString(frontendUrl+"/oauth2/redirect")
                .queryParam("token",jwtToken)
                .build().toString();
        this.setDefaultTargetUrl(targetUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
