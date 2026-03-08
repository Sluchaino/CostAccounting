using CostAccounting.API.Features.Categories.Common;

namespace CostAccounting.API.Features.Categories.Create
{
    public static class CreateCategoryEndpoint
    {
        public static RouteGroupBuilder MapCreateCategory(this RouteGroupBuilder group)
        {
            group.MapPost("/", async (
                    CreateCategoryRequest request,
                    CreateCategoryHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(request, cancellationToken);
            })
                .WithName("CreateCategory")
                .WithSummary("Создать категорию")
                .WithDescription("Создаёт новую категорию доходов или расходов.")
                .Produces<CategoryResponse>(StatusCodes.Status201Created)
                .Produces(StatusCodes.Status400BadRequest)
                .Produces(StatusCodes.Status409Conflict);

            return group;
        }
    }
}
