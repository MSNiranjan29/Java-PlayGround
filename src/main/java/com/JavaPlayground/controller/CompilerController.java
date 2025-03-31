package com.JavaPlayground.controller;

import com.JavaPlayground.model.CodeSubmission;
import com.JavaPlayground.model.CompilationResponse;
import com.JavaPlayground.service.CompilerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompilerController {

    @Autowired
    private CompilerService compilerService;

    @PostMapping("/compile")
    public ResponseEntity<CompilationResponse> compileCode(@RequestBody CodeSubmission submission) {
        CompilationResponse response = compilerService.compileAndExecute(submission.getCode());
        return ResponseEntity.ok(response);
    }
}
