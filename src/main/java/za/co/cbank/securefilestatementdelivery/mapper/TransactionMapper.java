package za.co.cbank.securefilestatementdelivery.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.co.cbank.securefilestatementdelivery.dto.TransactionDTO;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
	@Mapping(source = "customerAccount.accountId", target = "accountId")
	TransactionDTO toDto(Transaction entity);

	@Mapping(source = "accountId", target = "customerAccount.accountId")
	Transaction toEntity(TransactionDTO dto);
}