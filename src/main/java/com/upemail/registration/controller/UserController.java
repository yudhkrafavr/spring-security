package com.upemail.registration.controller;

import com.upemail.registration.entity.AuthenticationRequest;
import com.upemail.registration.entity.RegisterRequest;
import com.upemail.registration.entity.Result;
import com.upemail.registration.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Result> register(@RequestBody RegisterRequest registerRequest) {
        return userService.createUser(registerRequest);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Result> login(@RequestBody AuthenticationRequest auth) {
        return userService.userLogin(auth);
    }

    @GetMapping("/confirm")
    public ResponseEntity<Result> confirmAccount(@RequestParam("token") String token) {
        return userService.confirmTokenRegistration(token);
    }

}
