package com.ecomm.user.repository;

import com.ecomm.user.entity.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByUserProfileId(Long userProfileId);

    Optional<SellerProfile> findByUuid(String uuid);

    Optional<SellerProfile> findByStoreSlugIgnoreCase(String storeSlug);

    boolean existsByStoreSlugIgnoreCase(String storeSlug);

    boolean existsByStoreSlugIgnoreCaseAndIdNot(String storeSlug, Long id);
}

