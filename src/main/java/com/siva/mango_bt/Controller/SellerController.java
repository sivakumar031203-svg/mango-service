package com.siva.mango_bt.Controller;

import com.siva.mango_bt.Entity.Seller;
import com.siva.mango_bt.Repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    @Autowired SellerRepository sellerRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // Default password given to new seller
    private static final String DEFAULT_PASSWORD = "Mango@1234";

    /** SUPER_ADMIN: list all sellers */
    @GetMapping
    public ResponseEntity<List<Seller>> getAll() {
        return ResponseEntity.ok(sellerRepository.findAll());
    }

    /** SUPER_ADMIN: get one seller */
    @GetMapping("/{id}")
    public ResponseEntity<Seller> getOne(@PathVariable Long id) {
        return sellerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** PUBLIC: get seller info for contact page */
    @GetMapping("/{id}/public")
    public ResponseEntity<?> getPublic(@PathVariable Long id) {
        return sellerRepository.findById(id).map(s -> ResponseEntity.ok(Map.of(
                "storeName", s.getStoreName(),
                "ownerName", s.getOwnerName() != null ? s.getOwnerName() : "",
                "mobile", s.getMobile(),
                "whatsapp", s.getWhatsapp() != null ? s.getWhatsapp() : s.getMobile(),
                "email", s.getEmail() != null ? s.getEmail() : "",
                "city", s.getCity() != null ? s.getCity() : "",
                "storeDescription", s.getStoreDescription() != null ? s.getStoreDescription() : ""
        ))).orElse(ResponseEntity.notFound().build());
    }

    /** PUBLIC: list all active sellers (for multi-store home page) */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveSellers() {

        List<Map<String, Object>> sellers = sellerRepository.findAll().stream()
                .filter(Seller::getIsActive)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("storeName", s.getStoreName());
                    m.put("city", s.getCity());
                    m.put("storeDescription", s.getStoreDescription());
                    return m;
                }).toList();

        return ResponseEntity.ok(sellers);
    }

    /** SUPER_ADMIN: create seller (generates default password) */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        if (sellerRepository.existsByMobile(mobile))
            return ResponseEntity.badRequest().body(Map.of("message", "Mobile already registered"));

        Seller seller = Seller.builder()
                .mobile(mobile)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .storeName(body.get("storeName"))
                .ownerName(body.get("ownerName"))
                .email(body.get("email"))
                .whatsapp(body.get("whatsapp"))
                .city(body.get("city"))
                .storeDescription(body.get("storeDescription"))
                .isActive(true)
                .passwordChanged(false)
                .build();

        Seller saved = sellerRepository.save(seller);
        return ResponseEntity.ok(Map.of(
                "message", "Seller created",
                "seller", saved,
                "defaultPassword", DEFAULT_PASSWORD  // shown once to super admin
        ));
    }

    /** SUPER_ADMIN: update seller */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return sellerRepository.findById(id).map(s -> {
            if (body.containsKey("storeName")) s.setStoreName(body.get("storeName"));
            if (body.containsKey("ownerName")) s.setOwnerName(body.get("ownerName"));
            if (body.containsKey("email")) s.setEmail(body.get("email"));
            if (body.containsKey("whatsapp")) s.setWhatsapp(body.get("whatsapp"));
            if (body.containsKey("city")) s.setCity(body.get("city"));
            if (body.containsKey("storeDescription")) s.setStoreDescription(body.get("storeDescription"));
            return ResponseEntity.ok(sellerRepository.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** SUPER_ADMIN: toggle active status */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        return sellerRepository.findById(id).map(s -> {
            s.setIsActive(!s.getIsActive());
            return ResponseEntity.ok(sellerRepository.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** SUPER_ADMIN: reset password to default */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        return sellerRepository.findById(id).map(s -> {
            s.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            s.setPasswordChanged(false);
            sellerRepository.save(s);
            return ResponseEntity.ok(Map.of("message", "Password reset", "defaultPassword", DEFAULT_PASSWORD));
        }).orElse(ResponseEntity.notFound().build());
    }
}