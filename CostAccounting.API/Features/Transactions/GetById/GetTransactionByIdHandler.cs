using CostAccounting.API.Data;
using CostAccounting.API.Features.Transactions.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Transactions.GetById
{
    public class GetTransactionByIdHandler
    {
        private readonly AppDbContext _dbContext;

        public GetTransactionByIdHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(Guid id, CancellationToken cancellationToken = default)
        {
            var transaction = await _dbContext.Transactions
                .AsNoTracking()
                .Include(x => x.Category)
                .Where(x => x.Id == id)
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
                .FirstOrDefaultAsync(cancellationToken);

            if (transaction is null)
            {
                return Results.NotFound(new { message = "Транзакция не найдена." });
            }

            return Results.Ok(transaction);
        }
    }
}
