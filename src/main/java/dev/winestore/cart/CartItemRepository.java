package dev.winestore.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = "wine")
    List<CartItem> findByUserId(long userId);

    @Query("select c from CartItem c where c.userId = :userId and c.wine.id = :wineId")
    Optional<CartItem> findOne(@Param("userId") long userId, @Param("wineId") long wineId);

    @Modifying
    @Query("delete from CartItem c where c.userId = :userId and c.wine.id = :wineId")
    void deleteOne(@Param("userId") long userId, @Param("wineId") long wineId);

    @Modifying
    @Query("delete from CartItem c where c.userId = :userId")
    void deleteAllByUser(@Param("userId") long userId);
}
