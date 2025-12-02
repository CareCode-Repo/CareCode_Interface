package com.carecode.domain.careFacility.app;

import com.carecode.domain.careFacility.dto.response.BookingResponse;
import com.carecode.domain.careFacility.dto.request.CreateBookingRequest;
import com.carecode.domain.careFacility.dto.request.UpdateBookingRequest;
import com.carecode.domain.careFacility.dto.request.CareFacilityRequest;
import com.carecode.domain.careFacility.dto.request.CareFacilitySearchRequest;
import com.carecode.domain.careFacility.dto.response.CareFacilityInfo;
import com.carecode.domain.careFacility.dto.response.CareFacilityListResponse;
import com.carecode.domain.careFacility.dto.response.CareFacilityStatsResponse;
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
    public List<CareFacilityInfo> getAllCareFacilities() {
        return careFacilityService.getAllCareFacilities();
    }

    @Transactional(readOnly = true)
    public CareFacilityInfo getCareFacilityById(Long id) {
        return careFacilityService.getCareFacilityById(id);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getCareFacilitiesByType(FacilityType facilityType) {
        return careFacilityService.getCareFacilitiesByType(facilityType);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getCareFacilitiesByLocation(String location) {
        return careFacilityService.getCareFacilitiesByLocation(location);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getCareFacilitiesByAgeRange(Integer minAge, Integer maxAge) {
        return careFacilityService.getCareFacilitiesByAgeRange(minAge, maxAge);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getCareFacilitiesByOperatingHours(String operatingHours) {
        return careFacilityService.getCareFacilitiesByOperatingHours(operatingHours);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getPopularCareFacilities(Integer limit) {
        return careFacilityService.getPopularCareFacilities(limit);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getNewCareFacilities(Integer limit) {
        return careFacilityService.getNewCareFacilities(limit);
    }

    @Transactional(readOnly = true)
    public List<CareFacilityInfo> getCareFacilitiesWithinRadius(Double lat, Double lon, Double radius) {
        return careFacilityService.getCareFacilitiesWithinRadius(lat, lon, radius);
    }

    @Transactional(readOnly = true)
    public CareFacilityListResponse searchCareFacilities(CareFacilitySearchRequest request) {
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
    public CareFacilityStatsResponse getFacilityStats() {
        return careFacilityService.getFacilityStats();
    }

    @Transactional
    public BookingResponse createBooking(Long facilityId, CreateBookingRequest request, UserDetails userDetails) {
        return bookingService.createBooking(facilityId, request, userDetails);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, UserDetails userDetails) {
        return bookingService.getBookingById(bookingId, userDetails);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(UserDetails userDetails) {
        return bookingService.getUserBookings(userDetails);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getFacilityBookings(Long facilityId) {
        return bookingService.getFacilityBookings(facilityId);
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, String status, UserDetails userDetails) {
        return bookingService.updateBookingStatus(bookingId, status, userDetails);
    }

    @Transactional
    public void cancelBooking(Long bookingId, UserDetails userDetails) {
        bookingService.cancelBooking(bookingId, userDetails);
    }

    @Transactional
    public BookingResponse updateBooking(Long bookingId, UpdateBookingRequest request, UserDetails userDetails) {
        return bookingService.updateBooking(bookingId, request, userDetails);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayBookings() {
        return bookingService.getTodayBookings();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayBookingsByFacility(Long facilityId) {
        return bookingService.getTodayBookingsByFacility(facilityId);
    }
}


