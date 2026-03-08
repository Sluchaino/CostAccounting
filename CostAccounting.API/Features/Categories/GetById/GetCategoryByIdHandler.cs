using CostAccounting.API.Data;
using CostAccounting.API.Features.Categories.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Categories.GetById
{
    public class GetCategoryByIdHandler
    {
        private readonly AppDbContext _dbContext;

        public GetCategoryByIdHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(Guid id, CancellationToken cancellationToken = default)
        {
            var category = await _dbContext.Categories
                .AsNoTracking()
                .Where(x => x.Id == id)
                .Select(x => new CategoryResponse
                {
                    Id = x.Id,
                    Name = x.Name,
                    Type = x.Type,
                    CreatedAt = x.CreatedAt
                })
                .FirstOrDefaultAsync(cancellationToken);

            if (category is null)
            {
                return Results.NotFound(new { message = "Категория не найдена." });
            }

            return Results.Ok(category);
        }
    }
}
