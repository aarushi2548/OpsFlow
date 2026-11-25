
package opsflow.repository;

import opsflow.entity.Inventory;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Find inventory for a particular warehouse and product
    Optional<Inventory> findByWarehouseIdAndProductId(
        Long warehouseId,
        Long productId
    );

    // Check whether inventory already exists
    boolean existsByWarehouseIdAndProductId(
        Long warehouseId,
        Long productId
    );

    // Get all inventory records in a warehouse
    List<Inventory> findByWarehouseId(
        Long warehouseId,
        Sort sort
    );

    // Get all warehouses containing a particular product
    List<Inventory> findByProductId(
        Long productId,
        Sort sort
    );

    // Find products that have reached or fallen below reorder level
    @Query("SELECT i FROM Inventory i WHERE i.quantityOnHand <= i.reorderLevel")
    List<Inventory> findLowStockItems();

    // Lock inventory record while updating stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(
        @Param("id") Long id
    );

    // Fetch warehouse and product details along with inventory
    @EntityGraph(attributePaths = {"warehouse", "product"})
    Optional<Inventory> findWithDetailsByWarehouseIdAndProductId(
        Long warehouseId,
        Long productId
    );
}
