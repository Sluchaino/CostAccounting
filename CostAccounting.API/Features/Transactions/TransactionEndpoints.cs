using CostAccounting.API.Features.Transactions.Create;
using CostAccounting.API.Features.Transactions.Delete;
using CostAccounting.API.Features.Transactions.GetAll;
using CostAccounting.API.Features.Transactions.GetById;
using CostAccounting.API.Features.Transactions.Update;

namespace CostAccounting.API.Features.Transactions
{
    public static class TransactionEndpoints
    {
        public static IEndpointRouteBuilder MapTransactionEndpoints(this IEndpointRouteBuilder app)
        {
            var group = app.MapGroup("/api/transactions")
                .WithTags("Transactions");

            group.MapCreateTransaction();
            group.MapGetAllTransactions();
            group.MapGetTransactionById();
            group.MapUpdateTransaction();
            group.MapDeleteTransaction();

            return app;
        }
    }
}
