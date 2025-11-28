# Firebase Security Rules Setup Guide

## Files Created
- `firestore.rules` - Firestore Database Security Rules
- `storage.rules` - Firebase Storage Security Rules

## How to Deploy These Rules

### Option 1: Using Firebase Console (Recommended for Beginners)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to Firestore Database > Rules tab
4. Copy and paste the contents of `firestore.rules` file
5. Click "Publish"
6. Navigate to Storage > Rules tab
7. Copy and paste the contents of `storage.rules` file
8. Click "Publish"

### Option 2: Using Firebase CLI (Advanced)

If you have Firebase CLI installed, run:

```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase (if not already done)
firebase init firestore
firebase init storage

# Deploy rules
firebase deploy --only firestore:rules
firebase deploy --only storage:rules
```

## What These Rules Do

### Firestore Rules
- **Users**: Authenticated users can read and write user data
- **Events**: Authenticated users can read, admins can write
- **Bank Accounts**: Authenticated users can read, admins can write
- **Bookings**: Authenticated users can read their own bookings and create new ones
- **Documents**: Users can create documents, authenticated users can read all

### Storage Rules
- **Documents Folder**: Authenticated users can upload and download
- **Gallery Folder**: Authenticated users can upload and download
- **All Files**: Any authenticated user can read/write

## Important Security Notes

⚠️ **Warning**: The current rules allow all authenticated users to write to most collections. This is for development/testing purposes only.

### For Production, Consider:
1. **Restrict writes to admin-only** for sensitive data
2. **Add validation** to prevent unauthorized modifications
3. **Use custom claims** for role-based access control
4. **Implement field-level security** for sensitive data

### Example Production Rules:

```javascript
// Only admins can write to events
allow write: if request.auth != null && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isAdmin == true;

// Users can only modify their own data
allow update: if request.auth.uid == userId;
```

## Testing the Rules

After deploying, test your app to ensure:
1. ✅ Users can register and login
2. ✅ Users can view events and bookings
3. ✅ Users can upload documents and gallery media
4. ✅ Admins can manage all data
5. ❌ Unauthenticated users cannot access data

## Troubleshooting

If you're getting permission errors:

1. **Check Authentication**: Ensure users are logged in
2. **Verify Rules**: Check Firebase Console > Rules to see active rules
3. **Check Logs**: Look at Cloud Functions logs for detailed error messages
4. **Test in Simulator**: Use Firebase Console > Rules > Simulator

## Current Configuration Summary

Your Firebase is currently configured to:
- ✅ Allow all authenticated users to read data
- ✅ Allow authenticated users to write data
- ✅ Allow authenticated users to upload files to Storage
- ❌ Does NOT allow unauthenticated access (secure)

This configuration works for development but should be tightened for production!

