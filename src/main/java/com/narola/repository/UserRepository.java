package com.narola.repository;

import com.narola.common.enums.RoleType;
import com.narola.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmailIdAndPassword(String emailId, String password);

    boolean existsByEmailIdAndPassword(String emailId, String password);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.emailId = :email OR u.phoneNo = :phone")
    boolean existsByEmailIdOrPhoneNo(String email, String phone);

    List<User> findByRole_Role(RoleType roleType);

    @Query("SELECT u.displayId FROM User u WHERE u.userId = :userId")
    String findDisplayIdByUserId(Integer userId);

    @Query("SELECT CONCAT(u.firstName, ' ', u.lastName) FROM User u WHERE u.userId = :userId")
    String findFullNameByUserId(Integer userId);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.password = :newPassword WHERE u.emailId = :email")
    void updatePasswordByEmailId(String email, String newPassword);

    @Transactional
    @Modifying
    @Query("""
           UPDATE User u SET u.totalRatings = ((u.totalRatings * u.ratingCount) + :rating) / (u.ratingCount + 1),
           u.ratingCount = u.ratingCount + 1 WHERE u.userId = :userId
           """)
    void updateUserRating(Integer userId, Integer rating);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.isBlocked = true WHERE u.userId = :userId")
    void blockUserById(Integer userId);

    @Query("SELECT u.isBlocked FROM User u WHERE u.userId = :userId")
    boolean isUserBlocked(Integer userId);

    @Query("SELECT u FROM User u WHERE u.totalRatings < :rating AND u.ratingCount < :reviewCount")
    List<User> findByLowRatingAndReviewCount(Integer rating, Integer reviewCount);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.favoriteUsers = :favoriteUsers WHERE u.userId = :userId")
    void addFavouriteUser(Integer userId, List<User> favoriteUsers);

    @Query("""
           SELECT CASE 
                    WHEN COUNT(f) > 0 
                        THEN TRUE 
                    ELSE FALSE END 
           FROM User u JOIN u.favoriteUsers f WHERE u.userId = :customerId AND f.userId = :driverId
           """)
    boolean isAlreadyFavourite(Integer customerId, Integer driverId);

    @Transactional
    @Modifying
    @Query("""
           UPDATE User u SET u.favoriteUsers = 
           (SELECT f FROM User u JOIN u.favoriteUsers f WHERE f.userId != :driverId)
           WHERE u.userId = :customerId
           """)
    void removeFavouriteUser(Integer customerId, Integer driverId);

    Optional<User> findByEmailId(String email);
}
