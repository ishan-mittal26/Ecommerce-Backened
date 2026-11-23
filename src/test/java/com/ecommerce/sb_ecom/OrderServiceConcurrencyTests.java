package com.ecommerce.sb_ecom;


import com.ecommerce.sb_ecom.service.OrderServiceImpl;
//import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(OrderServiceImpl.class)
public class OrderServiceConcurrencyTests {

    @Test
    void contextLoads() {
    }
}
