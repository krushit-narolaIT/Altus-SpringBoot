package com.narola.service;

import com.narola.common.Message;
import com.narola.common.enums.RoleType;
import com.narola.common.exception.ApplicationException;
import com.narola.common.mapper.Mapper;
import com.narola.common.utils.EmailUtils;
import com.narola.dto.UpdateUserDTO;
import com.narola.dto.UserDTO;
import com.narola.entity.User;
import com.narola.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Mapper mapper;

    public void userBlocked(int userId) throws ApplicationException {
        if (userRepository.isUserBlocked(userId)) {
            throw new ApplicationException(Message.User.YOUR_ACCOUNT_IS_SUSPENDED_PLEASE_CONTACT_SUPPORT);
        }
    }

    @Transactional
    public void registerUser(User user) throws ApplicationException {
        if (userRepository.existsByEmailIdOrPhoneNo(user.getEmailId(), user.getPhoneNo())) {
            throw new ApplicationException(Message.USER_ALREADY_EXIST);
        }
        User savedUser = userRepository.save(user);
        String displayId = user.getRole().getRoleType() == RoleType.ROLE_CUSTOMER ? generateCustomerDisplayId(savedUser.getUserId()) : generateDriverDisplayId(savedUser.getUserId());
        savedUser.setDisplayId(displayId);
        userRepository.save(savedUser);
        try {
            EmailUtils.sendWelcomeEmailWithCSS(user.getEmailId(), user.getFirstName(), user.getLastName());
        } catch (MessagingException e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }


    private String generateCustomerDisplayId(int userId) {
        String timestampPart = String.valueOf(System.currentTimeMillis() % 1000);
        String userIdPart = String.format("%04d", userId % 10000);
        return "US" + userIdPart + "R" + timestampPart;
    }

    private String generateDriverDisplayId(int userId) {
        String timestampPart = String.valueOf(System.currentTimeMillis() % 1000);
        String userIdPart = String.format("%04d", userId % 10000);
        return "DR" + userIdPart + "V" + timestampPart;
    }

    public UserDTO userLogin(String email, String password) throws ApplicationException {
        User user = userRepository.findByEmailIdAndPassword(email, password).orElseThrow(() -> new ApplicationException(Message.User.PLEASE_ENTER_VALID_EMAIL_OR_PASS));
        return mapper.convertToDTO(user);
    }

    public List<UserDTO> getAllCustomers() throws ApplicationException {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserDTO.UserDTOBuilder()
                        .setUserId(user.getUserId())
                        .setRole(user.getRole())
                        .setFirstName(user.getFirstName())
                        .setLastName(user.getLastName())
                        .setPhoneNo(user.getPhoneNo())
                        .setEmailId(user.getEmailId())
                        .setPassword(user.getPassword())
                        .setDisplayId(user.getDisplayId())
                        .setIsActive(user.isActive())
                        .setIsBlocked(user.isBlocked())
                        .setTotalRatings(user.getTotalRatings())
                        .setRatingCount(user.getRatingCount())
                        .setCreatedAt(user.getCreatedAt())
                        .setUpdatedAt(user.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void updateUser(UpdateUserDTO userDTO, int userId) throws ApplicationException {
        if (userId == 0) {
            throw new ApplicationException("User ID is required");
        }
        User existingUser = userRepository.findById(userId).orElseThrow(() -> new ApplicationException(Message.User.USER_NOT_FOUND));
        if (userDTO.getFirstName() != null) {
            existingUser.setFirstName(userDTO.getFirstName());
        }
        if (userDTO.getLastName() != null) {
            existingUser.setLastName(userDTO.getLastName());
        }
        if (userDTO.getPhoneNo() != null) {
            existingUser.setPhoneNo(userDTO.getPhoneNo());
        }
        if (userDTO.getEmailId() != null) {
            existingUser.setEmailId(userDTO.getEmailId());
        }
        userRepository.save(existingUser);
    }

    public void updatePassword(String email, String oldPassword, String newPassword) throws ApplicationException {
        User user = userRepository.findByEmailId(email).orElseThrow(() -> new ApplicationException(Message.User.USER_NOT_FOUND));
        if (!user.getPassword().equals(oldPassword)) {
            throw new ApplicationException(Message.User.PASSWORD_MISMATCHED);
        }
        userRepository.updatePasswordByEmailId(email, newPassword);
    }

    public void blockUser(int userId) throws ApplicationException {
        if (!userRepository.existsById(userId)) {
            throw new ApplicationException(Message.User.USER_NOT_FOUND);
        }
        userRepository.blockUserById(userId);
    }

    public String getUserNameById(int userId) throws ApplicationException {
        return userRepository.findFullNameByUserId(userId);
    }

    public String getUserDisplayIdById(int userId) throws ApplicationException {
        return userRepository.findDisplayIdByUserId(userId);
    }


    public User getUserDetails(int fromUserId) throws ApplicationException {
        return userRepository.findById(fromUserId).orElseThrow(() -> new ApplicationException(Message.User.USER_NOT_FOUND));
    }

    public List<UserDTO> getUsersWithLessRatingAndReviews(int ratingThreshold, int reviewCountThreshold) throws ApplicationException {
        List<User> users = userRepository.findByLowRatingAndReviewCount(ratingThreshold, reviewCountThreshold);
        return users.stream().map(user -> new UserDTO.UserDTOBuilder()
                .setUserId(user.getUserId())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setDisplayId(user.getDisplayId())
                .setTotalRatings(user.getTotalRatings())
                .setRatingCount(user.getRatingCount())
                .build()
        ).collect(Collectors.toList());
    }

    public List<User> getCustomersByOffsetAndLimit(int offset, int limit) throws ApplicationException {
        try {
            Pageable pageable = PageRequest.of(offset / limit, limit);
            Page<User> userPage = userRepository.findAll(pageable);
            return userPage.getContent();
        } catch (Exception e) {
            throw new ApplicationException("Error while fetching users with offset and limit", e);
        }
    }

    public List<User> getCustomersByDescendingOrder(boolean flag) throws ApplicationException {
        try {
            return userRepository.findAll(Sort.by(flag == true ? Sort.Direction.DESC : Sort.Direction.ASC));
        } catch (Exception e) {
            throw new ApplicationException(Message.User.ERROR_WHILE_GETTING_ALL_CUSTOMERS, e);
        }
    }

    public List<User> getCustomersByPage(int page, int recordsPerPage) throws ApplicationException {
        try {
            Pageable pageable = PageRequest.of(page - 1, recordsPerPage);
            Page<User> userPage = userRepository.findAll(pageable);
            return userPage.getContent();
        } catch (Exception e) {
            throw new ApplicationException("Error while fetching users by page", e);
        }
    }

    public void addFavouriteUser(int customerId, int driverId) throws ApplicationException {
        if (!userRepository.existsById(customerId)) {
            throw new ApplicationException(Message.User.USER_NOT_FOUND);
        }
        User user = userRepository.findById(driverId).orElseThrow(() -> new ApplicationException(Message.User.DRIVER_NOT_FOUND));
        if (userRepository.isAlreadyFavourite(customerId, driverId)) {
            throw new ApplicationException(Message.User.DRIVER_ALREADY_FAVOURITE);
        }
        userRepository.addFavouriteUser(customerId, List.of(user));
    }

    public void removeFavouriteDriver(int customerId, int driverId) throws ApplicationException {
        userRepository.removeFavouriteUser(customerId, driverId);
    }
}
