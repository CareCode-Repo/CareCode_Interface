package com.carecode.domain.careFacility.mapper;

import com.carecode.domain.careFacility.dto.response.BookingListResponse;
import com.carecode.domain.careFacility.dto.response.BookingResponse;
import com.carecode.domain.careFacility.entity.CareFacilityBooking;

/**
 * 예약 Entity와 DTO 간 변환을 담당하는 Mapper 클래스
 */
public class BookingMapper {

    /**
     * Entity를 DTO로 변환
     */
    public static BookingResponse fromEntity(CareFacilityBooking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .userId(booking.getUserId())
                .childName(booking.getChildName())
                .childAge(booking.getChildAge())
                .parentName(booking.getParentName())
                .parentPhone(booking.getParentPhone())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .specialRequirements(booking.getSpecialRequirements())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    /**
     * Entity를 목록 DTO로 변환
     */
    public static BookingListResponse toListResponse(CareFacilityBooking booking) {
        return BookingListResponse.builder()
                .id(booking.getId())
                .facilityId(booking.getFacility().getId())
                .facilityName(booking.getFacility().getName())
                .childName(booking.getChildName())
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

