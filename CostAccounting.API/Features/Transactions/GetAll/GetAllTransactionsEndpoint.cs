using CostAccounting.API.Features.Transactions.Common;

namespace CostAccounting.API.Features.Transactions.GetAll
{
    public static class GetAllTransactionsEndpoint
    {
        public static RouteGroupBuilder MapGetAllTransactions(this RouteGroupBuilder group)
        {
            group.MapGet("/", async (
                    [AsParameters] GetAllTransactionsRequest request,
                    GetAllTransactionsHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(request, cancellationToken);
            })
                .WithName("GetAllTransactions")
                .WithSummary("Получить список транзакций")
                .WithDescription("Возвращает список транзакций с фильтрацией по периоду, категории и типу.")
                .Produces<List<TransactionResponse>>(StatusCodes.Status200OK);

            return group;
        }
    }
}
