using CostAccounting.API.Features.Categories.Common;

namespace CostAccounting.API.Features.Categories.GetAll
{
    public static class GetAllCategoriesEndpoint
    {
        public static RouteGroupBuilder MapGetAllCategories(this RouteGroupBuilder group)
        {
            group.MapGet("/", async (
                    GetAllCategoriesHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(cancellationToken);
            })
                .WithName("GetAllCategories")
                .WithSummary("Получить список категорий")
                .WithDescription("Возвращает все категории доходов и расходов.")
                .Produces<List<CategoryResponse>>(StatusCodes.Status200OK);

            return group;
        }
    }
}
