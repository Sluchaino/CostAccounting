using CostAccounting.API.Domain;

namespace CostAccounting.API.Features.Transactions.Common
{
    public class TransactionResponse
    {
        public Guid Id { get; set; }
        public DateTime Date { get; set; }
        public decimal Amount { get; set; }
        public Guid CategoryId { get; set; }
        public string CategoryName { get; set; } = default!;
        public CategoryType CategoryType { get; set; }
        public string? Comment { get; set; }
        public DateTime CreatedAt { get; set; }
    }

}
