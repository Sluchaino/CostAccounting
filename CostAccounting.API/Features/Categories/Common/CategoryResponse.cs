using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Categories.Common
{
    public class CategoryResponse
    {
        public Guid Id { get; set; }
        public string Name { get; set; } = default!;
        public CategoryType Type { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
