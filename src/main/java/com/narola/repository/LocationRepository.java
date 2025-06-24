package com.narola.repository;

import com.narola.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {
    Optional<Location> findByIdAndIsActiveTrue(Integer id);

    @Query("SELECT l FROM Location l WHERE l.id = :locationId AND l.isActive = true")
    Optional<Location> findActiveLocationById(@Param("locationId") int locationId);

    @Query("SELECT " +
            "CASE  " +
            "   WHEN COUNT(l) > 0 THEN true " +
            "   ELSE false " +
            "END " +
            "FROM Location l WHERE l.id = :locationId AND l.isActive = true")
    boolean existsByIdAndIsActiveTrue(Integer locationId);

    List<Location> findAllByOrderByName();

    Page<Location> findByIsActive(Boolean active, Pageable pageable);
}
