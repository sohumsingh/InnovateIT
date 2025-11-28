using Google.Cloud.Firestore;
using UmbiloTemple.Models;

namespace UmbiloTemple.Services
{
    public class ContactService
    {
        private readonly FirestoreDb _firestore;
        public ContactService(FirestoreDb firestore) => _firestore = firestore;

        public async Task SaveMessageAsync(ContactMessage message)
        {
            var docRef = _firestore.Collection("ContactMessages").Document(message.Id);
            await docRef.SetAsync(message);
        }
    }
}
