package za.co.cbank.securefilestatementdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import za.co.cbank.securefilestatementdelivery.Audit.annotation.Auditable;
import za.co.cbank.securefilestatementdelivery.dto.TransactionDTO;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.exception.DatabaseEntityException;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;
import za.co.cbank.securefilestatementdelivery.mapper.TransactionMapper;
import za.co.cbank.securefilestatementdelivery.repository.CustomerAccountRepository;
import za.co.cbank.securefilestatementdelivery.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final CustomerAccountRepository customerAccountRepository;
	private final TransactionMapper transactionMapper;

	@Auditable(action = "Transactions fetched by accountId",detail = "#accountId")
	public List<TransactionDTO> getTransactionsByAccountId(Integer accountId){
		log.info("Fetching transactions for accountId: {}", accountId);
		return transactionRepository.findByCustomerAccount_AccountId(accountId)
				.stream()
				.map(transactionMapper::toDto)
				.collect(Collectors.toList());
	}

	@Auditable(action = "Transaction fetched by transactionId",detail = "#transactionId")
	public TransactionDTO getTransactionByTransactionId(Integer transactionId){
		log.info("Fetching transaction by transactionId: {}", transactionId);
		return transactionRepository.findById(transactionId)
				.map(transactionMapper::toDto)
				.orElseThrow(() -> new DatabaseEntityException("Transaction not found using transactionId"));
	}

	@Auditable(action = "Transaction created")
	public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
		try {
			Transaction transaction = transactionMapper.toEntity(transactionDTO);
			CustomerAccount account = customerAccountRepository.findByAccountId(transactionDTO.getAccountId());

			transaction.setCustomerAccount(account);

			Transaction savedTransaction = transactionRepository.save(transaction);
			log.info("Transaction created successfully.");

			return transactionMapper.toDto(savedTransaction);
		} catch (DataIntegrityViolationException e) {
			throw new DatabaseEntityException("Database constraint violation: " + e.getMostSpecificCause().getMessage());
		} catch (Exception e) {
			log.error("Transaction failed to create :", e);
			throw new RuntimeException("An unexpected error occurred.");
		}
	}

}
