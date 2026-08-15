package com.example.amazon.controller;

import com.example.amazon.dto.request.CardRequest;
import com.example.amazon.exception.SmbcPaymentException;
import com.example.amazon.security.UserPrincipal;
import com.example.amazon.service.SmbcClient;
import com.example.amazon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class CardController {

    private final SmbcClient smbcClient;
    private final UserService userService;

    @GetMapping("/account/card")
    public String cardPage(Model model) {
        model.addAttribute("cardRequest", new CardRequest());
        return "card";
    }

    @PostMapping("/account/card")
    public String registerCard(@Valid @ModelAttribute CardRequest cardRequest, BindingResult result,
                               @AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (result.hasErrors()) {
            return "card";
        }

        try {
            String token = smbcClient.issueToken(
                    cardRequest.getCardNumber(),
                    cardRequest.getExpiryMonth(),
                    cardRequest.getExpiryYear(),
                    cardRequest.getCvv());
            userService.registerCardToken(principal.getUserId(), token);
        } catch (SmbcPaymentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "card";
        }

        return "redirect:/products?cardRegistered";
    }
}