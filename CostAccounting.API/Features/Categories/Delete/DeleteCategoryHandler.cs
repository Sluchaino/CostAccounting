using CostAccounting.API.Data;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Categories.Delete
{
    public class DeleteCategoryHandler
    {
        private readonly AppDbContext _dbContext;

        public DeleteCategoryHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(Guid id, CancellationToken cancellationToken = default)
        {
            var category = await _dbContext.Categories
                .FirstOrDefaultAsync(x => x.Id == id, cancellationToken);

            if (category is null)
            {
                return Results.NotFound(new { message = "Категория не найдена." });
            }

            var hasTransactions = await _dbContext.Transactions
                .AnyAsync(x => x.CategoryId == id, cancellationToken);

            if (hasTransactions)
            {
                return Results.Conflict(new
                {
                    message = "Нельзя удалить категорию, так как она используется в транзакциях."
                });
            }

            _dbContext.Categories.Remove(category);
            await _dbContext.SaveChangesAsync(cancellationToken);

            return Results.NoContent();
        }
    }
}
