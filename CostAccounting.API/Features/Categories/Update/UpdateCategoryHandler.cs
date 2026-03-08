using CostAccounting.API.Data;
using CostAccounting.API.Features.Categories.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Categories.Update
{
    public class UpdateCategoryHandler
    {
        private readonly AppDbContext _dbContext;

        public UpdateCategoryHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(
            Guid id,
            UpdateCategoryRequest request,
            CancellationToken cancellationToken = default)
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

            var category = await _dbContext.Categories
                .FirstOrDefaultAsync(x => x.Id == id, cancellationToken);

            if (category is null)
            {
                return Results.NotFound(new { message = "Категория не найдена." });
            }

            var duplicateExists = await _dbContext.Categories
                .AnyAsync(x => x.Id != id && x.Name.ToLower() == normalizedName.ToLower(), cancellationToken);

            if (duplicateExists)
            {
                return Results.Conflict(new { message = "Категория с таким названием уже существует." });
            }

            category.Name = normalizedName;
            category.Type = request.Type;

            await _dbContext.SaveChangesAsync(cancellationToken);

            var response = new CategoryResponse
            {
                Id = category.Id,
                Name = category.Name,
                Type = category.Type,
                CreatedAt = category.CreatedAt
            };

            return Results.Ok(response);
        }
    }
}
