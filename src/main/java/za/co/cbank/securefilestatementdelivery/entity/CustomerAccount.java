package za.co.cbank.securefilestatementdelivery.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccount {

	@Id
	@Column(name = "account_id", nullable = false, unique = true)
	private Integer accountId;
	private String firstName;
	private String lastName;
	@Column(nullable = false, unique = true)
	private String idNumber;
	private String emailAddress;
	private String cellphoneNumber;
}