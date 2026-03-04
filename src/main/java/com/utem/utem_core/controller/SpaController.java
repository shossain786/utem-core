package com.utem.utem_core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API routes to index.html so React Router can handle them.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/runs/**",
        "/jobs/**",
        "/archive",
        "/search",
        "/trends",
        "/compare/**"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
