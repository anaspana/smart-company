package com.smartcompany.repository.order;

import com.smartcompany.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}