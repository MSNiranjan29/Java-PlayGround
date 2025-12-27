package com.JavaPlayground.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Explicitly forward to the static HTML file
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        // Explicitly forward to the login page
        return "forward:/login.html";
    }
}
