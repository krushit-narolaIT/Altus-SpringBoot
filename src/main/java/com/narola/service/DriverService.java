package com.narola.service;

import com.narola.common.Message;
import com.narola.common.enums.DocumentVerificationStatus;
import com.narola.common.exception.ApplicationException;
import com.narola.common.exception.DBException;
import com.narola.dto.DriverDTO;
import com.narola.dto.DriverVerificationRequestDTO;
import com.narola.dto.PendingDriverDTO;
import com.narola.entity.Driver;
import com.narola.entity.User;
import com.narola.entity.Vehicle;
import com.narola.repository.BrandModelRepository;
import com.narola.repository.DriverRepository;
import com.narola.repository.VehicleRepository;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DriverService {
    private static final String STORAGE_PATH = "D:\\Project\\AltusDriverLicences";
    private final UserService userService = new UserService();

    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private BrandModelRepository brandModelRepository;

    public void storeDriverDetails(Driver driver) throws ApplicationException {
        Optional<Driver> driverOpt = driverRepository.findByUser_UserId(driver.getUser().getUserId());
        if (!driverOpt.isPresent()) {
            throw new ApplicationException(Message.User.USER_NOT_FOUND);
        }
        if (driverRepository.existsByLicenceNumber(driver.getLicenceNumber())) {
            throw new ApplicationException(Message.Driver.LICENCE_NUMBER_IS_ALREADY_EXIST);
        }
        if (driverRepository.findDocumentVerificationStatus(driver.getDriverId()) == DocumentVerificationStatus.PENDING) {
            throw new ApplicationException(Message.Driver.DOCUMENT_IS_UNDER_REVIEW);
        }
        Driver updatedDriver = new Driver.DriverBuilder()
                .setLicenceNumber(driver.getLicenceNumber())
                .setLicencePhoto(driver.getLicencePhoto())
                .setVerificationStatus(DocumentVerificationStatus.PENDING)
                .setUser(new User.UserBuilder().setUserId(driver.getUser().getUserId()).build())
                .build();
        driverRepository.save(updatedDriver);
    }

    public String storeLicencePhoto(Part filePart, String licenceNumber, String displayId) throws ApplicationException {
        try {
            File file = new File(STORAGE_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            String originalFileName = filePart.getSubmittedFileName();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = "DRI_" + licenceNumber + "_" + displayId + extension;
            Path path = Paths.get(STORAGE_PATH, fileName);
            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return path.toString();
        } catch (IOException e) {
            throw new DBException(Message.Driver.FAILED_TO_STORE_DOCUMENT + e.getMessage());
        }
    }

    public List<PendingDriverDTO> getPendingVerificationDrivers() throws DBException {
        return driverRepository.findDriversWithPendingVerification();
    }

    public boolean isDriverExist(int driverId) throws ApplicationException {
        return driverRepository.existsById(driverId);
    }

    public boolean isDocumentExist(int driverId) throws ApplicationException {
        return driverRepository.findDocumentVerificationStatus(driverId) == DocumentVerificationStatus.INCOMPLETE;
    }

    public void verifyDriver(DriverVerificationRequestDTO verificationRequestDTO, int driverId) throws ApplicationException {
        if (!isDriverExist(driverId)) {
            throw new ApplicationException(Message.DRIVER_NOT_EXIST);
        }
        if(!isDocumentExist(driverId)){
            throw new ApplicationException(Message.Driver.DOCUMENT_NOT_UPLOADED);
        }
        if (DocumentVerificationStatus.VERIFIED.name().equalsIgnoreCase(verificationRequestDTO.getVerificationStatus())) {
            driverRepository.updateDriverVerificationStatus(driverId, DocumentVerificationStatus.VERIFIED, null,
                    true, true);
        } else if (DocumentVerificationStatus.REJECTED.name().equalsIgnoreCase(verificationRequestDTO.getVerificationStatus())) {
            driverRepository.updateDriverVerificationStatus(driverId, DocumentVerificationStatus.REJECTED,
                    verificationRequestDTO.getMessage(),false,false
            );
        } else {
            throw new ApplicationException(Message.Driver.PLEASE_PERFORM_VALID_VERIFICATION_OPERATION);
        }

    }

    public List<DriverDTO> getAllDrivers() throws ApplicationException {
        List<Driver> drivers = driverRepository.findAll();
        return drivers.stream()
                .map(driver -> {
                    User user = driver.getUser();
                    return new DriverDTO.DriverDTOBuilder()
                            .setUserId(user.getUserId())
                            .setRole(user.getRole().getRoleType())
                            .setFirstName(user.getFirstName())
                            .setLastName(user.getLastName())
                            .setPhoneNo(user.getPhoneNo())
                            .setEmailId(user.getEmailId())
                            .setDisplayId(user.getDisplayId())
                            .setLicenceNumber(driver.getLicenceNumber())
                            .setLicencePhoto(driver.getLicencePhoto())
                            .setDocumentVerified(driver.isDocumentVerified())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public void addVehicle(Vehicle vehicle, int userId) throws ApplicationException {
        Driver driver = driverRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ApplicationException(Message.Driver.NO_DRIVERS_FOUND));
        DocumentVerificationStatus verificationStatus = driverRepository.findDocumentVerificationStatus(driver.getDriverId());
        if (verificationStatus != DocumentVerificationStatus.INCOMPLETE) {
            throw new ApplicationException(Message.Driver.DOCUMENT_NOT_UPLOADED);
        }
        if (verificationStatus != DocumentVerificationStatus.PENDING) {
            throw new ApplicationException(Message.Driver.DOCUMENT_NOT_VERIFIED);
        }
        if (vehicleRepository.existsById(driver.getDriverId())) {
            throw new ApplicationException(Message.Vehicle.DRIVER_VEHICLE_ALREADY_EXIST);
        }
        if (!brandModelRepository.existsById(vehicle.getVehicleId())) {
            throw new ApplicationException(Message.Vehicle.BRAND_MODEL_NOT_SUPPORTED);
        }
        int minYear = brandModelRepository.findMinYearByBrandModelId(vehicle.getBrandModel().getBrandModelId());
        if (vehicle.getYear() < minYear) {
            throw new ApplicationException(Message.Vehicle.BRAND_MODEL_YEAR_NOT_SUPPORTED);
        }
        vehicle.setDriver(driver);
        driverRepository.updateDriverAvailability(driver.getDriverId());
        vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(int userId) throws ApplicationException {
        Driver driver = driverRepository.findById(userId)
                .orElseThrow(() -> new DBException(Message.Driver.NO_DRIVERS_FOUND));
        if (vehicleRepository.countByDriverId(driver.getDriverId()) <= 0) {
            throw new ApplicationException(Message.Vehicle.VEHICLE_NOT_EXIST);
        }
        vehicleRepository.deleteByUserId(userId);
    }

    public int getDriverIdFromUserId(int userId) throws ApplicationException{
        Driver driver = driverRepository.findById(userId)
                .orElseThrow(() -> new DBException(Message.Driver.NO_DRIVERS_FOUND));
        return driver.getDriverId();
    }
}