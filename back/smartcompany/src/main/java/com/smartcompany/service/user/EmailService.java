package com.smartcompany.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.smartcompany.model.company.CompanyStatus;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ─── Aktivacioni email ────────────────────────────────────────────────────

    public void sendActivationEmail(String toEmail, String activationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(toEmail);
            helper.setSubject("Activate your Smart Company account");
            helper.setText("""
                <html>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        
                        <!-- Header -->
                        <tr>
                          <td style="background:#4f46e5;padding:32px 40px;text-align:center;">
                            <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">Smart Company</h1>
                          </td>
                        </tr>
                        
                        <!-- Body -->
                        <tr>
                          <td style="padding:40px;">
                            <h2 style="color:#1a1a2e;margin:0 0 16px 0;font-size:20px;">Activate your account</h2>
                            <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 24px 0;">
                              Thank you for registering with Smart Company! Please click the button below to activate your account.
                            </p>
                            <table cellpadding="0" cellspacing="0" width="100%%">
                              <tr>
                                <td align="center" style="padding:8px 0 32px 0;">
                                  <a href="%s"
                                     style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;padding:14px 36px;border-radius:6px;font-size:15px;font-weight:bold;">
                                    Activate Account
                                  </a>
                                </td>
                              </tr>
                            </table>
                            <p style="color:#888;font-size:13px;margin:0;">
                              If you did not create an account, you can safely ignore this email.<br/>
                              The link will expire after use.
                            </p>
                          </td>
                        </tr>
                        
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8f8f8;padding:20px 40px;text-align:center;border-top:1px solid #eeeeee;">
                            <p style="color:#aaa;font-size:12px;margin:0;">© 2025 Smart Company. All rights reserved.</p>
                          </td>
                        </tr>
                        
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(activationLink), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send activation email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── Faktura ──────────────────────────────────────────────────────────────

    public void sendInvoice(String toEmail, byte[] pdfBytes, Long orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(toEmail);
            helper.setSubject("Order #" + orderId + " - Smart Company Invoice");
            helper.setText("""
                <html>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        
                        <!-- Header -->
                        <tr>
                          <td style="background:#4f46e5;padding:32px 40px;text-align:center;">
                            <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">Smart Company</h1>
                          </td>
                        </tr>
                        
                        <!-- Body -->
                        <tr>
                          <td style="padding:40px;">
                            <h2 style="color:#1a1a2e;margin:0 0 16px 0;font-size:20px;">Thank you for your order!</h2>
                            <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 16px 0;">
                              Your order <strong>#%d</strong> has been successfully placed.
                            </p>
                            <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 32px 0;">
                              Please find your invoice attached to this email.
                            </p>
                            <p style="color:#888;font-size:13px;margin:0;">
                              If you have any questions, please contact our support team.
                            </p>
                          </td>
                        </tr>
                        
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8f8f8;padding:20px 40px;text-align:center;border-top:1px solid #eeeeee;">
                            <p style="color:#aaa;font-size:12px;margin:0;">© 2025 Smart Company. All rights reserved.</p>
                          </td>
                        </tr>
                        
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(orderId), true);

            helper.addAttachment("invoice-" + orderId + ".pdf",
                new org.springframework.core.io.ByteArrayResource(pdfBytes),
                "application/pdf");

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send invoice email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error sending invoice: " + e.getMessage(), e);
        }
    }

    // ─── Odluka o registraciji firme ─────────────────────────────────────────

    public void sendCompanyDecisionEmail(String toEmail, String companyName,
                                         CompanyStatus status, String rejectionReason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(toEmail);

            if (status == CompanyStatus.APPROVED) {
                helper.setSubject("Company Registration Approved - Smart Company");
                helper.setText("""
                    <html>
                    <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                        <tr><td align="center">
                          <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                            <tr>
                              <td style="background:#4f46e5;padding:32px 40px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">Smart Company</h1>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:40px;">
                                <table cellpadding="0" cellspacing="0" width="100%%">
                                  <tr>
                                    <td style="padding-bottom:16px;">
                                      <span style="display:inline-block;background:#dcfce7;color:#166534;padding:6px 16px;border-radius:20px;font-size:13px;font-weight:bold;">✓ Approved</span>
                                    </td>
                                  </tr>
                                </table>
                                <h2 style="color:#1a1a2e;margin:0 0 16px 0;font-size:20px;">Your company has been approved!</h2>
                                <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 16px 0;">
                                  Great news! Your company registration request for <strong>%s</strong> has been approved.
                                </p>
                                <p style="color:#555;font-size:15px;line-height:1.6;margin:0;">
                                  You can now use this company to place orders on Smart Company.
                                </p>
                              </td>
                            </tr>
                            <tr>
                              <td style="background:#f8f8f8;padding:20px 40px;text-align:center;border-top:1px solid #eeeeee;">
                                <p style="color:#aaa;font-size:12px;margin:0;">© 2025 Smart Company. All rights reserved.</p>
                              </td>
                            </tr>
                          </table>
                        </td></tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(companyName), true);
            } else {
                helper.setSubject("Company Registration Rejected - Smart Company");
                helper.setText("""
                    <html>
                    <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                        <tr><td align="center">
                          <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                            <tr>
                              <td style="background:#4f46e5;padding:32px 40px;text-align:center;">
                                <h1 style="color:#ffffff;margin:0;font-size:24px;letter-spacing:1px;">Smart Company</h1>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:40px;">
                                <table cellpadding="0" cellspacing="0" width="100%%">
                                  <tr>
                                    <td style="padding-bottom:16px;">
                                      <span style="display:inline-block;background:#fee2e2;color:#991b1b;padding:6px 16px;border-radius:20px;font-size:13px;font-weight:bold;">✗ Rejected</span>
                                    </td>
                                  </tr>
                                </table>
                                <h2 style="color:#1a1a2e;margin:0 0 16px 0;font-size:20px;">Company registration not approved</h2>
                                <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 16px 0;">
                                  Unfortunately, your company registration request for <strong>%s</strong> has been rejected.
                                </p>
                                <div style="background:#fff8f8;border-left:4px solid #ef4444;padding:16px;border-radius:4px;margin:0 0 24px 0;">
                                  <p style="color:#555;font-size:14px;margin:0;"><strong>Reason:</strong> %s</p>
                                </div>
                                <p style="color:#888;font-size:13px;margin:0;">
                                  If you believe this is a mistake, please contact our support team.
                                </p>
                              </td>
                            </tr>
                            <tr>
                              <td style="background:#f8f8f8;padding:20px 40px;text-align:center;border-top:1px solid #eeeeee;">
                                <p style="color:#aaa;font-size:12px;margin:0;">© 2025 Smart Company. All rights reserved.</p>
                              </td>
                            </tr>
                          </table>
                        </td></tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(companyName, rejectionReason), true);
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send company decision email to {}: {}", toEmail, e.getMessage());
        }
    }
}