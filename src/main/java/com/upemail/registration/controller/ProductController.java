package com.upemail.registration.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
@AllArgsConstructor
public class ProductController {

    @GetMapping("/all")
    public String getAllProducts() {
        return "This is all product";
    }

    @GetMapping("/purchased")
    public String getPurchasedProducts() {
        return "This is purchased user product";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/hide")
    public String getHiddenItems() {return "This item is hidden and admin access only";}
}
