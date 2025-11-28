using Google.Cloud.Firestore;

namespace UmbiloTemple.Models
{
    [FirestoreData]
    public class GalleryImage
    {
        [FirestoreProperty]
        public string Id { get; set; } = Guid.NewGuid().ToString();

        [FirestoreProperty]
        public string Title { get; set; }

        [FirestoreProperty]
        public string Description { get; set; }

        [FirestoreProperty]
        public string ImageUrl { get; set; }

        [FirestoreProperty]
        public DateTime UploadedAt { get; set; } = DateTime.UtcNow;
    }
}
