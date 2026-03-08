using CostAccounting.API.Domain;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace CostAccounting.API.Data.Configurations
{
    public class TransactionConfiguration : IEntityTypeConfiguration<Transaction>
    {
        public void Configure(EntityTypeBuilder<Transaction> builder)
        {
            builder.ToTable("transactions");

            builder.HasKey(x => x.Id);

            builder.Property(x => x.Id)
                .HasColumnName("id");

            builder.Property(x => x.Date)
                .HasColumnName("date")
                .IsRequired();

            builder.Property(x => x.Amount)
                .HasColumnName("amount")
                .HasPrecision(12, 2)
                .IsRequired();

            builder.Property(x => x.CategoryId)
                .HasColumnName("category_id")
                .IsRequired();

            builder.Property(x => x.Comment)
                .HasColumnName("comment")
                .HasMaxLength(255);

            builder.Property(x => x.CreatedAt)
                .HasColumnName("created_at")
                .IsRequired();

            builder.HasIndex(x => x.Date);
            builder.HasIndex(x => x.CategoryId);
        }
    }
}
