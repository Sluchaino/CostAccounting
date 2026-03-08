namespace CostAccounting.API.Features.Transactions.Update
{
    public class UpdateTransactionRequest
    {
        public DateTime Date { get; set; }
        public decimal Amount { get; set; }
        public Guid CategoryId { get; set; }
        public string? Comment { get; set; }
    }
}
