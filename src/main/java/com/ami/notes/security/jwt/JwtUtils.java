package com.ami.notes.security.jwt;

import com.ami.notes.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${spring.app.jwtSecret}")
    String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    int jwtExpirationMs;

    private static final Logger logger= LoggerFactory.getLogger(JwtUtils.class);

    public String getJwtFromHeader(HttpServletRequest request){
        String token=request.getHeader("Authorization");
        logger.debug("Bearer Token - getJwtFromHeader - Jwtutils: {}",token);
        if(token!=null && token.startsWith("Bearer")){
            return token.substring(7);
        }
        return null;
    }

    public String generateTokenFromUsername(UserDetailsImpl userDetails){
        String username=userDetails.getUsername();
        String roles=userDetails.getAuthorities().stream()
                .map(authority->authority.getAuthority())
                .collect(Collectors.joining(","));
        return Jwts.builder()
                .subject(username)
                .claim("roles",roles)
                .claim("is2faEnabled",userDetails.isIs2faEnabled())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUsernameFromToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey)key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    public Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String jwtToken){
        try{
            System.out.println("Validate!");
                Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build().parseSignedClaims(jwtToken);
                return true;
        }catch(MalformedJwtException e){
            logger.error("Invalid Jwt Token : {}",e.getMessage());
        }catch(UnsupportedJwtException e){
            logger.error("Jwt Token is not supported : {}",e.getMessage());
        }catch(ExpiredJwtException e){
            logger.error("Jwt Token is expired : {}",e.getMessage());
        }catch(IllegalArgumentException e){
            logger.error("Jwt Token is empty : {}",e.getMessage());
        }
        return false;
    }
}
