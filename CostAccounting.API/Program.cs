using CostAccounting.API.Data;
using CostAccounting.API.Data.Seed;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var connectionString = BuildConnectionString(builder.Configuration);

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

builder.Services.AddHealthChecks()
    .AddDbContextCheck<AppDbContext>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapHealthChecks("/health");

app.MapGet("/node", (IConfiguration configuration) =>
{
    var nodeName = configuration["APP_NODE_NAME"] ?? Environment.MachineName;

    return Results.Ok(new
    {
        nodeName,
        machineName = Environment.MachineName
    });
});

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    await db.Database.MigrateAsync();
    await DbSeeder.SeedAsync(db);
}

app.Run();

static string BuildConnectionString(IConfiguration configuration)
{
    var host = configuration["DB_HOST"] ?? "localhost";
    var port = configuration["DB_PORT"] ?? "5432";
    var database = configuration["DB_NAME"] ?? "CostAccounting";
    var username = configuration["DB_USER"] ?? "postgres";
    var password = configuration["DB_PASSWORD"] ?? "777buba777";

    return $"Host={host};Port={port};Database={database};Username={username};Password={password}";
}

public partial class Program { }