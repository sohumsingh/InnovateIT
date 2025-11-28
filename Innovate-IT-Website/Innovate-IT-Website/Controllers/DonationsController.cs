using Microsoft.AspNetCore.Mvc;
using UmbiloTemple.Models;

namespace UmbiloTemple.Controllers
{
    public class DonationsController : Controller
    {
        public IActionResult Index()
        {
            var donationInfo = new DonationInfo
            {
                BankName = "Nedbank",
                AccountHolder = "UMBILO SHREE AMBALAVAANAR ALAYAM",
                AccountNumber = "1304036316",
                BranchCode = "198765",
                Reference = "Name + Prayer Type"
            };

            return View(donationInfo);
        }
    }
}
