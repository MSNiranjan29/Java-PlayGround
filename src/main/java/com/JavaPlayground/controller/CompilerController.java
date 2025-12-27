package com.JavaPlayground.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.JavaPlayground.model.CompilationResponse;
import com.JavaPlayground.service.CompilerService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompilerController {

    @Autowired
    private CompilerService compilerService;

    @PostMapping("/compile")
    public CompilationResponse compileAndRun(@RequestBody CodeRequest request) {
        return compilerService.compileAndExecute(
                request.getCode(),
                request.getInput()
        );
    }
}
