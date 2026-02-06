package za.co.cbank.securefilestatementdelivery.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import za.co.cbank.securefilestatementdelivery.Audit.annotation.Auditable;
import za.co.cbank.securefilestatementdelivery.exception.FileCreationException;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCreationService {

	@Auditable(action = "PDF Statement generated")
	public ByteArrayOutputStream GeneratePdfFile(String pdfData,String secretKey){
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			PDDocument document = new PDDocument();
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.withHtmlContent(pdfData,"/");
			builder.usePDDocument(document);
			builder.buildPdfRenderer().createPDFWithoutClosing();
			AccessPermission permission = new AccessPermission();
			permission.setCanPrint(true);
			permission.setCanModify(false);

			String masterSecretKey = "${config.pdf.encryption.master_secret_key}";
			StandardProtectionPolicy policy = new StandardProtectionPolicy(
					masterSecretKey,
					secretKey,
					permission
			);
			policy.setEncryptionKeyLength(256);
			document.protect(policy);
			document.save(outputStream);
			log.info("PDF file created successfully.");
			return outputStream;

		}
		catch (IOException e) {
			log.error("PDF Generation Error: " + e.getMessage());
			throw new FileCreationException("Failed to get a PDF file: " + e.getMessage());
		}
		catch (Exception e){
			throw new FileCreationException("An unexpected error occurred");
		}
	}
}
