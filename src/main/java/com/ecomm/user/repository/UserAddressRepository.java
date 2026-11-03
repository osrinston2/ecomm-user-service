package com.ecomm.user.repository;

import com.ecomm.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserProfileIdOrderByDefaultShippingDescDefaultBillingDescCreatedAtDesc(Long userProfileId);

    Optional<UserAddress> findByUuidAndUserProfileId(String uuid, Long userProfileId);

    long countByUserProfileId(Long userProfileId);

    @Modifying
    @Query("UPDATE UserAddress a SET a.defaultShipping = false WHERE a.userProfile.id = :userProfileId AND a.defaultShipping = true")
    void clearDefaultShippingForUser(@Param("userProfileId") Long userProfileId);

    @Modifying
    @Query("UPDATE UserAddress a SET a.defaultBilling = false WHERE a.userProfile.id = :userProfileId AND a.defaultBilling = true")
    void clearDefaultBillingForUser(@Param("userProfileId") Long userProfileId);
}

