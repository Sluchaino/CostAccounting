using CostAccounting.API.Features.Transactions.Common;

namespace CostAccounting.API.Features.Transactions.Create
{
    public static class CreateTransactionEndpoint
    {
        public static RouteGroupBuilder MapCreateTransaction(this RouteGroupBuilder group)
        {
            group.MapPost("/", async (
                    CreateTransactionRequest request,
                    CreateTransactionHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(request, cancellationToken);
            })
                .WithName("CreateTransaction")
                .WithSummary("Создать транзакцию")
                .WithDescription("Создаёт новую транзакцию дохода или расхода.")
                .Produces<TransactionResponse>(StatusCodes.Status201Created)
                .Produces(StatusCodes.Status400BadRequest);

            return group;
        }
    }
}
