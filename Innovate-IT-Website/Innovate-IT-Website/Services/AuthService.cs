using Google.Cloud.Firestore;
using System.Security.Cryptography;
using System.Text;
using UmbiloTemple.Models;

namespace UmbiloTemple.Services
{
    public class AuthService
    {
        private readonly FirestoreDb _firestore;

        public AuthService(FirestoreDb firestore)
        {
            _firestore = firestore;
        }

        public async Task<User?> ValidateUser(string email, string password)
        {
            var query = _firestore.Collection("Users").WhereEqualTo("Email", email);
            var snapshot = await query.GetSnapshotAsync();

            foreach (var doc in snapshot.Documents)
            {
                var user = doc.ConvertTo<User>();
                if (user.PasswordHash == HashPassword(password))
                    return user;
            }
            return null;
        }

        public async Task<bool> RegisterUser(User newUser)
        {
            var existing = await _firestore.Collection("Users")
                .WhereEqualTo("Email", newUser.Email)
                .GetSnapshotAsync();

            if (existing.Count > 0)
                return false; // user already exists

            newUser.PasswordHash = HashPassword(newUser.PasswordHash);
            await _firestore.Collection("Users").AddAsync(newUser);
            return true;
        }

        public static string HashPassword(string password)
        {
            using var sha256 = SHA256.Create();
            var bytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(password));
            return BitConverter.ToString(bytes).Replace("-", "").ToLower();
        }
    }
}
