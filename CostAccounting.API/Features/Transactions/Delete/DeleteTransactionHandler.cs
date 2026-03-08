using CostAccounting.API.Data;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Transactions.Delete
{
    public class DeleteTransactionHandler
    {
        private readonly AppDbContext _dbContext;

        public DeleteTransactionHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(Guid id, CancellationToken cancellationToken = default)
        {
            var transaction = await _dbContext.Transactions
                .FirstOrDefaultAsync(x => x.Id == id, cancellationToken);

            if (transaction is null)
            {
                return Results.NotFound(new { message = "Транзакция не найдена." });
            }

            _dbContext.Transactions.Remove(transaction);
            await _dbContext.SaveChangesAsync(cancellationToken);

            return Results.NoContent();
        }
    }
}
