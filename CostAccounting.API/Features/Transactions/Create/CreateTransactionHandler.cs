using CostAccounting.API.Data;
using CostAccounting.API.Features.Transactions.Common;
using Microsoft.EntityFrameworkCore;
using CostAccounting.API.Domain;
namespace CostAccounting.API.Features.Transactions.Create
{
    public class CreateTransactionHandler
    {
        private readonly AppDbContext _dbContext;

        public CreateTransactionHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(
            CreateTransactionRequest request,
            CancellationToken cancellationToken = default)
        {
            if (request.Amount <= 0)
            {
                return Results.BadRequest(new { message = "Сумма должна быть больше 0." });
            }

            var category = await _dbContext.Categories
                .AsNoTracking()
                .FirstOrDefaultAsync(x => x.Id == request.CategoryId, cancellationToken);

            if (category is null)
            {
                return Results.BadRequest(new { message = "Указанная категория не существует." });
            }

            var transaction = new Transaction
            {
                Id = Guid.NewGuid(),
                Date = request.Date,
                Amount = request.Amount,
                CategoryId = request.CategoryId,
                Comment = string.IsNullOrWhiteSpace(request.Comment)
                    ? null
                    : request.Comment.Trim(),
                CreatedAt = DateTime.UtcNow
            };

            _dbContext.Transactions.Add(transaction);
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

            return Results.Created($"/api/transactions/{transaction.Id}", response);
        }
    }
}
