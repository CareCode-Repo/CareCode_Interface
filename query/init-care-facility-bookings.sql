-- 방문 예약 (대기중)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    1, 'user_1753176450894_771', '김민수', 3, '김부모', '010-1234-5678', 
    'VISIT', 'PENDING', '2024-01-15 10:00:00', '2024-01-15 11:00:00', 
    '알레르기 주의', '첫 방문입니다', NOW(), NOW()
);

-- 정기 예약 (확정)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    2, 'user_1753176450894_772', '이지은', 4, '이부모', '010-2345-6789', 
    'REGULAR', 'CONFIRMED', '2024-01-16 09:00:00', '2024-01-16 17:00:00', 
    '특별한 요구사항 없음', '정기 이용 시작', NOW(), NOW()
);

-- 임시 예약 (대기중)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    3, 'user_1753176450894_773', '박준호', 2, '박부모', '010-3456-7890', 
    'TEMPORARY', 'PENDING', '2024-01-17 14:00:00', '2024-01-17 16:00:00', 
    '잠깐 맡겨주세요', '긴급한 일이 있어서', NOW(), NOW()
);

-- 방문 예약 (완료)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    1, 'user_1753176450894_774', '최수진', 5, '최부모', '010-4567-8901', 
    'VISIT', 'COMPLETED', '2024-01-10 13:00:00', '2024-01-10 14:00:00', 
    '시설 견학 희망', '입학을 고려중입니다', NOW(), NOW()
);

-- 정기 예약 (취소됨)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    2, 'user_1753176450894_775', '정현우', 3, '정부모', '010-5678-9012', 
    'REGULAR', 'CANCELLED', '2024-01-18 08:00:00', '2024-01-18 18:00:00', 
    '없음', '개인 사정으로 취소', NOW(), NOW()
);

-- 오늘 예약 (확정)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    1, 'user_1753176450894_776', '한소희', 4, '한부모', '010-6789-0123', 
    'VISIT', 'CONFIRMED', CONCAT(CURDATE(), ' 15:00:00'), CONCAT(CURDATE(), ' 16:00:00'), 
    '특별한 요구사항 없음', '오늘 방문 예정', NOW(), NOW()
);

-- 내일 예약 (대기중)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    3, 'user_1753176450894_777', '송민재', 2, '송부모', '010-7890-1234', 
    'TEMPORARY', 'PENDING', CONCAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), ' 10:00:00'), 
    CONCAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), ' 12:00:00'), 
    '잠깐 맡겨주세요', '내일 임시 이용', NOW(), NOW()
);

-- 다음주 예약 (확정)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    2, 'user_1753176450894_778', '임서연', 5, '임부모', '010-8901-2345', 
    'REGULAR', 'CONFIRMED', CONCAT(DATE_ADD(CURDATE(), INTERVAL 7 DAY), ' 09:00:00'), 
    CONCAT(DATE_ADD(CURDATE(), INTERVAL 7 DAY), ' 17:00:00'), 
    '정기 이용 시작', '다음주부터 정기 이용', NOW(), NOW()
);

-- 시간 중복 테스트용 예약 (확정)
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    1, 'user_1753176450894_779', '강동원', 3, '강부모', '010-9012-3456', 
    'VISIT', 'CONFIRMED', '2024-01-20 10:00:00', '2024-01-20 11:00:00', 
    '없음', '시간 중복 테스트', NOW(), NOW()
);

-- 다른 사용자의 예약
INSERT INTO care_facility_bookings (
    facility_id, user_id, child_name, child_age, parent_name, parent_phone, 
    booking_type, status, start_time, end_time, special_requirements, notes, created_at, updated_at
) VALUES (
    2, 'user_1753176450894_780', '윤지민', 4, '윤부모', '010-0123-4567', 
    'REGULAR', 'PENDING', '2024-01-21 08:00:00', '2024-01-21 18:00:00', 
    '특별한 요구사항 없음', '다른 사용자 예약', NOW(), NOW()
); 