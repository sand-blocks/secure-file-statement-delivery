package za.co.cbank.securefilestatementdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementDTO {
	private Integer statementId;
	private LocalDateTime createdAt;
	private Integer accountId;
	private String filename;
	private String link;
	private String retrievalToken;
	private LocalDateTime expiresAt;
}