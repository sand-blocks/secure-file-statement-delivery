package za.co.cbank.securefilestatementdelivery.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.cbank.securefilestatementdelivery.dto.StatementDTO;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.entity.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class StatementMapperTest {

	@Autowired
	private StatementMapper mapper;

	@Test
	void shouldFlattenAccountIdInDto() {
		CustomerAccount account = CustomerAccount.builder().accountId(1000000003).build();
		Statement entity = Statement.builder()
				.statementId(11)
				.customerAccount(account)
				.filename("49a26347453d40b1a9bcb4939f259828.pdf")
				.build();

		StatementDTO dto = mapper.toDto(entity);

		assertNotNull(dto);
		assertEquals(1000000003, dto.getAccountId());
		assertEquals("49a26347453d40b1a9bcb4939f259828.pdf", dto.getFilename());
	}
}