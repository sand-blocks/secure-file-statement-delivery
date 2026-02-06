package za.co.cbank.securefilestatementdelivery.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.cbank.securefilestatementdelivery.dto.TransactionDTO;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TransactionMapperTest {

	@Autowired
	private TransactionMapper mapper;

	@Test
	void shouldMapTransactionWithAccountIdAndMaintainAmount() {
		CustomerAccount account = CustomerAccount.builder().accountId(1000000001).build();
		Transaction entity = Transaction.builder()
				.transactionId(15)
				.amount(new java.math.BigDecimal("7.50"))
				.customerAccount(account)
				.drOrCr("DR")
				.build();

		TransactionDTO dto = mapper.toDto(entity);

		assertNotNull(dto);
		assertEquals(1000000001, dto.getAccountId());
		assertEquals(0, new java.math.BigDecimal("7.50").compareTo(dto.getAmount()));
	}
}
