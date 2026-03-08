using CostAccounting.API.Features.Reports.Common;

namespace CostAccounting.API.Features.Reports.ByCategory
{
    public static class GetReportByCategoryEndpoint
    {
        public static RouteGroupBuilder MapGetReportByCategory(this RouteGroupBuilder group)
        {
            group.MapGet("/by-category", async (
                    [AsParameters] GetReportByCategoryRequest request,
                    GetReportByCategoryHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(request, cancellationToken);
            })
                .WithName("GetReportByCategory")
                .WithSummary("Получить отчёт по категориям")
                .WithDescription("Возвращает суммы транзакций, сгруппированные по категориям.")
                .Produces<List<CategoryReportItemResponse>>(StatusCodes.Status200OK)
                .Produces(StatusCodes.Status400BadRequest);

            return group;
        }
    }
}
