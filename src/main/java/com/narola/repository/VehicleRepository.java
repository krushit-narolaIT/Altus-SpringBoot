package com.narola.repository;

import com.narola.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.driver.driverId = :driverId")
    int countByDriverId(@Param("driverId") int driverId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Vehicle v WHERE v.driver.user.userId = :userId")
    void deleteByUserId(@Param("userId") int userId);
}
