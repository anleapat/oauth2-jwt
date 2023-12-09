package oauth2.jwt.controller;

import jakarta.servlet.http.HttpServletResponse;
import oauth2.jwt.constants.SsoConstant;
import oauth2.jwt.dto.UserInfoDto;
import oauth2.jwt.dto.UserInfo;
import oauth2.jwt.service.JwtService;
import oauth2.jwt.service.UserService;
import oauth2.jwt.utils.CookieUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/add")
    public String addUser(@RequestBody UserInfo userInfo) {
        return userService.addUser(userInfo);
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestBody UserInfoDto userInfoDto, HttpServletResponse response) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userInfoDto.getUsername(), userInfoDto.getPassword()));
        if (authentication.isAuthenticated()) {
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(userInfoDto, userInfo);
            String key = SsoConstant.SSO_CONST + userInfoDto.getUsername();
            redisTemplate.boundValueOps(key).setIfAbsent(userInfo);
            redisTemplate.expire(key, SsoConstant.EXPIRE_TIME, TimeUnit.MINUTES);
            String token = jwtService.generateToken(userInfoDto.getUsername());
            String authHeader = SsoConstant.BEARER_PREFIX + token;
            response.setHeader(SsoConstant.AUTHORIZATION, authHeader);
            CookieUtils.setCookie(response, SsoConstant.AUTHORIZATION, authHeader, SsoConstant.EXPIRE_TIME*1000*60, null);
            return token;
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }
    }
}