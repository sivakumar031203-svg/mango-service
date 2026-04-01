package com.siva.mango_bt.Controller;

import com.siva.mango_bt.Entity.Mango;
import com.siva.mango_bt.Repos.MangoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
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

    private static final String UPLOAD_DIR = "uploads/mangoes/";

    // PUBLIC: Get all available mangoes
    @GetMapping
    public ResponseEntity<List<Mango>> getAllMangoes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean adminView) {

        List<Mango> mangoes;
        if (adminView) {
            mangoes = (category != null && !category.isEmpty())
                    ? mangoRepository.findByCategory(category)
                    : mangoRepository.findAll();
        } else if (search != null && !search.isEmpty()) {
            mangoes = mangoRepository.findByNameContainingIgnoreCaseAndIsAvailableTrue(search);
        } else if (category != null && !category.isEmpty()) {
            mangoes = mangoRepository.findByCategoryAndIsAvailableTrue(category);
        } else {
            mangoes = mangoRepository.findByIsAvailableTrue();
        }
        return ResponseEntity.ok(mangoes);
    }

    // PUBLIC: Get single mango
    @GetMapping("/{id}")
    public ResponseEntity<Mango> getMango(@PathVariable Long id) {
        return mangoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUBLIC: Get categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(mangoRepository.findAllCategories());
    }

    // ADMIN: Create mango with image
    @PostMapping
    public ResponseEntity<?> createMango(
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("unit") String unit,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "weightPerUnit", required = false) String weightPerUnit,
            @RequestParam(value = "isAvailable", defaultValue = "true") Boolean isAvailable,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                imageUrl = saveImage(image);
            }

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
                    .build();

            return ResponseEntity.ok(mangoRepository.save(mango));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ADMIN: Update mango
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMango(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam("unit") String unit,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "weightPerUnit", required = false) String weightPerUnit,
            @RequestParam(value = "isAvailable", defaultValue = "true") Boolean isAvailable,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        return mangoRepository.findById(id).map(mango -> {
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
                    String imageUrl = saveImage(image);
                    mango.setImageUrl(imageUrl);
                }

                return ResponseEntity.ok(mangoRepository.save(mango));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // ADMIN: Delete mango
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMango(@PathVariable Long id) {
        if (!mangoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mangoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Mango deleted successfully"));
    }

    // ADMIN: Toggle availability
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleAvailability(@PathVariable Long id) {
        return mangoRepository.findById(id).map(mango -> {
            mango.setIsAvailable(!mango.getIsAvailable());
            return ResponseEntity.ok(mangoRepository.save(mango));
        }).orElse(ResponseEntity.notFound().build());
    }

    private String saveImage(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, file.getBytes());
        return "/uploads/mangoes/" + filename;
    }
}
