-- 육아 시설 테스트 데이터 생성 스크립트
-- MariaDB/MySQL용

-- 기존 테스트 데이터 삭제 (선택사항)
DELETE FROM TBL_CARE_FACILITIES WHERE facility_code IN ('FAC001', 'FAC002', 'FAC003', 'FAC004', 'FAC005');

-- 육아 시설 1: 행복어린이집
INSERT INTO TBL_CARE_FACILITIES (
    facility_code, name, facility_type, address, latitude, longitude, 
    phone, email, website, capacity, current_enrollment, available_spots,
    age_range_min, age_range_max, operating_hours, tuition_fee, 
    additional_fees, facilities, curriculum, teacher_count, 
    student_teacher_ratio, accreditation, rating, review_count,
    is_active, is_public, subsidy_available, created_at, updated_at
) VALUES (
    'FAC001', '행복어린이집', 'DAYCARE', '서울시 강남구 테헤란로 123', 37.501274, 127.039585,
    '02-1234-5678', 'happy@daycare.com', 'http://www.happydaycare.com', 50, 35, 15,
    0, 5, '07:00-19:00', 500000, '급식비 10만원, 교재비 5만원',
    '놀이터, 실내체육관, 도서관, 미술실', '창의력 중심 교육과정', 8,
    '5:1', '보건복지부 인증', 4.5, 25,
    true, false, true, NOW(), NOW()
);

-- 육아 시설 2: 맘편한유치원
INSERT INTO TBL_CARE_FACILITIES (
    facility_code, name, facility_type, address, latitude, longitude, 
    phone, email, website, capacity, current_enrollment, available_spots,
    age_range_min, age_range_max, operating_hours, tuition_fee, 
    additional_fees, facilities, curriculum, teacher_count, 
    student_teacher_ratio, accreditation, rating, review_count,
    is_active, is_public, subsidy_available, created_at, updated_at
) VALUES (
    'FAC002', '맘편한유치원', 'KINDERGARTEN', '서울시 서초구 반포대로 45', 37.504198, 127.004055,
    '02-2345-6789', 'mom@kindergarten.com', 'http://www.momkindergarten.com', 80, 65, 15,
    3, 6, '08:00-18:00', 600000, '급식비 12만원, 교재비 8만원',
    '놀이터, 실내체육관, 도서관, 미술실, 음악실', '전인교육 중심 교육과정', 12,
    '6:1', '교육부 인증', 4.8, 42,
    true, false, true, NOW(), NOW()
);

-- 육아 시설 3: 아이튼튼놀이방
INSERT INTO TBL_CARE_FACILITIES (
    facility_code, name, facility_type, address, latitude, longitude, 
    phone, email, website, capacity, current_enrollment, available_spots,
    age_range_min, age_range_max, operating_hours, tuition_fee, 
    additional_fees, facilities, curriculum, teacher_count, 
    student_teacher_ratio, accreditation, rating, review_count,
    is_active, is_public, subsidy_available, created_at, updated_at
) VALUES (
    'FAC003', '아이튼튼놀이방', 'PLAYGROUP', '서울시 송파구 올림픽로 300', 37.515512, 127.112946,
    '02-3456-7890', 'strong@playgroup.com', 'http://www.strongplaygroup.com', 30, 20, 10,
    1, 4, '09:00-17:00', 400000, '급식비 8만원, 교재비 3만원',
    '놀이터, 실내놀이터, 미술실', '놀이 중심 교육과정', 5,
    '4:1', '보건복지부 인증', 4.2, 18,
    true, false, true, NOW(), NOW()
);

-- 육아 시설 4: 맘스케어보육원
INSERT INTO TBL_CARE_FACILITIES (
    facility_code, name, facility_type, address, latitude, longitude, 
    phone, email, website, capacity, current_enrollment, available_spots,
    age_range_min, age_range_max, operating_hours, tuition_fee, 
    additional_fees, facilities, curriculum, teacher_count, 
    student_teacher_ratio, accreditation, rating, review_count,
    is_active, is_public, subsidy_available, created_at, updated_at
) VALUES (
    'FAC004', '맘스케어보육원', 'NURSERY', '서울시 마포구 월드컵북로 400', 37.566345, 126.901451,
    '02-4567-8901', 'care@nursery.com', 'http://www.carenursery.com', 40, 28, 12,
    0, 3, '07:30-18:30', 450000, '급식비 9만원, 교재비 4만원',
    '놀이터, 실내놀이터, 수면실, 미술실', '영유아 발달 중심 교육과정', 6,
    '4:1', '보건복지부 인증', 4.6, 31,
    true, false, true, NOW(), NOW()
);

-- 육아 시설 5: 튼튼소아방
INSERT INTO TBL_CARE_FACILITIES (
    facility_code, name, facility_type, address, latitude, longitude, 
    phone, email, website, capacity, current_enrollment, available_spots,
    age_range_min, age_range_max, operating_hours, tuition_fee, 
    additional_fees, facilities, curriculum, teacher_count, 
    student_teacher_ratio, accreditation, rating, review_count,
    is_active, is_public, subsidy_available, created_at, updated_at
) VALUES (
    'FAC005', '튼튼소아방', 'OTHER', '서울시 노원구 동일로 1000', 37.654321, 127.056789,
    '02-5678-9012', 'healthy@childcare.com', 'http://www.healthychildcare.com', 25, 15, 10,
    2, 6, '08:30-17:30', 350000, '급식비 7만원, 교재비 2만원',
    '놀이터, 실내놀이터, 미술실, 음악실', '건강 중심 교육과정', 4,
    '4:1', '보건복지부 인증', 4.3, 22,
    true, false, true, NOW(), NOW()
);

-- 생성된 시설 확인
SELECT id, facility_code, name, facility_type, address, capacity, available_spots, rating 
FROM TBL_CARE_FACILITIES 
ORDER BY id; 