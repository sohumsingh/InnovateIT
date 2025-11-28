using Microsoft.AspNetCore.Mvc;
using Google.Cloud.Firestore;
using UmbiloTemple.Models;

namespace UmbiloTemple.Controllers
{
    public class EventsController : Controller
    {
        private readonly FirestoreDb _firestore;

        public EventsController(FirestoreDb firestore)
        {
            _firestore = firestore;
        }

        public async Task<IActionResult> Index()
        {
            try
            {
                var snapshot = await _firestore.Collection("Events").GetSnapshotAsync();
                var events = snapshot.Documents.Select(d => d.ConvertTo<Event>()).ToList();

                // Order by date (upcoming first)
                events = events.OrderBy(e => e.Date).ToList();
                return View(events);
            }
            catch (Exception ex)
            {
                ViewBag.Error = "Could not load events: " + ex.Message;
                return View(new List<Event>());
            }
        }
    }
}
