using Google.Cloud.Firestore;

namespace UmbiloTemple.Models
{
    [FirestoreData]
    public class Event
    {
        [FirestoreProperty] public string Id { get; set; }
        [FirestoreProperty] public string Title { get; set; }
        [FirestoreProperty] public string Description { get; set; }
        [FirestoreProperty] public string ImageUrl { get; set; }
        [FirestoreProperty] public DateTime Date { get; set; }     // Firestore requires UTC
        [FirestoreProperty] public string Location { get; set; }
        [FirestoreProperty] public string UploadedBy { get; set; }
        [FirestoreProperty] public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}
