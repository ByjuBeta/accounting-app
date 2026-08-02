package com.accountingapp.ar;

import com.accountingapp.common.dto.AddressDto;
import com.accountingapp.ar.dto.CustomerDto;
import com.accountingapp.common.value.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDto toDto(Customer customer);

    Address toEntity(AddressDto dto);
}
