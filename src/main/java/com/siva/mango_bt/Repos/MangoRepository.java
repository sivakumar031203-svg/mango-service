package com.siva.mango_bt.Repos;

import com.siva.mango_bt.Entity.Mango;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MangoRepository extends JpaRepository<Mango, Long> {
    List<Mango> findByIsAvailableTrue();
    List<Mango> findByCategoryAndIsAvailableTrue(String category);
    List<Mango> findByCategory(String category);

    @Query("SELECT DISTINCT m.category FROM Mango m WHERE m.isAvailable = true")
    List<String> findAllActiveCategories();

    @Query("SELECT DISTINCT m.category FROM Mango m")
    List<String> findAllCategories();

    List<Mango> findByNameContainingIgnoreCaseAndIsAvailableTrue(String name);
}
