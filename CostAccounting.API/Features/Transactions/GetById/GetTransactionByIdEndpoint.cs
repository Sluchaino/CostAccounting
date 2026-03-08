using CostAccounting.API.Features.Transactions.Common;

namespace CostAccounting.API.Features.Transactions.GetById
{
    public static class GetTransactionByIdEndpoint
    {
        public static RouteGroupBuilder MapGetTransactionById(this RouteGroupBuilder group)
        {
            group.MapGet("/{id:guid}", async (
                    Guid id,
                    GetTransactionByIdHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(id, cancellationToken);
            })
                .WithName("GetTransactionById")
                .WithSummary("Получить транзакцию по Id")
                .WithDescription("Возвращает одну транзакцию по идентификатору.")
                .Produces<TransactionResponse>(StatusCodes.Status200OK)
                .Produces(StatusCodes.Status404NotFound);

            return group;
        }
    }
}
