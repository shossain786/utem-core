package com.utem.utem_core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API routes to index.html so React Router can handle them.
 */
@Controller
public class SpaController {

    @GetMapping(value = "/{path:[^\\.]*}")
    public String spaRoot() {
        return "forward:/index.html";
    }

    @GetMapping(value = {
        "/runs/{id}",
        "/runs/{id}/compare/{compareId}",
        "/jobs/{jobName}",
        "/jobs/{jobName}/{id}"
    })
    public String spaNested() {
        return "forward:/index.html";
    }
}
