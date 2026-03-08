using CostAccounting.API.Features.Categories.Common;

namespace CostAccounting.API.Features.Categories.Update
{
    public static class UpdateCategoryEndpoint
    {
        public static RouteGroupBuilder MapUpdateCategory(this RouteGroupBuilder group)
        {
            group.MapPut("/{id:guid}", async (
                    Guid id,
                    UpdateCategoryRequest request,
                    UpdateCategoryHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(id, request, cancellationToken);
            })
                .WithName("UpdateCategory")
                .WithSummary("Обновить категорию")
                .WithDescription("Обновляет название и тип категории.")
                .Produces<CategoryResponse>(StatusCodes.Status200OK)
                .Produces(StatusCodes.Status400BadRequest)
                .Produces(StatusCodes.Status404NotFound)
                .Produces(StatusCodes.Status409Conflict);

            return group;
        }
    }
}
