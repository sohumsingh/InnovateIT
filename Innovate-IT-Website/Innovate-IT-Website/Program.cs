using Google.Apis.Auth.OAuth2;
using Google.Cloud.Firestore;
using Google.Cloud.Firestore.V1;
using Grpc.Auth;
using UmbiloTemple.Services;

var builder = WebApplication.CreateBuilder(args);

var firebaseJson = Environment.GetEnvironmentVariable("FIREBASE_KEY_JSON");
GoogleCredential credential;

if (!string.IsNullOrEmpty(firebaseJson))
{
    credential = GoogleCredential.FromJson(firebaseJson);
}
else
{
    var localPath = Path.Combine(builder.Environment.ContentRootPath, "Firestore/temple-firebase-key.json");
    credential = GoogleCredential.FromFile(localPath);
}

var firestoreClient = new FirestoreClientBuilder
{
    ChannelCredentials = credential.ToChannelCredentials()
}.Build();

var firestoreDb = FirestoreDb.Create("umbilotemple-f8c8f", firestoreClient);

builder.Services.AddSingleton(firestoreDb);
builder.Services.AddControllersWithViews();

builder.Services.AddSession(options =>
{
    options.IdleTimeout = TimeSpan.FromHours(1);
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
});

builder.Services.AddScoped<AuthService>();
builder.Services.AddScoped<ContactService>();
builder.Services.AddScoped<EmailService>();

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseSession();
app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();
