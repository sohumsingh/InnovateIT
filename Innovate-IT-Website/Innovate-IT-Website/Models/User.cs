using Google.Cloud.Firestore;

namespace UmbiloTemple.Models
{
    [FirestoreData]
    public class User
    {
        [FirestoreProperty]
        public string Id { get; set; } = Guid.NewGuid().ToString();

        [FirestoreProperty]
        public string Name { get; set; }

        [FirestoreProperty]
        public string Email { get; set; }

        [FirestoreProperty]
        public string PasswordHash { get; set; }

        [FirestoreProperty]
        public string Role { get; set; } = "user"; // "admin" or "user"
    }
}
