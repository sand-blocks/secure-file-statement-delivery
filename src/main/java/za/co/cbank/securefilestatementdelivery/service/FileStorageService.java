package za.co.cbank.securefilestatementdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import za.co.cbank.securefilestatementdelivery.Audit.annotation.Auditable;
import za.co.cbank.securefilestatementdelivery.exception.FileStorageException;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	@Value("${config.aws.s3client.bucket_name}")
	private String bucketName;
	@Value("${config.aws.s3client.link_expiry_mins}")
	private Integer expiryMins;

	@Auditable(action = "Presigned Link generated")
	public String createPresignedLink(String filename){
		try {
			//Create the GetObjectRequest
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(filename)
					.build();
			//Generate the Presigned Url
			PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(
					p -> p
							.signatureDuration(Duration.ofMinutes(expiryMins))
							.getObjectRequest(getObjectRequest)
			);
			log.info("Presigned URL successfully created.");
			return presignedGetObjectRequest.url().toString();
		}
		catch (S3Exception e) {
			log.error("AWS S3 Presign Error: " + e.awsErrorDetails().errorMessage());
			throw new FileStorageException("Failed to get a presigned Url: " + e.getMessage());
		}
		catch (Exception e){
			throw new FileStorageException("An unexpected error occurred");
		}
	}

	@Auditable(action = "PDF uploaded")
	public String uploadPdfStatement(ByteArrayOutputStream filestream) {
		try {
			//1.Create the upload request
			String filename = UUID.randomUUID().toString().replace("-", "") + ".pdf";
			System.out.println("bucketname"+bucketName);
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(filename)
					.contentType(MediaType.APPLICATION_PDF_VALUE)
					.build();

			//2.Upload the statement
			PutObjectResponse putObjectResponse = s3Client.putObject(
					putObjectRequest,
					RequestBody.fromBytes(filestream.toByteArray()));

			log.info("File successfully uploaded.");
			return filename;
		}
		catch (S3Exception e) {
			log.error("AWS S3 Client Error: " + e.awsErrorDetails().errorMessage());
			throw new FileStorageException("Failed to upload to S3: " + e.getMessage());
		}
		catch (Exception e){
			throw new FileStorageException("An unexpected error occurred during upload");
		}
	}
}
