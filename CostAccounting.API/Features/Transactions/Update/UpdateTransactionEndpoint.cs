using CostAccounting.API.Features.Transactions.Common;

namespace CostAccounting.API.Features.Transactions.Update
{
    public static class UpdateTransactionEndpoint
    {
        public static RouteGroupBuilder MapUpdateTransaction(this RouteGroupBuilder group)
        {
            group.MapPut("/{id:guid}", async (
                    Guid id,
                    UpdateTransactionRequest request,
                    UpdateTransactionHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(id, request, cancellationToken);
            })
                .WithName("UpdateTransaction")
                .WithSummary("Обновить транзакцию")
                .WithDescription("Обновляет дату, сумму, категорию и комментарий транзакции.")
                .Produces<TransactionResponse>(StatusCodes.Status200OK)
                .Produces(StatusCodes.Status400BadRequest)
                .Produces(StatusCodes.Status404NotFound);

            return group;
        }
    }
}
