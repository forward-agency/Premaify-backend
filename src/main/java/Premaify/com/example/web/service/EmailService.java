package Premaify.com.example.web.service;

import Premaify.com.example.web.model.Lead;
import Premaify.com.example.web.model.Product;
import Premaify.com.example.web.repository.ProductRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter SUBMITTED_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final URI BREVO_EMAIL_API_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final JavaMailSender mailSender;
    private final ProductRepository productRepository;
    private final HttpClient httpClient;
    private final String brevoApiKey;
    private final String fromEmail;
    private final String adminEmail;
    private final String siteBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            ProductRepository productRepository,
            @Value("${brevo.api.key:}") String brevoApiKey,
            @Value("${premaify.mail.from:}") String fromEmail,
            @Value("${premaify.mail.admin-to:premaify@gmail.com}") String adminEmail,
            @Value("${premaify.site.base-url:https://www.premaify.com}") String siteBaseUrl
    ) {
        this.mailSender = mailSender;
        this.productRepository = productRepository;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.brevoApiKey = brevoApiKey;
        this.fromEmail = fromEmail;
        this.adminEmail = adminEmail;
        this.siteBaseUrl = siteBaseUrl;
    }

    public void sendOrderNotification(Lead lead) {
        sendNotification("New Laptop Order Received", buildOrderBody(lead), lead.getId());
    }

    public void sendEnquiryNotification(Lead lead) {
        sendNotification("New Customer Enquiry Received", buildEnquiryBody(lead), lead.getId());
    }

    private void sendNotification(String subject, String body, Long leadId) {
        try {
            if (fromEmail == null || fromEmail.isBlank()) {
                logger.warn("Skipping admin email for lead {} because premaify.mail.from is not configured", leadId);
                return;
            }

            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                sendWithBrevoApi(subject, body, leadId);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            logger.info("Admin email notification sent for lead {} to {}", leadId, adminEmail);
        } catch (Exception exception) {
            logger.error("Unable to send admin email notification for lead {}", leadId, exception);
        }
    }

    private void sendWithBrevoApi(String subject, String body, Long leadId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(BREVO_EMAIL_API_URI)
                .timeout(Duration.ofSeconds(15))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildBrevoPayload(subject, body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Brevo API email failed with status " + response.statusCode() + ": " + response.body());
        }
        logger.info("Admin email notification sent for lead {} to {} using Brevo API", leadId, adminEmail);
    }

    private String buildBrevoPayload(String subject, String body) {
        return "{"
                + "\"sender\":{\"name\":\"Premaify\",\"email\":\"" + jsonEscape(fromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + jsonEscape(adminEmail) + "\"}],"
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"htmlContent\":\"" + jsonEscape(body) + "\""
                + "}";
    }

    private String buildOrderBody(Lead lead) {
        Product product = findProduct(lead);
        return buildHtmlEmail(lead, product, "New Laptop Order Received", true);
    }

    private String buildEnquiryBody(Lead lead) {
        Product product = findProduct(lead);
        return buildHtmlEmail(lead, product, "New Customer Enquiry Received", false);
    }

    private Product findProduct(Lead lead) {
        if (lead.getProductId() == null || lead.getProductId().isBlank()) {
            return null;
        }
        Optional<Product> product = productRepository.findById(lead.getProductId());
        return product.orElse(null);
    }

    private String buildHtmlEmail(Lead lead, Product product, String title, boolean includeAddress) {
        String imageUrl = product == null ? "" : safe(product.getImageUrl());
        String productLink = productLink(lead);
        String whatsappLink = whatsappLink(lead);

        StringBuilder body = new StringBuilder();
        body.append("<div style=\"font-family:Arial,sans-serif;color:#061427;line-height:1.5;max-width:680px\">");
        if (!"Not shared".equals(imageUrl)) {
            body.append("<img src=\"").append(escape(imageUrl)).append("\" alt=\"")
                    .append(escape(safe(lead.getLaptop())))
                    .append("\" style=\"width:100%;max-width:520px;border-radius:18px;margin-bottom:18px;display:block\"/>");
        }
        body.append("<h2 style=\"margin:0 0 16px;font-size:24px\">").append(escape(title)).append("</h2>");
        body.append("<table style=\"width:100%;border-collapse:collapse\">");
        appendRow(body, "Customer Name", lead.getName());
        appendRow(body, "Phone Number", lead.getPhone());
        appendRow(body, "Product Name", lead.getLaptop());
        appendRow(body, "District", lead.getDistrict());
        if (includeAddress) {
            appendRow(body, "Full Address", lead.getFullAddress());
            appendRow(body, "Pincode", lead.getPincode());
        }
        appendRow(body, "Submitted Time", submittedTime(lead));
        body.append("</table>");
        body.append("<div style=\"margin-top:22px;display:flex;gap:10px;flex-wrap:wrap\">");
        body.append("<a href=\"").append(escape(productLink)).append("\" style=\"background:#075de7;color:#fff;text-decoration:none;padding:12px 16px;border-radius:12px;font-weight:700\">View Product</a>");
        body.append("<a href=\"").append(escape(whatsappLink)).append("\" style=\"background:#13a884;color:#fff;text-decoration:none;padding:12px 16px;border-radius:12px;font-weight:700\">WhatsApp Customer</a>");
        body.append("</div>");
        body.append("<p style=\"margin-top:18px;color:#667085;font-size:13px\">Product Link: <a href=\"").append(escape(productLink)).append("\">").append(escape(productLink)).append("</a></p>");
        body.append("<p style=\"margin-top:6px;color:#667085;font-size:13px\">Customer WhatsApp: <a href=\"").append(escape(whatsappLink)).append("\">").append(escape(whatsappLink)).append("</a></p>");
        body.append("</div>");
        return body.toString();
    }

    private void appendRow(StringBuilder body, String label, String value) {
        body.append("<tr>")
                .append("<td style=\"padding:10px 12px;border:1px solid #eef2f6;background:#f8fafc;font-weight:700;width:180px\">")
                .append(escape(label))
                .append("</td>")
                .append("<td style=\"padding:10px 12px;border:1px solid #eef2f6\">")
                .append(escape(safe(value)))
                .append("</td>")
                .append("</tr>");
    }

    private String productLink(Lead lead) {
        if (lead.getProductId() == null || lead.getProductId().isBlank()) {
            return stripTrailingSlash(siteBaseUrl) + "/products";
        }
        return stripTrailingSlash(siteBaseUrl) + "/products?product=" + encode(lead.getProductId());
    }

    private String whatsappLink(Lead lead) {
        String phone = safe(lead.getPhone()).replaceAll("[^0-9]", "");
        if (phone.length() == 10) {
            phone = "91" + phone;
        }
        String message = "Hi " + safe(lead.getName()) + ", this is Premaify regarding your laptop request for " + safe(lead.getLaptop()) + ".";
        return "https://wa.me/" + phone + "?text=" + encode(message);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://www.premaify.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String jsonEscape(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String submittedTime(Lead lead) {
        if (lead.getCreatedAt() == null) {
            return "Not available";
        }
        return lead.getCreatedAt().format(SUBMITTED_TIME_FORMAT);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not shared" : value;
    }
}
