/*
 * (c) Copyright 2020 sothawo
 */
package com.sothawo.dbstations;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@RestController
@RequestMapping("/rest")
public class Resti {

    @GetMapping
    public String hello() {
        return "hello";
    }
}
