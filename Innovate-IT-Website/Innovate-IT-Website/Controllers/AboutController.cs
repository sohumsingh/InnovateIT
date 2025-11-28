using Microsoft.AspNetCore.Mvc;
using UmbiloTemple.Models;

namespace UmbiloTemple.Controllers
{
    public class AboutController : Controller
    {
        public IActionResult Index()
        {
            var info = new TempleInfo();
            return View(info);
        }
    }
}
