package za.co.cbank.securefilestatementdelivery.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest

class TransactionRepositoryTest {

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private CustomerAccountRepository accountRepository;

	@Test
	void findByAccountId_ShouldReturnSortedTransactions() {
		CustomerAccount account = CustomerAccount.builder()
				.accountId(1000000003)
				.firstName("Alice")
				.idNumber("9001010000085")
				.build();
		accountRepository.save(account);

		Transaction t_newer = Transaction.builder()
				.postDate(LocalDate.of(2025, 2, 1))
				.amount(new BigDecimal("500.00"))
				.customerAccount(account)
				.drOrCr("CR")
				.build();

		Transaction t_older = Transaction.builder()
				.postDate(LocalDate.of(2025, 1, 1))
				.amount(new BigDecimal("200.00"))
				.customerAccount(account)
				.drOrCr("DR")
				.build();

		transactionRepository.save(t_newer);
		transactionRepository.save(t_older);

		List<Transaction> results = transactionRepository.findByCustomerAccount_AccountId_OrderByPostDate(1000000003);

		assertEquals(2, results.size());
		assertEquals(LocalDate.of(2025, 1, 1), results.get(0).getPostDate(), "Older transaction should be first");
		assertEquals(LocalDate.of(2025, 2, 1), results.get(1).getPostDate(), "Newer transaction should be second");
	}
}
