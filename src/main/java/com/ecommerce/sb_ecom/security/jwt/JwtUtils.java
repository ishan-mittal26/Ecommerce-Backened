package com.ecommerce.sb_ecom.security.jwt;

import com.ecommerce.sb_ecom.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtils
{

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirtationMs;

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;


    @Value("${spring.app.jwtCookieName}")
    private String jwtCookie;

    public String getJwtFromCookies(HttpServletRequest request)
    {
        Cookie cookie = WebUtils.getCookie(request,jwtCookie);

        if(cookie!=null)
        {
            return cookie.getValue();
        }
        else {
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal)
    {
        // here we have .secure(false) as when we were working in frontened the cookie was not setting up in browser so this is first step and second step is in frontened in api file we add userCredentials:true property for it working
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        ResponseCookie cookie = ResponseCookie.from(jwtCookie,jwt)
                .path("/api").maxAge(24*60*60).httpOnly(false).secure(false).build();

        return cookie;
    }

    // this is for signout
    public ResponseCookie getCleanJwtCookie()
    {

        ResponseCookie cookie = ResponseCookie.from(jwtCookie,null)
                .path("/api").build();

        return cookie;
    }

    public String generateTokenFromUsername(String userName )
    {
                Date now  =new Date();
        Date expiry = new Date(now.getTime()+jwtExpirtationMs);
        return Jwts.builder()
                .subject(userName)
                .issuedAt(new Date())
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public Key key()
    {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }


        public String getUsernameFromJwtToken(String token)
    {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }


        public boolean validateJwtToken(String authToken)
    {
        try
        {
            System.out.println("validate");
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);

            return true;
        }catch(MalformedJwtException exception)
        {
            logger.error("invalid jwt token :{}", exception.getMessage());
        }
        catch(ExpiredJwtException exception)
        {
            logger.error("invalid jwt expired :{}", exception.getMessage());
        }
        catch(UnsupportedJwtException exception)
        {
            logger.error("invalid jwt unsupported :{}", exception.getMessage());
        }
        catch(IllegalArgumentException exception)
        {
            logger.error("jwt claims string is mpty :{}", exception.getMessage());
        }
        return false;
    }


    // ***** this whole implementation is for getting jwt from authorization bearer from header
    //getting jwt from header
//   public  String getJwtFromHeader(HttpServletRequest request)
//   {
//       String bearerToken = request.getHeader("Authorization");
//       logger.debug("Authorization header : {}",bearerToken);
//       if(bearerToken!=null  && bearerToken.startsWith("Bearer "))
//       {
//           return bearerToken.substring(7);
//       }
//
//       return null;
//   }
//
//   //generating token from username
//    public String generateTokenFromUsername(UserDetails userdetails)
//    {
//        logger.info("current time" + new Date());
//
//        String userName = userdetails.getUsername();
//
//        Date now  =new Date();
//        Date expiry = new Date(now.getTime()+jwtExpirtationMs);
////        logger.info("Current Time : {}", now);
////        logger.info("Expiry Time  : {}", expiry);
//        return Jwts.builder()
//                .subject(userName)
//                .issuedAt(new Date())
//                .expiration(expiry)
//                .signWith(key())
//                .compact();
//    }
//
//
//    //getting username from jwt token
//    public String getUsernameFromJwtToken(String token)
//    {
//        return Jwts.parser()
//                .verifyWith((SecretKey) key())
//                .build().parseSignedClaims(token)
//                .getPayload().getSubject();
//    }
//
//    public Key key()
//    {
//        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
//    }
//
//    //validate jwttoken
//
//    public boolean validateJwtToken(String authToken)
//    {
//        try
//        {
//            System.out.println("validate");
//            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
//
//            return true;
//        }catch(MalformedJwtException exception)
//        {
//            logger.error("invalid jwt token :{}", exception.getMessage());
//        }
//        catch(ExpiredJwtException exception)
//        {
//            logger.error("invalid jwt expired :{}", exception.getMessage());
//        }
//        catch(UnsupportedJwtException exception)
//        {
//            logger.error("invalid jwt unsupported :{}", exception.getMessage());
//        }
//        catch(IllegalArgumentException exception)
//        {
//            logger.error("jwt claims string is mpty :{}", exception.getMessage());
//        }
//        return false;
//    }


}
