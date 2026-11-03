package com.ecomm.user.repository;

import com.ecomm.user.entity.RoleName;
import com.ecomm.user.entity.UserProfile;
import com.ecomm.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUuid(String uuid);

    Optional<UserProfile> findByEmailIgnoreCase(String email);

    boolean existsByUuid(String uuid);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        SELECT DISTINCT u FROM UserProfile u
        LEFT JOIN u.roles r
        WHERE (:status IS NULL OR u.status = :status)
          AND (:role IS NULL OR r = :role)
          AND (:search IS NULL OR :search = ''
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<UserProfile> searchUsers(
        @Param("status") UserStatus status,
        @Param("role") RoleName role,
        @Param("search") String search,
        Pageable pageable
    );
}

