namespace CostAccounting.API.Domain
{
    public class Category
    {
        public Guid Id { get; set; }
        public string Name { get; set; } = default!;
        public CategoryType Type { get; set; }
        public DateTime CreatedAt { get; set; }

        public ICollection<Transaction> Transactions { get; set; } = new List<Transaction>();
    }
}
