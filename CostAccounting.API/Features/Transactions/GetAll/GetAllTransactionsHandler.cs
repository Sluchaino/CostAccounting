using CostAccounting.API.Data;
using CostAccounting.API.Features.Transactions.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Transactions.GetAll
{
    public class GetAllTransactionsHandler
    {
        private readonly AppDbContext _dbContext;

        public GetAllTransactionsHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(
            GetAllTransactionsRequest request,
            CancellationToken cancellationToken = default)
        {
            var query = _dbContext.Transactions
                .AsNoTracking()
                .Include(x => x.Category)
                .AsQueryable();

            if (request.From.HasValue)
            {
                query = query.Where(x => x.Date >= request.From.Value);
            }

            if (request.To.HasValue)
            {
                query = query.Where(x => x.Date <= request.To.Value);
            }

            if (request.CategoryId.HasValue)
            {
                query = query.Where(x => x.CategoryId == request.CategoryId.Value);
            }

            if (request.Type.HasValue)
            {
                query = query.Where(x => x.Category.Type == request.Type.Value);
            }

            var transactions = await query
                .OrderByDescending(x => x.Date)
                .ThenByDescending(x => x.CreatedAt)
                .Select(x => new TransactionResponse
                {
                    Id = x.Id,
                    Date = x.Date,
                    Amount = x.Amount,
                    CategoryId = x.CategoryId,
                    CategoryName = x.Category.Name,
                    CategoryType = x.Category.Type,
                    Comment = x.Comment,
                    CreatedAt = x.CreatedAt
                })
                .ToListAsync(cancellationToken);

            return Results.Ok(transactions);
        }
    }
}
