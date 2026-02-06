package za.co.cbank.securefilestatementdelivery.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.cbank.securefilestatementdelivery.dto.CustomerAccountDTO;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CustomerAccountMapperTest {

	@Autowired
	private CustomerAccountMapper mapper;

	@Test
	void shouldMapEntityToDto() {
		CustomerAccount entity = CustomerAccount.builder()
				.accountId(1000000001)
				.firstName("John")
				.lastName("Doe")
				.emailAddress("john.doe@cbank.co.za")
				.build();

		CustomerAccountDTO dto = mapper.toDto(entity);

		assertNotNull(dto);
		assertEquals(entity.getAccountId(), dto.getAccountId());
		assertEquals(entity.getFirstName(), dto.getFirstName());
	}
}