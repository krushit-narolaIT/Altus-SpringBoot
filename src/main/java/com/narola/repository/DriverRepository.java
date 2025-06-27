package com.narola.repository;

import com.narola.common.enums.DocumentVerificationStatus;
import com.narola.dto.PendingDriverDTO;
import com.narola.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Integer> {

    @Query("SELECT d.verificationStatus FROM Driver d WHERE d.driverId = :driverId")
    DocumentVerificationStatus findDocumentVerificationStatus(@Param("driverId") int driverId);

    @Transactional
    @Query("UPDATE Driver d SET d.licenceNumber = :licenceNumber, d.licencePhoto = :licencePhoto," +
            " d.verificationStatus = :verificationStatus WHERE d.user.userId = :userId")
    void updateDriverDetails(@Param("userId") int userId,
                             @Param("licenceNumber") String licenceNumber,
                             @Param("licencePhoto") String licencePhoto,
                             @Param("verificationStatus") DocumentVerificationStatus verificationStatus);

 /*   @Query("SELECT new PendingDriverDTO(" +
            "d.driverId, d.user.userId, d.licenceNumber, d.licencePhoto, " +
            "d.isDocumentVerified, d.comment, u.emailId, u.firstName, u.lastName, u.displayId) " +
            "FROM Driver d JOIN d.user u WHERE d.isDocumentVerified = FALSE")
    List<PendingDriverDTO> findDriversWithPendingVerification();*/

    @Transactional
    @Query("UPDATE Driver d SET d.verificationStatus = :verificationStatus, d.comment = :comment, " +
            "d.isDocumentVerified = :isDocumentVerified, d.isAvailable = :isAvailable " +
            "WHERE d.driverId = :driverId")
    void updateDriverVerificationStatus(@Param("driverId") int driverId,
                                        @Param("verificationStatus") DocumentVerificationStatus verificationStatus,
                                        @Param("comment") String comment,
                                        @Param("isDocumentVerified") boolean isDocumentVerified,
                                        @Param("isAvailable") boolean isAvailable);

    @Query("SELECT d.isDocumentVerified FROM Driver d WHERE d.driverId = :driverId")
    Boolean isDocumentVerified(@Param("driverId") int driverId);

    boolean existsByLicenceNumber(String licenceNumber);

    Optional<Driver> findByUser_UserId(int userId);

    @Modifying
    @Transactional
    @Query("UPDATE Driver d SET d.isAvailable = true WHERE d.driverId = :driverId")
    void updateDriverAvailability(@Param("driverId") int driverId);
}
