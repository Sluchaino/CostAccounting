using CostAccounting.API.Data;
using CostAccounting.API.Features.Categories.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Categories.GetAll
{
    public class GetAllCategoriesHandler
    {
        private readonly AppDbContext _dbContext;

        public GetAllCategoriesHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(CancellationToken cancellationToken = default)
        {
            var categories = await _dbContext.Categories
                .AsNoTracking()
                .OrderBy(x => x.Type)
                .ThenBy(x => x.Name)
                .Select(x => new CategoryResponse
                {
                    Id = x.Id,
                    Name = x.Name,
                    Type = x.Type,
                    CreatedAt = x.CreatedAt
                })
                .ToListAsync(cancellationToken);

            return Results.Ok(categories);
        }
    }
}
