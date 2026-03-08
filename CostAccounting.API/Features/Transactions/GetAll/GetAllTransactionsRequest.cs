using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Transactions.GetAll
{
    public class GetAllTransactionsRequest
    {
        public DateTime? From { get; set; }
        public DateTime? To { get; set; }
        public Guid? CategoryId { get; set; }
        public CategoryType? Type { get; set; }
    }
}
