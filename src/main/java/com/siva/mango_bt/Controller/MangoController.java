package com.siva.mango_bt.Controller;

import com.siva.mango_bt.Config.JwtUtil;
import com.siva.mango_bt.Entity.Mango;
import com.siva.mango_bt.Entity.Seller;
import com.siva.mango_bt.Repos.MangoRepository;
import com.siva.mango_bt.Repos.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mangoes")
public class MangoController {

    @Autowired
    private MangoRepository mangoRepository;

    // FIX Bug 4/5: inject repos needed for seller resolution
    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String UPLOAD_DIR = "uploads/mangoes/";

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns true if the caller holds the SUPER_ADMIN role. */
    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN"));
    }

    /**
     * Resolves the Seller entity from the JWT principal (mobile = subject).
     * Returns null if the caller is a SUPER_ADMIN (they don't own any store).
     */
    private Seller resolveSellerFromAuth(Authentication auth) {
        if (auth == null || isSuperAdmin(auth)) return null;
        String mobile = auth.getName(); // subject = mobile for sellers
        return sellerRepository.findByMobile(mobile).orElse(null);
    }

    // ── PUBLIC: Get all mangoes ───────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Mango>> getAllMangoes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            // FIX Bug 5: accept optional sellerId to filter by seller
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "false") boolean adminView) {

        List<Mango> mangoes;

        if (adminView) {
            // Admin/Seller view — all mangoes (available + unavailable)
            if (sellerId != null) {
                // FIX Bug 5: filter by seller when sellerId provided
                mangoes = mangoRepository.findBySellerId(sellerId);
            } else if (category != null && !category.isEmpty()) {
                mangoes = mangoRepository.findByCategory(category);
            } else {
                mangoes = mangoRepository.findAll();
            }
        } else if (search != null && !search.isEmpty()) {
            mangoes = mangoRepository.findByNameContainingIgnoreCaseAndIsAvailableTrue(search);
        } else if (category != null && !category.isEmpty()) {
            mangoes = mangoRepository.findByCategoryAndIsAvailableTrue(category);
        } else {
            mangoes = mangoRepository.findByIsAvailableTrue();
        }

        return ResponseEntity.ok(mangoes);
    }

    // ── PUBLIC: Get single mango ──────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Mango> getMango(@PathVariable Long id) {
        return mangoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── PUBLIC: Get categories ────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(mangoRepository.findAllCategories());
    }

    // ── SELLER / ADMIN: Create mango ──────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createMango(
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("unit") String unit,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "weightPerUnit", required = false) String weightPerUnit,
            @RequestParam(value = "isAvailable", defaultValue = "true") Boolean isAvailable,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication auth) {

        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                imageUrl = saveImage(image);
            }

            // FIX Bug 4: resolve seller from JWT and set on mango
            Seller seller = resolveSellerFromAuth(auth);

            Mango mango = Mango.builder()
                    .name(name)
                    .category(category)
                    .description(description)
                    .price(price)
                    .stock(stock)
                    .unit(unit)
                    .origin(origin)
                    .weightPerUnit(weightPerUnit)
                    .isAvailable(isAvailable)
                    .imageUrl(imageUrl)
                    .seller(seller)   // null for super admin — that's fine
                    .build();

            return ResponseEntity.ok(mangoRepository.save(mango));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── SELLER / ADMIN: Update mango ──────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMango(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("unit") String unit,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "weightPerUnit", required = false) String weightPerUnit,
            @RequestParam(value = "isAvailable", defaultValue = "true") Boolean isAvailable,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication auth) {

        return mangoRepository.findById(id).map(mango -> {
            // FIX Bug 4: ownership check — seller can only update their own mangoes
            if (!isSuperAdmin(auth)) {
                Seller seller = resolveSellerFromAuth(auth);
                if (seller == null || mango.getSeller() == null ||
                        !mango.getSeller().getId().equals(seller.getId())) {
                    return ResponseEntity.status(403)
                            .<Object>body(Map.of("message", "You do not own this mango"));
                }
            }

            try {
                mango.setName(name);
                mango.setCategory(category);
                mango.setDescription(description);
                mango.setPrice(price);
                mango.setStock(stock);
                mango.setUnit(unit);
                mango.setOrigin(origin);
                mango.setWeightPerUnit(weightPerUnit);
                mango.setIsAvailable(isAvailable);

                if (image != null && !image.isEmpty()) {
                    mango.setImageUrl(saveImage(image));
                }

                return ResponseEntity.<Object>ok(mangoRepository.save(mango));
            } catch (Exception e) {
                return ResponseEntity.badRequest().<Object>body(Map.of("message", "Error: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── SELLER / ADMIN: Delete mango ──────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMango(@PathVariable Long id, Authentication auth) {
        return mangoRepository.findById(id).map(mango -> {
            // FIX Bug 4: ownership check
            if (!isSuperAdmin(auth)) {
                Seller seller = resolveSellerFromAuth(auth);
                if (seller == null || mango.getSeller() == null ||
                        !mango.getSeller().getId().equals(seller.getId())) {
                    return ResponseEntity.status(403)
                            .<Object>body(Map.of("message", "You do not own this mango"));
                }
            }
            mangoRepository.deleteById(id);
            return ResponseEntity.<Object>ok(Map.of("message", "Mango deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── SELLER / ADMIN: Toggle availability ──────────────────────────────────

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleAvailability(@PathVariable Long id, Authentication auth) {
        return mangoRepository.findById(id).map(mango -> {
            // FIX Bug 4: ownership check
            if (!isSuperAdmin(auth)) {
                Seller seller = resolveSellerFromAuth(auth);
                if (seller == null || mango.getSeller() == null ||
                        !mango.getSeller().getId().equals(seller.getId())) {
                    return ResponseEntity.status(403)
                            .<Object>body(Map.of("message", "You do not own this mango"));
                }
            }
            mango.setIsAvailable(!mango.getIsAvailable());
            return ResponseEntity.<Object>ok(mangoRepository.save(mango));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Image helper ──────────────────────────────────────────────────────────

    private String saveImage(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String filename = UUID.randomUUID() + "_" +
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.write(Paths.get(UPLOAD_DIR + filename), file.getBytes());
        return "/uploads/mangoes/" + filename;
    }
}