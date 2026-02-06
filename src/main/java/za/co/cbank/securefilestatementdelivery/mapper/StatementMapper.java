package za.co.cbank.securefilestatementdelivery.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.co.cbank.securefilestatementdelivery.dto.StatementDTO;
import za.co.cbank.securefilestatementdelivery.entity.Statement;

@Mapper(componentModel = "spring")
public interface StatementMapper {
	@Mapping(source = "customerAccount.accountId", target = "accountId")
	StatementDTO toDto(Statement entity);

	@Mapping(source = "accountId", target = "customerAccount.accountId")
	Statement toEntity(StatementDTO dto);
}