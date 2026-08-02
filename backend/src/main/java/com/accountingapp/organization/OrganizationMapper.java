package com.accountingapp.organization;

import com.accountingapp.organization.dto.OrganizationDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationDto toDto(Organization organization);
}
