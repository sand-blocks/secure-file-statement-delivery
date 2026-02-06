package za.co.cbank.securefilestatementdelivery.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

	private List<UserCredentials> users = new ArrayList<>();

	public List<UserCredentials> getUsers() { return users; }
	public void setUsers(List<UserCredentials> users) { this.users = users; }

	public static class UserCredentials {
		private String name;
		private String password;
		private String role;

		// Getters and Setters
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public String getPassword() { return password; }
		public void setPassword(String password) { this.password = password; }
		public String getRole() { return role; }
		public void setRole(String role) { this.role = role; }
	}
}