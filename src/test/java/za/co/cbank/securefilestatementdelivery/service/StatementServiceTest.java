package za.co.cbank.securefilestatementdelivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

	@Mock
	private TemplateEngine templateEngine;

	@InjectMocks
	private StatementService statementService;

	private CustomerAccount testAccount;

	@BeforeEach
	void setUp() {
		testAccount = CustomerAccount.builder()
				.accountId(1000000001)
				.firstName("John")
				.idNumber("9001010000083")
				.build();
	}

	@Test
	void formatStatementTransactions_ShouldCalculateCorrectBalance() {
		List<Transaction> transactions = Arrays.asList(
				Transaction.builder().amount(new BigDecimal("1000.00")).drOrCr("CR").customerAccount(testAccount).build(),
				Transaction.builder().amount(new BigDecimal("500.00")).drOrCr("DR").customerAccount(testAccount).build(),
				Transaction.builder().amount(new BigDecimal("50.00")).drOrCr("CR").customerAccount(testAccount).build()
		);
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

		statementService.formatStatementTransactions(transactions);

		verify(templateEngine).process(eq("statement"), contextCaptor.capture());

		Context capturedContext = contextCaptor.getValue();

		BigDecimal capturedBalance = (BigDecimal) capturedContext.getVariable("totalBalance");
		CustomerAccount capturedAccount = (CustomerAccount) capturedContext.getVariable("customerAccount");

		assertEquals(0, new BigDecimal("550.00").compareTo(capturedBalance),
				"The balance should be 550.00");
		assertEquals("John", capturedAccount.getFirstName());
	}

	@Test
	void formatStatementTransactions_ShouldReturnEmpty_WhenListIsEmpty() {
		// Act
		String result = statementService.formatStatementTransactions(List.of());

		// Assert
		assertEquals("No transactions, cannot generate statement", result);
	}
}