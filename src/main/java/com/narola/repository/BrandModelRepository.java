package com.narola.repository;

import com.narola.entity.BrandModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandModelRepository extends JpaRepository<BrandModel, Integer> {

    @Query("SELECT COUNT(bm) FROM BrandModel bm WHERE bm.brandName = :brandName AND bm.model = :model")
    int countByBrandNameAndModel(@Param("brandName") String brandName, @Param("model") String model);

    @Query("SELECT DISTINCT b.brandName FROM BrandModel b")
    List<String> findAllDistinctBrands();

    @Query("SELECT DISTINCT b.model FROM BrandModel b WHERE b.brandName = :brandName")
    List<String> findModelsByBrand(@Param("brandName") String brandName);

    @Query("SELECT bm FROM BrandModel bm")
    List<BrandModel> findAllBrandModels();

    @Query("SELECT bm.minYear FROM BrandModel bm WHERE bm.brandModelId = :brandModelId")
    int findMinYearByBrandModelId(@Param("brandModelId") int brandModelId);
}