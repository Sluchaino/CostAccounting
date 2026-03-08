using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Categories.Create
{
    public class CreateCategoryRequest
    {
        public string Name { get; set; } = default!;
        public CategoryType Type { get; set; }
    }
}
