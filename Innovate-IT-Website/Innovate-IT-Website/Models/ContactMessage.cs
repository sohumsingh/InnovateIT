using Google.Cloud.Firestore;

namespace UmbiloTemple.Models
{
    [FirestoreData]
    public class ContactMessage
    {
        [FirestoreProperty] public string Id { get; set; } = Guid.NewGuid().ToString();
        [FirestoreProperty] public string Name { get; set; }
        [FirestoreProperty] public string Email { get; set; }
        [FirestoreProperty] public string Message { get; set; }
        [FirestoreProperty] public DateTime SentAt { get; set; } = DateTime.UtcNow;
    }
}
