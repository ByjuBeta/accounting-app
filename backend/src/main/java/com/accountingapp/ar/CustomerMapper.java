package com.accountingapp.ar;

import com.accountingapp.ar.dto.AddressDto;
import com.accountingapp.ar.dto.CustomerDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDto toDto(Customer customer);

    Address toEntity(AddressDto dto);
}
