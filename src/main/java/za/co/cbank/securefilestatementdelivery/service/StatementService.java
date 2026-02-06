package za.co.cbank.securefilestatementdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import za.co.cbank.securefilestatementdelivery.Audit.annotation.Auditable;
import za.co.cbank.securefilestatementdelivery.dto.StatementDTO;
import za.co.cbank.securefilestatementdelivery.exception.DatabaseEntityException;
import za.co.cbank.securefilestatementdelivery.exception.FileCreationException;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.entity.Statement;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;
import za.co.cbank.securefilestatementdelivery.mapper.StatementMapper;
import za.co.cbank.securefilestatementdelivery.mapper.TransactionMapper;
import za.co.cbank.securefilestatementdelivery.repository.CustomerAccountRepository;
import za.co.cbank.securefilestatementdelivery.repository.StatementRepository;
import za.co.cbank.securefilestatementdelivery.repository.TransactionRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

	private final StatementRepository statementRepository;
	private final TransactionRepository transactionRepository;
	private final CustomerAccountRepository customerAccountRepository;
	private final FileCreationService fileCreationService;
	private final FileStorageService fileStorageService;
	private final StatementMapper statementMapper;
	private final TransactionMapper transactionMapper;
	private final TemplateEngine templateEngine;

	@Value("${config.pdf.retrieval.public_url_base}")
	private String retrievalUrlBase;



	@Auditable(action = "Statement downloading using public link")
	public Mono<byte[]> downloadStatementUsingToken(String retrievalToken) throws Exception {
		try {
			//Get the statement download link
			Statement statement = Optional.ofNullable(statementRepository.findByRetrievalToken(retrievalToken))
					.orElseThrow(() -> new Exception("Invalid token"));
			//Retrieve statement using web client
			String downloadUrl = URLDecoder.decode(statement.getLink(), StandardCharsets.UTF_8);
			WebClient client = WebClient.create(downloadUrl);
			Mono<byte[]> fileBody = client.get()
					.retrieve()
					.bodyToMono(byte[].class);
			//Return the actual file
			return fileBody;
		} catch (Exception e) {
			throw new Exception("Failed to retrieve Statement");
		}
	}

	@Auditable(action = "New Statement generated")
	public Map<String, Object> generateStatement(Integer accountId){
		try {
			log.info("Starting file generation");
			//Get the transactions for the Statement
			List<Transaction> transactions = transactionRepository.findByCustomerAccount_AccountId_OrderByPostDate(accountId);
			if (transactions.isEmpty()) throw new DatabaseEntityException("No transactions found for account");
			CustomerAccount customerAccount = transactions.getFirst().getCustomerAccount();
			log.info("Got the transactions");

			//Generate HTML statement
			String htmlStatement = formatStatementTransactions(transactions);
			log.info("Formatted the transactions for the statement");

			//Generate the PDF statement file
			ByteArrayOutputStream outputStream = fileCreationService.GeneratePdfFile(htmlStatement,customerAccount.getIdNumber());
			log.info("Generated the file");

			//Upload the PDF statement file
			String filename = fileStorageService.uploadPdfStatement(outputStream);
			log.info("Uploaded the file to file storage");

			//Generate Presigned Link
			String presignedLink = fileStorageService.createPresignedLink(filename);
			log.info("Generated the presigned URL");

			//Create the Statement record
			StatementDTO statementDTO = StatementDTO.builder()
					.createdAt(LocalDateTime.now())
					.accountId(accountId)
					.filename(filename)
					.retrievalToken(UUID.randomUUID().toString())
					.link(presignedLink)
					.expiresAt(LocalDateTime.now().plusMinutes(30))
					.build();
			StatementDTO savedStatement = createStatement(statementDTO);
			log.info("Created the statement record");

			Map<String,Object> result = new HashMap<>();
			String retrievalLink = retrievalUrlBase + savedStatement.getRetrievalToken();
			result.put("retrievalLink",retrievalLink);
			return result;

		}
		catch (Exception e){
			throw new FileCreationException("Failed to create Statement");
		}
	}

	@Auditable(action = "Statement transactions formatted")
	public String formatStatementTransactions(List<Transaction> statementTransactions){
		List<Map<String, Object>> trnDataList = new ArrayList<>();
		//We need some transactions to retrieve the customerAccount
		if(statementTransactions == null || statementTransactions.isEmpty()) return "No transactions, cannot generate statement";
		log.info("Starting statement formatting");
		BigDecimal balance = BigDecimal.ZERO;
		//Calculate the balance to carry forward
		for (Transaction trn : statementTransactions) {
			if ("CR".equalsIgnoreCase(trn.getDrOrCr())) {
				balance = balance.add(trn.getAmount());
			} else if ("DR".equalsIgnoreCase(trn.getDrOrCr())) {
				balance = balance.subtract(trn.getAmount());
			}
		}

		Context  context = new Context();
		context.setVariable("customerAccount",statementTransactions.getFirst().getCustomerAccount());
		context.setVariable("transactions",statementTransactions);
		context.setVariable("totalBalance",balance);

		return templateEngine.process("statement",context);
	}

	@Auditable(action = "Statement created")
	public StatementDTO createStatement(StatementDTO statementDTO) {
		try {
			CustomerAccount account = customerAccountRepository.findById(statementDTO.getAccountId())
					.orElseThrow(() -> new DatabaseEntityException("Account not found"));

			Statement statement = statementMapper.toEntity(statementDTO);
			statement.setCustomerAccount(account);

			log.info("Statement created successfully.");
			return statementMapper.toDto(statementRepository.save(statement));
		} catch (DataIntegrityViolationException e) {
			throw new DatabaseEntityException("Database constraint violation: " + e.getMostSpecificCause().getMessage());
		} catch (Exception e) {
			log.error("Statement failed to create :", e);
			throw new RuntimeException("An unexpected error occurred.");
		}
	}
}
