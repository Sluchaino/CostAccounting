using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Reports.ByCategory
{
    public class GetReportByCategoryRequest
    {
        public DateTime? From { get; set; }
        public DateTime? To { get; set; }
        public CategoryType? Type { get; set; }
    }
}
