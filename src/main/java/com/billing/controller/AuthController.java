package com.billing.controller;

import com.billing.model.User;
import com.billing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Check if passwords match
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
        }

        // Validate email uniqueness
        if (!userService.isEmailUnique(user.getEmail())) {
            result.rejectValue("email", "error.user", "Email already exists");
        }

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Registration successful! Please login to continue.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", 
                "An error occurred during registration. Please try again.");
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(String email, RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(email);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Password reset instructions have been sent to your email.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error processing your request. Please try again.");
        }
        return "redirect:/login";
    }

    // Access denied handler
    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
