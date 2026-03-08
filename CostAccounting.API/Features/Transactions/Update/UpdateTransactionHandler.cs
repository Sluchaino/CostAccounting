using CostAccounting.API.Data;
using CostAccounting.API.Features.Transactions.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Transactions.Update
{
    public class UpdateTransactionHandler
    {
        private readonly AppDbContext _dbContext;

        public UpdateTransactionHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(
            Guid id,
            UpdateTransactionRequest request,
            CancellationToken cancellationToken = default)
        {
            if (request.Amount <= 0)
            {
                return Results.BadRequest(new { message = "Сумма должна быть больше 0." });
            }

            var transaction = await _dbContext.Transactions
                .FirstOrDefaultAsync(x => x.Id == id, cancellationToken);

            if (transaction is null)
            {
                return Results.NotFound(new { message = "Транзакция не найдена." });
            }

            var category = await _dbContext.Categories
                .AsNoTracking()
                .FirstOrDefaultAsync(x => x.Id == request.CategoryId, cancellationToken);

            if (category is null)
            {
                return Results.BadRequest(new { message = "Указанная категория не существует." });
            }

            transaction.Date = request.Date;
            transaction.Amount = request.Amount;
            transaction.CategoryId = request.CategoryId;
            transaction.Comment = string.IsNullOrWhiteSpace(request.Comment)
                ? null
                : request.Comment.Trim();

            await _dbContext.SaveChangesAsync(cancellationToken);

            var response = new TransactionResponse
            {
                Id = transaction.Id,
                Date = transaction.Date,
                Amount = transaction.Amount,
                CategoryId = category.Id,
                CategoryName = category.Name,
                CategoryType = category.Type,
                Comment = transaction.Comment,
                CreatedAt = transaction.CreatedAt
            };

            return Results.Ok(response);
        }
    }
}
