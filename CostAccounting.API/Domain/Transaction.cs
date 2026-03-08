namespace CostAccounting.API.Domain
{
    public class Transaction
    {
        public Guid Id { get; set; }
        public DateTime Date { get; set; }
        public decimal Amount { get; set; }
        public Guid CategoryId { get; set; }
        public string? Comment { get; set; }
        public DateTime CreatedAt { get; set; }

        public Category Category { get; set; } = default!;
    }
}
