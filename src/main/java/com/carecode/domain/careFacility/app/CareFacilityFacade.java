package com.carecode.domain.careFacility.app;

import com.carecode.domain.careFacility.dto.CareFacilityBookingDto;
import com.carecode.domain.careFacility.dto.CareFacilityRequest;
import com.carecode.domain.careFacility.dto.CareFacilityResponse;
import com.carecode.domain.careFacility.entity.FacilityType;
import com.carecode.domain.careFacility.service.CareFacilityBookingService;
import com.carecode.domain.careFacility.service.CareFacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareFacilityFacade {

    private final CareFacilityService careFacilityService;
    private final CareFacilityBookingService bookingService;

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getAllCareFacilities() {
        return careFacilityService.getAllCareFacilities();
    }

    @Transactional(readOnly = true)
    public CareFacilityResponse.CareFacility getCareFacilityById(Long id) {
        return careFacilityService.getCareFacilityById(id);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getCareFacilitiesByType(FacilityType facilityType) {
        return careFacilityService.getCareFacilitiesByType(facilityType);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getCareFacilitiesByLocation(String location) {
        return careFacilityService.getCareFacilitiesByLocation(location);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getCareFacilitiesByAgeRange(Integer minAge, Integer maxAge) {
        return careFacilityService.getCareFacilitiesByAgeRange(minAge, maxAge);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getCareFacilitiesByOperatingHours(String operatingHours) {
        return careFacilityService.getCareFacilitiesByOperatingHours(operatingHours);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getPopularCareFacilities(Integer limit) {
        return careFacilityService.getPopularCareFacilities(limit);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getNewCareFacilities(Integer limit) {
        return careFacilityService.getNewCareFacilities(limit);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityResponse.CareFacility> getCareFacilitiesWithinRadius(Double lat, Double lon, Double radius) {
        return careFacilityService.getCareFacilitiesWithinRadius(lat, lon, radius);
    }

    @Transactional(readOnly = true)
    public CareFacilityResponse.CareFacilityList searchCareFacilities(CareFacilityRequest.Search request) {
        return careFacilityService.searchCareFacilities(request);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        careFacilityService.incrementViewCount(id);
    }

    @Transactional
    public void updateRating(Long id, Double rating) {
        careFacilityService.updateRating(id, rating);
    }

    @Transactional(readOnly = true)
    public CareFacilityResponse.CareFacilityStats getFacilityStats() {
        return careFacilityService.getFacilityStats();
    }

    @Transactional
    public CareFacilityBookingDto.BookingResponse createBooking(Long facilityId, CareFacilityBookingDto.CreateBookingRequest request, UserDetails userDetails) {
        return bookingService.createBooking(facilityId, request, userDetails);
    }

    @Transactional(readOnly = true)
    public CareFacilityBookingDto.BookingResponse getBookingById(Long bookingId, UserDetails userDetails) {
        return bookingService.getBookingById(bookingId, userDetails);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityBookingDto.BookingResponse> getUserBookings(UserDetails userDetails) {
        return bookingService.getUserBookings(userDetails);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityBookingDto.BookingResponse> getFacilityBookings(Long facilityId) {
        return bookingService.getFacilityBookings(facilityId);
    }

    @Transactional
    public CareFacilityBookingDto.BookingResponse updateBookingStatus(Long bookingId, String status, UserDetails userDetails) {
        return bookingService.updateBookingStatus(bookingId, status, userDetails);
    }

    @Transactional
    public void cancelBooking(Long bookingId, UserDetails userDetails) {
        bookingService.cancelBooking(bookingId, userDetails);
    }

    @Transactional
    public CareFacilityBookingDto.BookingResponse updateBooking(Long bookingId, CareFacilityBookingDto.UpdateBookingRequest request, UserDetails userDetails) {
        return bookingService.updateBooking(bookingId, request, userDetails);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityBookingDto.BookingResponse> getTodayBookings() {
        return bookingService.getTodayBookings();
    }

    @Transactional(readOnly = true)
    public List<CareFacilityBookingDto.BookingResponse> getTodayBookingsByFacility(Long facilityId) {
        return bookingService.getTodayBookingsByFacility(facilityId);
    }
}


