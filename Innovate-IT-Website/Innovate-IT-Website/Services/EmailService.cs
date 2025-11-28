using System.Net;
using System.Net.Mail;
using Microsoft.Extensions.Configuration;

namespace UmbiloTemple.Services
{
    public class EmailService
    {
        private readonly IConfiguration _config;
        public EmailService(IConfiguration config) => _config = config;

        public async Task SendEmailAsync(string subject, string body)
        {
            // Read values from appsettings.json
            var smtpServer = _config["EmailSettings:SmtpServer"];
            var smtpPort = int.Parse(_config["EmailSettings:SmtpPort"] ?? "587");
            var senderName = _config["EmailSettings:SenderName"];
            var senderEmail = _config["EmailSettings:SenderEmail"];
            var senderPassword = _config["EmailSettings:SenderPassword"];

            // Simple null check to prevent silent failure
            if (string.IsNullOrEmpty(senderEmail) || string.IsNullOrEmpty(senderPassword))
                throw new InvalidOperationException("Sender email or password missing from configuration.");

            var smtpClient = new SmtpClient(smtpServer)
            {
                Port = smtpPort,
                Credentials = new NetworkCredential(senderEmail, senderPassword),
                EnableSsl = true
            };

            var message = new MailMessage
            {
                From = new MailAddress(senderEmail, senderName),
                Subject = subject,
                Body = body,
                IsBodyHtml = true
            };

            message.To.Add("umbilotemple.app@gmail.com"); // recipient (admin)

            await smtpClient.SendMailAsync(message);
        }
    }
}
