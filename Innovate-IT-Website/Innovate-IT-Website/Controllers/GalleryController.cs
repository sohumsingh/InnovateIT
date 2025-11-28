using Microsoft.AspNetCore.Mvc;
using Google.Cloud.Firestore;
using UmbiloTemple.Models;

namespace UmbiloTemple.Controllers
{
    public class GalleryController : Controller
    {
        private readonly FirestoreDb _firestore;

        public GalleryController(FirestoreDb firestore)
        {
            _firestore = firestore;
        }

        public async Task<IActionResult> Index()
        {
            try
            {
                var snapshot = await _firestore.Collection("Gallery").GetSnapshotAsync();
                var images = snapshot.Documents.Select(d => d.ConvertTo<GalleryImage>()).ToList();
                return View(images);
            }
            catch (Exception ex)
            {
                ViewBag.Error = "Could not load gallery images: " + ex.Message;
                return View(new List<GalleryImage>());
            }
        }
    }
}
