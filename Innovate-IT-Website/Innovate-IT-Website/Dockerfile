# ---------- Build & Publish ----------
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src
COPY *.csproj .
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /app/publish

# ---------- Run ----------
# Use SDK image instead of runtime (Render doesn't fully support aspnet:9.0 yet)
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS final
WORKDIR /app
COPY --from=build /app/publish .

# Ensure gRPC/Firestore native libs exist
RUN apt-get update && apt-get install -y libc6 libgcc1 libgssapi-krb5-2 libicu72 libssl3 libstdc++6 zlib1g

ENV ASPNETCORE_URLS=http://*:8080
EXPOSE 8080

ENTRYPOINT ["dotnet", "UmbiloTemple.dll"]
