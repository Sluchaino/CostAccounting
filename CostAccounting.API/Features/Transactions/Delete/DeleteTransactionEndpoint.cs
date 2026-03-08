namespace CostAccounting.API.Features.Transactions.Delete
{
    public static class DeleteTransactionEndpoint
    {
        public static RouteGroupBuilder MapDeleteTransaction(this RouteGroupBuilder group)
        {
            group.MapDelete("/{id:guid}", async (
                    Guid id,
                    DeleteTransactionHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(id, cancellationToken);
            })
                .WithName("DeleteTransaction")
                .WithSummary("Удалить транзакцию")
                .WithDescription("Удаляет транзакцию по идентификатору.")
                .Produces(StatusCodes.Status204NoContent)
                .Produces(StatusCodes.Status404NotFound);

            return group;
        }
    }
}
