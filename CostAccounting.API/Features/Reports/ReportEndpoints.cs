using CostAccounting.API.Features.Reports.ByCategory;
using CostAccounting.API.Features.Reports.Summary;

namespace CostAccounting.API.Features.Reports
{
    public static class ReportEndpoints
    {
        public static IEndpointRouteBuilder MapReportEndpoints(this IEndpointRouteBuilder app)
        {
            var group = app.MapGroup("/api/reports")
                .WithTags("Reports");

            group.MapGetSummaryReport();
            group.MapGetReportByCategory();

            return app;
        }
    }

}
