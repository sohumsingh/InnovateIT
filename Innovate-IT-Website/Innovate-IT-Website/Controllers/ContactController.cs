using Microsoft.AspNetCore.Mvc;
using UmbiloTemple.Models;
using UmbiloTemple.Services;

namespace UmbiloTemple.Controllers
{
    public class ContactController : Controller
    {
        private readonly ContactService _contactService;
        private readonly EmailService _emailService;

        public ContactController(ContactService contactService, EmailService emailService)
        {
            _contactService = contactService;
            _emailService = emailService;
        }

        [HttpGet]
        public IActionResult Index() => View();

        [HttpPost]
        public async Task<IActionResult> Index(ContactMessage contactMessage)
        {
            if (!ModelState.IsValid)
            {
                TempData["Error"] = "Please complete all fields correctly.";
                return View(contactMessage);
            }

            // Ensure UTC timestamp for Firestore
            contactMessage.SentAt = DateTime.UtcNow;

            await _contactService.SaveMessageAsync(contactMessage);

            string subject = $"📩 New Contact Message from {contactMessage.Name}";
            string body = $@"
                <h2>New Contact Message</h2>
                <p><strong>Name:</strong> {contactMessage.Name}</p>
                <p><strong>Email:</strong> {contactMessage.Email}</p>
                <p><strong>Message:</strong><br/>{contactMessage.Message}</p>
                <p><em>Received on {DateTime.Now:dddd, dd MMM yyyy HH:mm}</em></p>";

            await _emailService.SendEmailAsync(subject, body);

            TempData["Success"] = "Your message has been sent successfully!";
            return RedirectToAction("Index");
        }
    }
}

