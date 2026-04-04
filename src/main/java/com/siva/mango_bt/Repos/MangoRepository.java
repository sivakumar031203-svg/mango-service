package com.siva.mango_bt.Repos;

import com.siva.mango_bt.Entity.Mango;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MangoRepository extends JpaRepository<Mango, Long> {

    // Public storefront — available only
    List<Mango> findByIsAvailableTrue();
    List<Mango> findByCategoryAndIsAvailableTrue(String category);
    List<Mango> findByNameContainingIgnoreCaseAndIsAvailableTrue(String name);

    // Admin view — all regardless of availability
    List<Mango> findByCategory(String category);

    // FIX Bug 5: seller-scoped queries
    List<Mango> findBySeller_Id(Long sellerId);

    // Convenience alias used in MangoController
    default List<Mango> findBySellerId(Long sellerId) {
        return findBySeller_Id(sellerId);
    }

    @Query("SELECT DISTINCT m.category FROM Mango m WHERE m.isAvailable = true")
    List<String> findAllActiveCategories();

    @Query("SELECT DISTINCT m.category FROM Mango m")
    List<String> findAllCategories();
}