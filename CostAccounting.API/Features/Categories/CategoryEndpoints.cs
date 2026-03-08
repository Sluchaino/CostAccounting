using CostAccounting.API.Features.Categories.Create;
using CostAccounting.API.Features.Categories.Delete;
using CostAccounting.API.Features.Categories.GetAll;
using CostAccounting.API.Features.Categories.GetById;
using CostAccounting.API.Features.Categories.Update;

namespace CostAccounting.API.Features.Categories
{
    public static class CategoryEndpoints
    {
        public static IEndpointRouteBuilder MapCategoryEndpoints(this IEndpointRouteBuilder app)
        {
            var group = app.MapGroup("/api/categories")
                .WithTags("Categories");

            group.MapCreateCategory();
            group.MapGetAllCategories();
            group.MapGetCategoryById();
            group.MapUpdateCategory();
            group.MapDeleteCategory();

            return app;
        }
    }
}
