package oauth2.jwt.filter;

import oauth2.jwt.constants.SsoConstant;
import oauth2.jwt.dto.UserInfo;
import oauth2.jwt.dto.UserInfoDetails;
import oauth2.jwt.service.JwtService;
import oauth2.jwt.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oauth2.jwt.utils.CookieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

// This class helps us to validate the generated jwt token
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userDetailsService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(SsoConstant.AUTHORIZATION);
        String token = null;
        String username = null;
        boolean isFromCookie = false;
        if (authHeader == null) {
            authHeader = CookieUtils.getCookie(request, SsoConstant.AUTHORIZATION);
            isFromCookie = true;
        }
        if (authHeader != null && authHeader.startsWith(SsoConstant.BEARER_PREFIX)) {
            token = authHeader.substring(7);
            if (isFromCookie) {
                try {
                    jwtService.isTokenExpired(token);
                    username = jwtService.extractUsername(token);
                } catch (Exception ex) {
                    authHeader = null;
                }
            } else {
                username = jwtService.extractUsername(token);
            }
        }
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String key = SsoConstant.SSO_CONST + username;
            UserInfo userInfo = (UserInfo) redisTemplate.boundValueOps(key).get();
            assert userInfo != null;
            UserInfoDetails userDetails = new UserInfoDetails(userInfo);
            // UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // SSO
                // 1.base on the refreshing interval to generate a new token
                // 2.expire user info in redis
                Date expireDate = jwtService.extractExpiration(token);
                // less than 1 minute
                if (expireDate.getTime() - System.currentTimeMillis() < 60*1000) {
                    token = jwtService.generateToken(username);
                    authHeader = SsoConstant.BEARER_PREFIX + token;
                    redisTemplate.expire(key, SsoConstant.EXPIRE_TIME, TimeUnit.MINUTES);
                }
                response.setHeader(SsoConstant.AUTHORIZATION, authHeader);
                CookieUtils.setCookie(response, SsoConstant.AUTHORIZATION, authHeader, SsoConstant.EXPIRE_TIME*1000*60, null);
            }
        }
        filterChain.doFilter(request, response);
    }
}
