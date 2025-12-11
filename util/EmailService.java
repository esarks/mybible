package com.mybible.util;

import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.net.ssl.*;

/**
 * EmailService - Sends verification emails for MyBible
 *
 * Uses raw SMTP over TLS (no external dependencies like JavaMail required).
 * Supports Gmail SMTP with App Password authentication.
 * In mock mode (enabled=false), emails are logged to console instead of sent.
 */
public class EmailService {

    // SMTP Configuration
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private String username = "";
    private String password = "";
    private String fromAddress = "noreply@mybible.app";
    private String fromName = "MyBible";
    private boolean enabled = false;
    private boolean useTLS = true;

    // Singleton instance
    private static EmailService instance;

    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    public EmailService() {
        // Default constructor - use setters to configure
    }

    /**
     * Configure the email service
     */
    public void configure(String smtpHost, int smtpPort, String username,
                          String password, String fromAddress, String fromName, boolean enabled) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.enabled = enabled;

        System.out.println("[EmailService] Configured: host=" + smtpHost + ", port=" + smtpPort +
                          ", from=" + fromAddress + ", enabled=" + enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Send a verification code email
     *
     * @param toEmail The recipient email address
     * @param verificationCode The 6-digit verification code
     * @return true if email sent successfully (or mock mode), false on error
     */
    public boolean sendVerificationEmail(String toEmail, String verificationCode) {
        if (!enabled) {
            // Mock mode - just log to console
            System.out.println("========================================");
            System.out.println("[EMAIL MOCK] Would send to: " + toEmail);
            System.out.println("[EMAIL MOCK] Verification Code: " + verificationCode);
            System.out.println("========================================");
            return true;
        }

        try {
            String subject = "Your MyBible Verification Code";
            String htmlBody = buildVerificationEmailHtml(verificationCode);
            String textBody = "Your MyBible verification code is: " + verificationCode +
                             "\n\nThis code expires in 15 minutes.\n\nIf you didn't request this, please ignore this email.";

            return sendEmail(toEmail, subject, htmlBody, textBody);
        } catch (Exception e) {
            System.err.println("[EmailService] Error sending verification email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Send an email using raw SMTP over TLS (STARTTLS)
     * Works with Gmail SMTP using App Passwords
     */
    public boolean sendEmail(String toEmail, String subject, String htmlBody, String textBody) {
        if (!enabled) {
            System.out.println("[EmailService] Email disabled - not sending to " + toEmail);
            return true;
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            System.err.println("[EmailService] SMTP credentials not configured");
            return false;
        }

        Socket socket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            System.out.println("[EmailService] Connecting to " + smtpHost + ":" + smtpPort);

            // Connect to SMTP server
            socket = new Socket(smtpHost, smtpPort);
            socket.setSoTimeout(30000); // 30 second timeout
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // Read server greeting
            String response = reader.readLine();
            System.out.println("[SMTP] Server: " + response);
            if (!response.startsWith("220")) {
                throw new Exception("SMTP server did not respond with 220: " + response);
            }

            // Send EHLO
            sendCommand(writer, reader, "EHLO localhost", "250");

            // Start TLS
            if (useTLS) {
                sendCommand(writer, reader, "STARTTLS", "220");

                // Upgrade to TLS
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(
                    socket, smtpHost, smtpPort, true);
                sslSocket.startHandshake();

                // Replace streams with SSL streams
                reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
                socket = sslSocket;

                // Send EHLO again after TLS
                sendCommand(writer, reader, "EHLO localhost", "250");
            }

            // Authenticate using AUTH LOGIN
            sendCommand(writer, reader, "AUTH LOGIN", "334");
            sendCommand(writer, reader, Base64.getEncoder().encodeToString(username.getBytes()), "334");
            sendCommand(writer, reader, Base64.getEncoder().encodeToString(password.getBytes()), "235");

            // Send email
            sendCommand(writer, reader, "MAIL FROM:<" + fromAddress + ">", "250");
            sendCommand(writer, reader, "RCPT TO:<" + toEmail + ">", "250");
            sendCommand(writer, reader, "DATA", "354");

            // Build email content with MIME for HTML
            String boundary = "----=_Part_" + System.currentTimeMillis();
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("From: ").append(fromName).append(" <").append(fromAddress).append(">\r\n");
            emailContent.append("To: ").append(toEmail).append("\r\n");
            emailContent.append("Subject: ").append(subject).append("\r\n");
            emailContent.append("MIME-Version: 1.0\r\n");
            emailContent.append("Content-Type: multipart/alternative; boundary=\"").append(boundary).append("\"\r\n");
            emailContent.append("\r\n");

            // Plain text part
            emailContent.append("--").append(boundary).append("\r\n");
            emailContent.append("Content-Type: text/plain; charset=UTF-8\r\n");
            emailContent.append("\r\n");
            emailContent.append(textBody).append("\r\n");

            // HTML part
            emailContent.append("--").append(boundary).append("\r\n");
            emailContent.append("Content-Type: text/html; charset=UTF-8\r\n");
            emailContent.append("\r\n");
            emailContent.append(htmlBody).append("\r\n");

            emailContent.append("--").append(boundary).append("--\r\n");
            emailContent.append(".\r\n");

            // Send the email content
            writer.write(emailContent.toString());
            writer.flush();

            response = reader.readLine();
            System.out.println("[SMTP] Server: " + response);
            if (!response.startsWith("250")) {
                throw new Exception("Failed to send email data: " + response);
            }

            // Quit
            sendCommand(writer, reader, "QUIT", "221");

            System.out.println("[EmailService] Email sent successfully to " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    /**
     * Send an SMTP command and verify the response
     */
    private void sendCommand(BufferedWriter writer, BufferedReader reader,
                            String command, String expectedCode) throws Exception {
        // Don't log passwords
        String logCommand = command;
        if (command.length() > 20 && !command.startsWith("EHLO") && !command.startsWith("MAIL")
            && !command.startsWith("RCPT") && !command.startsWith("DATA") && !command.startsWith("QUIT")
            && !command.startsWith("AUTH") && !command.startsWith("STARTTLS")) {
            logCommand = "[CREDENTIALS]";
        }
        System.out.println("[SMTP] Client: " + logCommand);

        writer.write(command + "\r\n");
        writer.flush();

        // Read response (may be multi-line)
        String response;
        StringBuilder fullResponse = new StringBuilder();
        do {
            response = reader.readLine();
            if (response == null) {
                throw new Exception("SMTP server closed connection unexpectedly");
            }
            fullResponse.append(response).append("\n");
            System.out.println("[SMTP] Server: " + response);
        } while (response.length() >= 4 && response.charAt(3) == '-');

        if (!response.startsWith(expectedCode)) {
            throw new Exception("SMTP error - expected " + expectedCode + " but got: " + response);
        }
    }

    /**
     * Build HTML email body for verification code
     */
    private String buildVerificationEmailHtml(String code) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "  <meta charset=\"UTF-8\">\n" +
               "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "</head>\n" +
               "<body style=\"margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5;\">\n" +
               "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5; padding: 40px 0;\">\n" +
               "    <tr>\n" +
               "      <td align=\"center\">\n" +
               "        <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);\">\n" +
               "          <!-- Header -->\n" +
               "          <tr>\n" +
               "            <td style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 12px 12px 0 0;\">\n" +
               "              <h1 style=\"margin: 0; color: #ffffff; font-size: 28px;\">MyBible</h1>\n" +
               "              <p style=\"margin: 8px 0 0 0; color: rgba(255,255,255,0.9); font-size: 14px;\">Personal Bible Study Application</p>\n" +
               "            </td>\n" +
               "          </tr>\n" +
               "          <!-- Content -->\n" +
               "          <tr>\n" +
               "            <td style=\"padding: 40px 30px;\">\n" +
               "              <h2 style=\"margin: 0 0 20px 0; color: #333; font-size: 22px;\">Verify Your Email</h2>\n" +
               "              <p style=\"margin: 0 0 30px 0; color: #666; font-size: 16px; line-height: 1.5;\">\n" +
               "                Use the verification code below to complete your registration.\n" +
               "              </p>\n" +
               "              <!-- Code Box -->\n" +
               "              <div style=\"background-color: #f8f9fa; border-radius: 8px; padding: 25px; text-align: center; margin-bottom: 30px;\">\n" +
               "                <p style=\"margin: 0 0 10px 0; color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;\">Your Verification Code</p>\n" +
               "                <div style=\"font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #667eea;\">" + code + "</div>\n" +
               "              </div>\n" +
               "              <p style=\"margin: 0; color: #999; font-size: 14px;\">\n" +
               "                This code will expire in <strong>15 minutes</strong>.\n" +
               "              </p>\n" +
               "              <p style=\"margin: 20px 0 0 0; color: #999; font-size: 14px;\">\n" +
               "                If you didn't request this code, you can safely ignore this email.\n" +
               "              </p>\n" +
               "            </td>\n" +
               "          </tr>\n" +
               "          <!-- Footer -->\n" +
               "          <tr>\n" +
               "            <td style=\"padding: 20px 30px; border-top: 1px solid #eee; text-align: center;\">\n" +
               "              <p style=\"margin: 0; color: #999; font-size: 12px;\">\n" +
               "                &copy; 2025 MyBible. All rights reserved.\n" +
               "              </p>\n" +
               "            </td>\n" +
               "          </tr>\n" +
               "        </table>\n" +
               "      </td>\n" +
               "    </tr>\n" +
               "  </table>\n" +
               "</body>\n" +
               "</html>";
    }

    // Getters and setters
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public boolean isUseTLS() { return useTLS; }
    public void setUseTLS(boolean useTLS) { this.useTLS = useTLS; }
}
