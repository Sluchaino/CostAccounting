namespace CostAccounting.API.Features.Categories.Delete
{
    public static class DeleteCategoryEndpoint
    {
        public static RouteGroupBuilder MapDeleteCategory(this RouteGroupBuilder group)
        {
            group.MapDelete("/{id:guid}", async (
                    Guid id,
                    DeleteCategoryHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(id, cancellationToken);
            })
                .WithName("DeleteCategory")
                .WithSummary("Удалить категорию")
                .WithDescription("Удаляет категорию, если она не используется в транзакциях.")
                .Produces(StatusCodes.Status204NoContent)
                .Produces(StatusCodes.Status404NotFound)
                .Produces(StatusCodes.Status409Conflict);

            return group;
        }
    }
}
