package com.marketatlas.common.web;

import com.marketatlas.trading.service.Caller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tells a logged-in caller who they are, so clients can tailor what they show. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    public record MeResponse(String username, String role) {
    }

    @GetMapping
    public MeResponse me(Authentication authentication) {
        Caller caller = Caller.from(authentication);
        return new MeResponse(caller.username(), caller.admin() ? "ADMIN" : "USER");
    }
}
