package za.co.cbank.securefilestatementdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccountDTO {
	private Integer accountId;
	private String firstName;
	private String lastName;
	private String emailAddress;
	private String cellphoneNumber;
	private String idNumber;
}