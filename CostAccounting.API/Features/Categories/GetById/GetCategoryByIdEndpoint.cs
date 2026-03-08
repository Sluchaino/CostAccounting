using CostAccounting.API.Features.Categories.Common;

namespace CostAccounting.API.Features.Categories.GetById
{
    public static class GetCategoryByIdEndpoint
    {
        public static RouteGroupBuilder MapGetCategoryById(this RouteGroupBuilder group)
        {
            group.MapGet("/{id:guid}", async (
                    Guid id,
                    GetCategoryByIdHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(id, cancellationToken);
            })
                .WithName("GetCategoryById")
                .WithSummary("Получить категорию по Id")
                .WithDescription("Возвращает одну категорию по её идентификатору.")
                .Produces<CategoryResponse>(StatusCodes.Status200OK)
                .Produces(StatusCodes.Status404NotFound);

            return group;
        }
    }
}
