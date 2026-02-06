package za.co.cbank.securefilestatementdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "transaction_id")
	private Integer transactionId;
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	private LocalDate postDate;
	@ManyToOne
	@JoinColumn(name = "account_id", nullable = false)
	private CustomerAccount customerAccount;
	private BigDecimal amount;
	private String description;
	@Column(name = "dr_or_cr")
	private String drOrCr;
}



