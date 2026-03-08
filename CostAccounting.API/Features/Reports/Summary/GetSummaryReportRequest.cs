namespace CostAccounting.API.Features.Reports.Summary
{
    public class GetSummaryReportRequest
    {
        public DateTime? From { get; set; }
        public DateTime? To { get; set; }
    }
}
