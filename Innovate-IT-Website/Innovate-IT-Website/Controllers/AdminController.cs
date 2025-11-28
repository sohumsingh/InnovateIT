using Microsoft.AspNetCore.Mvc;

namespace UmbiloTemple.Controllers
{
    public class AdminController : Controller
    {
        private bool IsAdmin()
        {
            var role = HttpContext.Session.GetString("UserRole");
            return role != null && role.Equals("admin", StringComparison.OrdinalIgnoreCase);
        }

        public IActionResult Index()
        {
            if (!IsAdmin())
            {
                TempData["Error"] = "Access denied. Admins only.";
                return RedirectToAction("Login", "Account");
            }

            return View();
        }
    }
}
