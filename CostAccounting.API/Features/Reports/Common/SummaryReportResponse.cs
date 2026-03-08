namespace CostAccounting.API.Features.Reports.Common
{
    public class SummaryReportResponse
    {
        public DateTime? From { get; set; }
        public DateTime? To { get; set; }
        public decimal TotalIncome { get; set; }
        public decimal TotalExpense { get; set; }
        public decimal Balance { get; set; }
    }
}
