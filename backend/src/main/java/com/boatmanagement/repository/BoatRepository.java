package com.boatmanagement.repository;

import com.boatmanagement.entity.Boat;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BoatRepository extends JpaRepository<Boat, Long> {

    @Query("SELECT b FROM Boat b WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:type IS NULL OR b.type = :type)")
    Page<Boat> findByFilters(
            @Param("search") String search,
            @Param("status") BoatStatus status,
            @Param("type") BoatType type,
            Pageable pageable);
}
