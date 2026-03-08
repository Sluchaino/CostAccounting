using CostAccounting.API.Domain;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Data.Seed
{
    public static class DbSeeder
    {
        public static async Task SeedAsync(AppDbContext db, CancellationToken cancellationToken = default)
        {
            if (await db.Categories.AnyAsync(cancellationToken))
                return;

            var now = DateTime.UtcNow;

            var categories = new[]
            {
            new Category
            {
                Id = Guid.NewGuid(),
                Name = "Еда",
                Type = CategoryType.Expense,
                CreatedAt = now
            },
            new Category
            {
                Id = Guid.NewGuid(),
                Name = "Транспорт",
                Type = CategoryType.Expense,
                CreatedAt = now
            },
            new Category
            {
                Id = Guid.NewGuid(),
                Name = "Подписки",
                Type = CategoryType.Expense,
                CreatedAt = now
            },
            new Category
            {
                Id = Guid.NewGuid(),
                Name = "Развлечения",
                Type = CategoryType.Expense,
                CreatedAt = now
            },
            new Category
            {
                Id = Guid.NewGuid(),
                Name = "Зарплата",
                Type = CategoryType.Income,
                CreatedAt = now
            }
        };

            await db.Categories.AddRangeAsync(categories, cancellationToken);
            await db.SaveChangesAsync(cancellationToken);
        }
    }
}
