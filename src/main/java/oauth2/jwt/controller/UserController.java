package oauth2.jwt.controller;

import oauth2.jwt.dto.UserInfoDto;
import oauth2.jwt.dto.UserInfo;
import oauth2.jwt.service.JwtService;
import oauth2.jwt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/add")
    public String addUser(@RequestBody UserInfo userInfo) {
        return userService.addUser(userInfo);
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestBody UserInfoDto userInfoDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userInfoDto.getUsername(), userInfoDto.getPassword()));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(userInfoDto.getUsername());
        } else {
            throw new UsernameNotFoundException("invalid user request !");
        }
    }

}