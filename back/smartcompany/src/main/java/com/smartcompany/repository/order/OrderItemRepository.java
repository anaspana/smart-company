package com.smartcompany.repository.order;

import com.smartcompany.model.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);
}