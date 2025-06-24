package com.narola.repository;

import com.narola.entity.VehicleService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleServiceRepository extends JpaRepository<VehicleService, Integer> {

    @Query("SELECT COUNT(vs) FROM VehicleService vs WHERE LOWER(vs.serviceName) = :name")
    long countByServiceNameIgnoreCase(@Param("name") String name);

    @Query("SELECT DISTINCT bm.vehicleService FROM BrandModel bm JOIN bm.vehicles v JOIN v.driver d WHERE d.isAvailable = true")
    List<VehicleService> findAvailableVehicleServices();
}
