using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Categories.Update
{
    public class UpdateCategoryRequest
    {
        public string Name { get; set; } = default!;
        public CategoryType Type { get; set; }
    }
}
