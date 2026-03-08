using CostAccounting.API.Features.Reports.Common;

namespace CostAccounting.API.Features.Reports.Summary
{
    public static class GetSummaryReportEndpoint
    {
        public static RouteGroupBuilder MapGetSummaryReport(this RouteGroupBuilder group)
        {
            group.MapGet("/summary", async (
                    [AsParameters] GetSummaryReportRequest request,
                    GetSummaryReportHandler handler,
                    CancellationToken cancellationToken) =>
            {
                return await handler.HandleAsync(request, cancellationToken);
            })
                .WithName("GetSummaryReport")
                .WithSummary("Получить сводный отчёт")
                .WithDescription("Возвращает доходы, расходы и баланс за выбранный период.")
                .Produces<SummaryReportResponse>(StatusCodes.Status200OK)
                .Produces(StatusCodes.Status400BadRequest);

            return group;
        }
    }
}
