package za.co.cbank.securefilestatementdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3ClientConfig {
	@Value("${config.aws.s3client.access_key}")
	private String accessKey;
	@Value("${config.aws.s3client.secret_key}")
	private String secretKey;
	@Value("${config.aws.s3client.endpoint}")
	private String endpoint;

	@Bean
	public S3Client s3Client() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(secretKey,accessKey);

		return S3Client.builder()
				.endpointOverride(URI.create(endpoint))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.region(Region.AF_SOUTH_1)
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build())
				.build();
	}

	@Bean
	public  S3Presigner s3Presigner() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey,secretKey);

		return S3Presigner.builder()
				.endpointOverride(URI.create(endpoint))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.region(Region.AF_SOUTH_1)
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build())
				.build();
	}
}
