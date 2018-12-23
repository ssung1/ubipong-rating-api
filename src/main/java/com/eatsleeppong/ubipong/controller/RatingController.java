package com.eatsleeppong.ubipong.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/rating")
public class RatingController {
    @RequestMapping("/test")
    public String test() {
        return "this is my rating";
    }
}