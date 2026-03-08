using CostAccounting.API.Data;
using CostAccounting.API.Data.Seed;
using CostAccounting.API.Features.Categories;
using CostAccounting.API.Features.Categories.Create;
using CostAccounting.API.Features.Categories.Delete;
using CostAccounting.API.Features.Categories.GetAll;
using CostAccounting.API.Features.Categories.GetById;
using CostAccounting.API.Features.Categories.Update;
using CostAccounting.API.Features.Reports;
using CostAccounting.API.Features.Reports.ByCategory;
using CostAccounting.API.Features.Reports.Summary;
using CostAccounting.API.Features.Transactions;
using CostAccounting.API.Features.Transactions.Create;
using CostAccounting.API.Features.Transactions.Delete;
using CostAccounting.API.Features.Transactions.GetAll;
using CostAccounting.API.Features.Transactions.GetById;
using CostAccounting.API.Features.Transactions.Update;
using Microsoft.EntityFrameworkCore;
using System.Text.Json.Serialization;

var builder = WebApplication.CreateBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
});

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var connectionString = BuildConnectionString(builder.Configuration);

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

builder.Services.AddScoped<CreateCategoryHandler>();
builder.Services.AddScoped<GetAllCategoriesHandler>();
builder.Services.AddScoped<GetCategoryByIdHandler>();
builder.Services.AddScoped<UpdateCategoryHandler>();
builder.Services.AddScoped<DeleteCategoryHandler>();

builder.Services.AddScoped<CreateTransactionHandler>();
builder.Services.AddScoped<GetAllTransactionsHandler>();
builder.Services.AddScoped<GetTransactionByIdHandler>();
builder.Services.AddScoped<UpdateTransactionHandler>();
builder.Services.AddScoped<DeleteTransactionHandler>();

builder.Services.AddScoped<GetSummaryReportHandler>();
builder.Services.AddScoped<GetReportByCategoryHandler>();

builder.Services.AddHealthChecks()
    .AddDbContextCheck<AppDbContext>();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();

app.MapGet("/", () => Results.Redirect("/swagger"));

app.MapHealthChecks("/health");

app.MapGet("/node", (IConfiguration configuration) =>
{
    var nodeName = configuration["APP_NODE_NAME"] ?? Environment.MachineName;

    return Results.Ok(new
    {
        nodeName,
        machineName = Environment.MachineName
    });
})
.WithTags("Technical");

app.MapCategoryEndpoints();
app.MapTransactionEndpoints();
app.MapReportEndpoints();

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