package za.co.cbank.securefilestatementdelivery.mapper;

import org.mapstruct.Mapper;
import za.co.cbank.securefilestatementdelivery.dto.CustomerAccountDTO;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;

@Mapper(componentModel = "spring")
public interface CustomerAccountMapper {
	CustomerAccountDTO toDto(CustomerAccount entity);
	CustomerAccount toEntity(CustomerAccountDTO dto);
}