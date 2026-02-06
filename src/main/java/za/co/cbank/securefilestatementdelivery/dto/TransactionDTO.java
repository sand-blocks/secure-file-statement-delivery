package za.co.cbank.securefilestatementdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
	private Integer transactionId;
	private LocalDate postDate;
	private BigDecimal amount;
	private String description;
	private String drOrCr;
	private Integer accountId;
}