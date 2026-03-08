using CostAccounting.API.Data;
using CostAccounting.API.Domain;
using CostAccounting.API.Features.Reports.Common;
using Microsoft.EntityFrameworkCore;

namespace CostAccounting.API.Features.Reports.Summary
{
    public class GetSummaryReportHandler
    {
        private readonly AppDbContext _dbContext;

        public GetSummaryReportHandler(AppDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<IResult> HandleAsync(
            GetSummaryReportRequest request,
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

            var totalIncome = await query
                .Where(x => x.Category.Type == CategoryType.Income)
                .SumAsync(x => (decimal?)x.Amount, cancellationToken) ?? 0m;

            var totalExpense = await query
                .Where(x => x.Category.Type == CategoryType.Expense)
                .SumAsync(x => (decimal?)x.Amount, cancellationToken) ?? 0m;

            var response = new SummaryReportResponse
            {
                From = request.From,
                To = request.To,
                TotalIncome = totalIncome,
                TotalExpense = totalExpense,
                Balance = totalIncome - totalExpense
            };

            return Results.Ok(response);
        }
    }
}
