package com.ecommerce.sb_ecom.repositories;

import com.ecommerce.sb_ecom.model.OrderItem;
import com.ecommerce.sb_ecom.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {
}
