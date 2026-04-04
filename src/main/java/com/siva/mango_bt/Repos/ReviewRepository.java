package com.siva.mango_bt.Repos;
import com.siva.mango_bt.Entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMango_IdAndApprovedTrueOrderByCreatedAtDesc(Long mangoId);
    List<Review> findByApprovedFalseOrderByCreatedAtDesc();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mango.id = :mangoId AND r.approved = true")
    Double findAvgRatingByMangoId(Long mangoId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.mango.id = :mangoId AND r.approved = true")
    Long countByMangoId(Long mangoId);
}