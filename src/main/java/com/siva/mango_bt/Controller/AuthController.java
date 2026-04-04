package com.siva.mango_bt.Controller;

import com.siva.mango_bt.Config.JwtUtil;
import com.siva.mango_bt.Entity.*;
import com.siva.mango_bt.Repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired AdminUserRepository adminUserRepository;
    @Autowired SellerRepository sellerRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    // ── Super admin login ─────────────────────────────────────────────────────

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> creds) {
        AdminUser admin = adminUserRepository.findByUsername(creds.get("username")).orElse(null);
        if (admin == null || !passwordEncoder.matches(creds.get("password"), admin.getPassword()))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));

        // Token carries ROLE_SUPER_ADMIN so JwtAuthFilter grants that authority
        String token = jwtUtil.generateToken(admin.getUsername(), "ROLE_SUPER_ADMIN", admin.getId());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", "SUPER_ADMIN",
                "username", admin.getUsername()
        ));
    }

    // ── Legacy login (backward compat — same as adminLogin) ──────────────────

    @PostMapping("/login")
    public ResponseEntity<?> legacyLogin(@RequestBody Map<String, String> creds) {
        return adminLogin(creds);
    }

    // ── Seller login ──────────────────────────────────────────────────────────

    @PostMapping("/seller/login")
    public ResponseEntity<?> sellerLogin(@RequestBody Map<String, String> creds) {
        Seller seller = sellerRepository.findByMobile(creds.get("mobile")).orElse(null);
        if (seller == null || !passwordEncoder.matches(creds.get("password"), seller.getPassword()))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        if (!seller.getIsActive())
            return ResponseEntity.status(403).body(Map.of("message", "Account is deactivated. Contact super admin."));

        // Token carries ROLE_SELLER
        String token = jwtUtil.generateToken(seller.getMobile(), "ROLE_SELLER", seller.getId());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", "SELLER",
                "sellerId", seller.getId(),
                "storeName", seller.getStoreName(),
                "ownerName", seller.getOwnerName() != null ? seller.getOwnerName() : "",
                "passwordChanged", seller.getPasswordChanged()
        ));
    }

    // ── Customer register ─────────────────────────────────────────────────────

    @PostMapping("/customer/register")
    public ResponseEntity<?> customerRegister(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        if (mobile == null || !mobile.matches("[6-9]\\d{9}"))
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid mobile number"));
        if (customerRepository.existsByMobile(mobile))
            return ResponseEntity.badRequest().body(Map.of("message", "Mobile already registered"));

        String password = body.get("password");
        if (password == null || password.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));

        Customer c = Customer.builder()
                .mobile(mobile)
                .password(passwordEncoder.encode(password))
                .name(body.get("name"))
                .email(body.get("email"))
                .isActive(true)
                .build();
        Customer saved = customerRepository.save(c);
        String token = jwtUtil.generateToken(mobile, "ROLE_CUSTOMER", saved.getId());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", "CUSTOMER",
                "customerId", saved.getId(),
                "name", saved.getName() != null ? saved.getName() : ""
        ));
    }

    // ── Customer login ────────────────────────────────────────────────────────

    @PostMapping("/customer/login")
    public ResponseEntity<?> customerLogin(@RequestBody Map<String, String> body) {
        Customer c = customerRepository.findByMobile(body.get("mobile")).orElse(null);
        if (c == null || !passwordEncoder.matches(body.get("password"), c.getPassword()))
            return ResponseEntity.status(401).body(Map.of("message", "Invalid mobile or password"));
        if (Boolean.FALSE.equals(c.getIsActive()))
            return ResponseEntity.status(403).body(Map.of("message", "Account is deactivated"));

        String token = jwtUtil.generateToken(c.getMobile(), "ROLE_CUSTOMER", c.getId());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", "CUSTOMER",
                "customerId", c.getId(),
                "name", c.getName() != null ? c.getName() : ""
        ));
    }

    // ── Seller change password ────────────────────────────────────────────────

    /**
     * FIX Bug 8: verifies currentPassword before allowing the change.
     * The endpoint is protected by SecurityConfig (ROLE_SELLER only).
     * The seller identity comes from the JWT subject (mobile).
     */
    @PostMapping("/seller/change-password")
    public ResponseEntity<?> sellerChangePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        String token = authHeader.replace("Bearer ", "").trim();
        String mobile = jwtUtil.extractUsername(token);

        Seller seller = sellerRepository.findByMobile(mobile).orElse(null);
        if (seller == null) return ResponseEntity.notFound().build();

        // FIX Bug 8: validate the current password before accepting the new one
        String currentPassword = body.get("currentPassword");
        if (currentPassword == null || currentPassword.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is required"));
        if (!passwordEncoder.matches(currentPassword, seller.getPassword()))
            return ResponseEntity.status(401).body(Map.of("message", "Current password is incorrect"));

        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 8 characters"));

        seller.setPassword(passwordEncoder.encode(newPassword));
        seller.setPasswordChanged(true);
        sellerRepository.save(seller);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ── Token verify ─────────────────────────────────────────────────────────

    @GetMapping("/verify")
    public ResponseEntity<?> verify() {
        return ResponseEntity.ok(Map.of("valid", true));
    }
}