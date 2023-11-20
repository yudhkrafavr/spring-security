package com.upemail.registration.service;

import com.upemail.registration.entity.AuthenticationRequest;
import com.upemail.registration.entity.RegisterRequest;
import com.upemail.registration.entity.Result;
import org.springframework.http.ResponseEntity;

public interface UserService {

    ResponseEntity<Result> createUser(RegisterRequest request);

    ResponseEntity<Result> userLogin(AuthenticationRequest request);

    ResponseEntity<Result> confirmTokenRegistration(String token);
}
