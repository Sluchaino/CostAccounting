using CostAccounting.API.Data;
using CostAccounting.API.Features.Reports.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Reports.ByCategory
{
    public class GetReportByCategoryHandler
    {
        private readonly AppDbContext _dbContext;

        public GetReportByCategoryHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(
            GetReportByCategoryRequest request,
            CancellationToken cancellationToken = default)
        {
            if (request.From.HasValue && request.To.HasValue && request.From > request.To)
            {
                return Results.BadRequest(new { message = "Дата 'from' не может быть больше даты 'to'." });
            }

            var query = _dbContext.Transactions
                .AsNoTracking()
                .AsQueryable();

            if (request.From.HasValue)
            {
                query = query.Where(x => x.Date >= request.From.Value);
            }

            if (request.To.HasValue)
            {
                query = query.Where(x => x.Date <= request.To.Value);
            }

            if (request.Type.HasValue)
            {
                query = query.Where(x => x.Category.Type == request.Type.Value);
            }

            var report = await query
                .GroupBy(x => new
                {
                    x.CategoryId,
                    CategoryName = x.Category.Name,
                    CategoryType = x.Category.Type
                })
                .Select(g => new CategoryReportItemResponse
                {
                    CategoryId = g.Key.CategoryId,
                    CategoryName = g.Key.CategoryName,
                    CategoryType = g.Key.CategoryType,
                    Total = g.Sum(x => x.Amount)
                })
                .OrderByDescending(x => x.Total)
                .ThenBy(x => x.CategoryName)
                .ToListAsync(cancellationToken);

            return Results.Ok(report);
        }
    }

}
