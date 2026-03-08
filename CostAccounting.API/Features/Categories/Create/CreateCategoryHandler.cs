using CostAccounting.API.Data;
using CostAccounting.API.Domain;
using CostAccounting.API.Features.Categories.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Categories.Create
{
    public class CreateCategoryHandler
    {
        private readonly AppDbContext _dbContext;

        public CreateCategoryHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(CreateCategoryRequest request, CancellationToken cancellationToken = default)
        {
            if (string.IsNullOrWhiteSpace(request.Name))
            {
                return Results.BadRequest(new { message = "Название категории обязательно." });
            }

            var normalizedName = request.Name.Trim();

            if (normalizedName.Length > 100)
            {
                return Results.BadRequest(new { message = "Название категории не должно превышать 100 символов." });
            }

            var exists = await _dbContext.Categories
                .AnyAsync(x => x.Name.ToLower() == normalizedName.ToLower(), cancellationToken);

            if (exists)
            {
                return Results.Conflict(new { message = "Категория с таким названием уже существует." });
            }

            var category = new Category
            {
                Id = Guid.NewGuid(),
                Name = normalizedName,
                Type = request.Type,
                CreatedAt = DateTime.UtcNow
            };

            _dbContext.Categories.Add(category);
            await _dbContext.SaveChangesAsync(cancellationToken);

            var response = new CategoryResponse
            {
                Id = category.Id,
                Name = category.Name,
                Type = category.Type,
                CreatedAt = category.CreatedAt
            };

            return Results.Created($"/api/categories/{category.Id}", response);
        }
    }
}
