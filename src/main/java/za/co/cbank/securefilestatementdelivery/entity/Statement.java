package za.co.cbank.securefilestatementdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "statement_id")
	private Integer statementId;
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
	@ManyToOne
	@JoinColumn(name = "account_id", nullable = false)
	private CustomerAccount customerAccount;
	private String filename;
	@Column(unique = true)
	private String retrievalToken;
	@Column(length = 1000)
	private String link;
	private LocalDateTime expiresAt;
}
