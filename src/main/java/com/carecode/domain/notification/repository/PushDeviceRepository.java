package com.carecode.domain.notification.repository;

import com.carecode.domain.notification.entity.PushDevice;
import com.carecode.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    /** 토큰은 전역 유일하다. 기기를 바꿔 로그인하면 주인만 옮겨 간다. */
    Optional<PushDevice> findByToken(String token);

    List<PushDevice> findByUser(User user);

    /** 발송할 때 쓰는 것은 토큰 문자열뿐이다. 엔티티를 통째로 올리지 않는다. */
    @Query("SELECT d.token FROM PushDevice d WHERE d.user = :user")
    List<String> findTokensByUser(@Param("user") User user);
}
