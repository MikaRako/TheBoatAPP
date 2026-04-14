package com.boatmanagement.repository;

import com.boatmanagement.entity.Boat;
import com.boatmanagement.entity.BoatStatus;
import com.boatmanagement.entity.BoatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoatRepository extends JpaRepository<Boat, Long> {

    // Redeclare Spring Data methods with explicit @NonNull contracts so JDT's
    // flow analyser can verify call sites in BoatService without warnings.
    @Override
    @NonNull <S extends Boat> S save(@NonNull S entity);

    @Override
    @NonNull Optional<Boat> findById(@NonNull Long id);

    @Override
    boolean existsById(@NonNull Long id);

    @Override
    void deleteById(@NonNull Long id);

    /**
     * Full-text search on name and description.
     *
     * :search must be pre-escaped by the caller (% and _ replaced with \% and \_)
     * before being passed here, so that user-supplied wildcards are treated as
     * literals. The ESCAPE clause tells the DB which character to honour.
     */
    @Query("SELECT b FROM Boat b WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\' OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\') " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:type IS NULL OR b.type = :type)")
    Page<Boat> findByFilters(
            @Param("search") @Nullable String search,
            @Param("status") @Nullable BoatStatus status,
            @Param("type") @Nullable BoatType type,
            Pageable pageable);
}
