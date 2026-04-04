package com.siva.mango_bt.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mangoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mango {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private String unit; // kg, dozen, piece

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "weight_per_unit")
    private String weightPerUnit;

    @Column(name = "origin")
    private String origin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
