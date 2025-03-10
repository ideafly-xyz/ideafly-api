package com.ideafly.controller;
import com.ideafly.aop.anno.NoAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/alive")
public class AliveController {

    @NoAuth
    @GetMapping()
    public String alive() {
        return "ok";
    }
}
