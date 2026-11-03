package com.ecomm.user.service;

import com.ecomm.user.dto.request.AddressRequest;
import com.ecomm.user.dto.response.AddressResponse;
import com.ecomm.user.entity.AddressType;
import com.ecomm.user.entity.UserAddress;
import com.ecomm.user.entity.UserProfile;
import com.ecomm.user.exception.UserServiceException;
import com.ecomm.user.repository.UserAddressRepository;
import com.ecomm.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(userAddressRepository, userProfileRepository);
    }

    @Test
    void createAddress_WhenMaxQuotaReached_ThrowsUserServiceException() {
        UserProfile customer = new UserProfile();
        customer.setId(1L);
        customer.setUuid("cust-uuid-1");

        when(userProfileRepository.findByUuid("cust-uuid-1")).thenReturn(Optional.of(customer));
        when(userAddressRepository.countByUserProfileId(1L)).thenReturn((long) AddressService.MAX_ADDRESSES_PER_USER);

        AddressRequest request = new AddressRequest();
        request.setRecipientName("Alice Customer");
        request.setPhoneNumber("+1000000001");
        request.setStreetLine1("100 Main St");
        request.setCity("Austin");
        request.setStateProvince("TX");
        request.setPostalCode("78701");
        request.setCountryCode("US");

        assertThrows(UserServiceException.class,
            () -> addressService.createAddress("cust-uuid-1", request));
    }

    @Test
    void createAddress_FirstAddressForUser_AutomaticallyMarksDefaultShippingAndBilling() {
        UserProfile customer = new UserProfile();
        customer.setId(1L);
        customer.setUuid("cust-uuid-1");

        when(userProfileRepository.findByUuid("cust-uuid-1")).thenReturn(Optional.of(customer));
        when(userAddressRepository.countByUserProfileId(1L)).thenReturn(0L);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(inv -> {
            UserAddress addr = inv.getArgument(0);
            addr.setUuid("addr-uuid-new");
            return addr;
        });

        AddressRequest request = new AddressRequest();
        request.setLabel("Home");
        request.setRecipientName("Alice Customer");
        request.setPhoneNumber("+1000000001");
        request.setAddressType(AddressType.BOTH);
        request.setStreetLine1("100 Main St");
        request.setCity("Austin");
        request.setStateProvince("TX");
        request.setPostalCode("78701");
        request.setCountryCode("US");
        request.setDefaultShipping(false);
        request.setDefaultBilling(false);

        AddressResponse created = addressService.createAddress("cust-uuid-1", request);

        assertTrue(created.isDefaultShipping(), "First address should automatically become default shipping");
        assertTrue(created.isDefaultBilling(), "First address should automatically become default billing");
        verify(userAddressRepository).clearDefaultShippingForUser(1L);
        verify(userAddressRepository).clearDefaultBillingForUser(1L);
    }
}

