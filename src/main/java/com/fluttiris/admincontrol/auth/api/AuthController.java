package com.fluttiris.admincontrol.auth.api;

import com.fluttiris.admincontrol.auth.api.dto.LoginRequest;
import com.fluttiris.admincontrol.auth.api.dto.LoginResponse;
import com.fluttiris.admincontrol.auth.application.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authenticationService.login(request.username(), request.password());
        return new LoginResponse(result.accessToken(), result.expiresInSeconds());
    }
}
