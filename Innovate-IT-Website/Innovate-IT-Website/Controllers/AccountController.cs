using Microsoft.AspNetCore.Mvc;
using UmbiloTemple.Models;
using UmbiloTemple.Services;

namespace UmbiloTemple.Controllers
{
    public class AccountController : Controller
    {
        private readonly AuthService _authService;
        private readonly IConfiguration _config;

        public AccountController(AuthService authService, IConfiguration config)
        {
            _authService = authService;
            _config = config;
        }

        [HttpGet]
        public IActionResult Login() => View();
    
        [HttpPost]
        public async Task<IActionResult> Login(string email, string password)
    {
         // Read Admin credentials from appsettings.json
         var adminEmail = _config["AdminCredentials:Email"];
         var adminPassword = _config["AdminCredentials:Password"];

         //  Hardcoded Admin Login
         if (email.Equals(adminEmail, StringComparison.OrdinalIgnoreCase) && password == adminPassword)
       {
          HttpContext.Session.SetString("UserEmail", adminEmail);
          HttpContext.Session.SetString("UserRole", "admin");
          TempData["Success"] = "Welcome, Admin!";
          return RedirectToAction("Index", "Admin"); // Redirect to Admin Dashboard
       }

         // Normal Firestore user login
         var user = await _authService.ValidateUser(email, password);
         if (user != null)
       {
        HttpContext.Session.SetString("UserEmail", user.Email);
        HttpContext.Session.SetString("UserRole", user.Role ?? "user");
        TempData["Success"] = "Login successful!";
        return RedirectToAction("Index", "Home"); // Normal users go to Home
     }

        //  Invalid credentials
          ViewBag.Error = "Invalid email or password.";
          return View();
  }

         
        [HttpGet]
        public IActionResult Register() => View();

        [HttpPost]
        public async Task<IActionResult> Register(string name, string email, string password)
        {
            var newUser = new User { Name = name, Email = email, PasswordHash = password };
            bool registered = await _authService.RegisterUser(newUser);

            if (registered)
            {
                TempData["Success"] = "Registration successful! Please log in.";
                return RedirectToAction("Login");
            }

            ViewBag.Error = "Email already exists.";
            return View();
        }

        public IActionResult Logout()
        {
            HttpContext.Session.Clear();
            return RedirectToAction("Login");
        }
    }
}
