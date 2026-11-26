package dev.winestore.cart;

import dev.winestore.wine.Wine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wine_id")
    private Wine wine;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {
    }

    public CartItem(long userId, Wine wine, int quantity) {
        this.userId = userId;
        this.wine = wine;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public Wine getWine() {
        return wine;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
