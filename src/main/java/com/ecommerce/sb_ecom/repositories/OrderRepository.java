package com.ecommerce.sb_ecom.repositories;

import com.ecommerce.sb_ecom.model.Order;
import com.ecommerce.sb_ecom.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order,Long>
{
    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o")
    Double getTotalRevenue();
}
