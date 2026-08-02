package com.accountingapp.ap;

import com.accountingapp.ap.dto.VendorDto;
import com.accountingapp.common.dto.AddressDto;
import com.accountingapp.common.value.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    VendorDto toDto(Vendor vendor);

    Address toEntity(AddressDto dto);
}
