using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Reports.Common
{
    public class CategoryReportItemResponse
    {
        public Guid CategoryId { get; set; }
        public string CategoryName { get; set; } = default!;
        public CategoryType CategoryType { get; set; }
        public decimal Total { get; set; }
    }
}
