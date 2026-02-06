package za.co.cbank.securefilestatementdelivery.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.entity.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StatementRepositoryTest {

	@Autowired
	private StatementRepository statementRepository;

	@Autowired
	private CustomerAccountRepository accountRepository;

	@Test
	void findByRetrievalToken_ShouldReturnCorrectStatement() {
		CustomerAccount account = CustomerAccount.builder().accountId(1000000003).idNumber("9001010000085").build();
		accountRepository.save(account);

		String uniqueToken = "secure-and-unique-token";
		Statement statement = Statement.builder()
				.retrievalToken(uniqueToken)
				.customerAccount(account)
				.filename("test.pdf")
				.build();
		statementRepository.save(statement);

		Statement found = statementRepository.findByRetrievalToken(uniqueToken);

		assertNotNull(found);
		assertEquals("test.pdf", found.getFilename());

		assertNull(statementRepository.findByRetrievalToken("invalid-token"));
	}
}