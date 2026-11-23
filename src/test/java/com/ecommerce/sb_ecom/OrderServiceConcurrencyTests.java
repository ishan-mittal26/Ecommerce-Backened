
package com.ecommerce.sb_ecom;

import com.ecommerce.sb_ecom.config.AppConfig;
import com.ecommerce.sb_ecom.model.*;
import com.ecommerce.sb_ecom.service.CartService;
import com.ecommerce.sb_ecom.service.OrderServiceImpl;
import com.ecommerce.sb_ecom.util.AuthUtil;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ecommerce.sb_ecom.repositories.ProductRepository;

import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.mockito.invocation.InvocationOnMock;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({OrderServiceImpl.class, AppConfig.class})
class OrderServiceConcurrencyTests {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderServiceImpl orderService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private AuthUtil authUtil;

    @Test
    void placeOrder_shouldDecreaseProductStock() {

        User buyer = new User(
                "buyer@example.com",
                "buyer",
                "password"
        );
        entityManager.persist(buyer);

        Category category = new Category();
        category.setCategoryName("Electronics");
        entityManager.persist(category);

        Product product = new Product();
        product.setProductName("Test Laptop");
        product.setQuantity(10);
        product.setPrice(1000.0);
        product.setDiscount(0.0);
        product.setSpecialPrice(1000.0);
        product.setCategory(category);
        entityManager.persist(product);

        Address address = new Address(
                "Test Street",
                "Test Building",
                "Noida",
                "Uttar Pradesh",
                "India",
                "201301"
        );
        address.setUser(buyer);
        entityManager.persist(address);

        Cart cart = new Cart();
        cart.setUser(buyer);
        cart.setTotalPrice(2000.0);
        entityManager.persist(cart);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setDiscount(0.0);
        cartItem.setProductPrice(1000.0);
        entityManager.persist(cartItem);

        Long productId = product.getProductId();
        Long addressId = address.getAddressId();

        entityManager.flush();
        entityManager.clear();

        orderService.placeOrder(
                "buyer@example.com",
                addressId,
                "CARD",
                "TEST",
                "payment-123",
                "SUCCESS",
                "Payment successful"
        );

        entityManager.flush();
        entityManager.clear();

        Product updatedProduct =
                entityManager.find(Product.class, productId);
        System.out.println("Updated product quantity ishan: " + updatedProduct.getQuantity());

        assertEquals(8, updatedProduct.getQuantity());
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentOrders_shouldExposeLostStockUpdate() throws Exception {

        CyclicBarrier barrier = new CyclicBarrier(2);

        // Both orders must reach save() before either can continue.
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);

            // Wait until both orders have calculated their updated stock.
            barrier.await(10, TimeUnit.SECONDS);

            // Perform the actual JPA merge in the current transaction.
            return entityManager.merge(product);

        }).when(productRepository).save(any(Product.class));

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        // Create test data in a committed transaction.
        TestData data = transactionTemplate.execute(status -> {

            User buyerA = new User(
                    "buyerA@example.com", "buyerA", "password");
            entityManager.persist(buyerA);

            User buyerB = new User(
                    "buyerB@example.com", "buyerB", "password");
            entityManager.persist(buyerB);

            Category category = new Category();
            category.setCategoryName("Electronics");
            entityManager.persist(category);

            Product product = new Product();
            product.setProductName("Shared Laptop");
            product.setQuantity(10);
            product.setPrice(1000.0);
            product.setDiscount(0.0);
            product.setSpecialPrice(1000.0);
            product.setCategory(category);
            entityManager.persist(product);

            Address addressA = new Address(
                    "Street A", "Building A", "Noida",
                    "Uttar Pradesh", "India", "201301");
            addressA.setUser(buyerA);
            entityManager.persist(addressA);

            Address addressB = new Address(
                    "Street B", "Building B", "Noida",
                    "Uttar Pradesh", "India", "201301");
            addressB.setUser(buyerB);
            entityManager.persist(addressB);

            Cart cartA = new Cart();
            cartA.setUser(buyerA);
            cartA.setTotalPrice(2000.0);
            entityManager.persist(cartA);

            Cart cartB = new Cart();
            cartB.setUser(buyerB);
            cartB.setTotalPrice(2000.0);
            entityManager.persist(cartB);

            CartItem itemA = new CartItem();
            itemA.setCart(cartA);
            itemA.setProduct(product);
            itemA.setQuantity(2);
            itemA.setDiscount(0.0);
            itemA.setProductPrice(1000.0);
            entityManager.persist(itemA);

            CartItem itemB = new CartItem();
            itemB.setCart(cartB);
            itemB.setProduct(product);
            itemB.setQuantity(2);
            itemB.setDiscount(0.0);
            itemB.setProductPrice(1000.0);
            entityManager.persist(itemB);

            entityManager.flush();

            return new TestData(
                    product.getProductId(),
                    addressA.getAddressId(),
                    addressB.getAddressId()
            );
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> orderA = executor.submit(() ->
                    orderService.placeOrder(
                            "buyerA@example.com",
                            data.addressAId(),
                            "CARD", "TEST",
                            "payment-A", "SUCCESS", "Payment successful"
                    )
            );

            Future<?> orderB = executor.submit(() ->
                    orderService.placeOrder(
                            "buyerB@example.com",
                            data.addressBId(),
                            "CARD", "TEST",
                            "payment-B", "SUCCESS", "Payment successful"
                    )
            );

            orderA.get(20, TimeUnit.SECONDS);
            orderB.get(20, TimeUnit.SECONDS);

        } finally {
            executor.shutdownNow();
        }

        Integer finalQuantity = transactionTemplate.execute(status ->
                entityManager.find(Product.class, data.productId())
                        .getQuantity()
        );

        System.out.println("Final stock after concurrent orders: " + finalQuantity);

        assertEquals(8, finalQuantity);
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentOrders_withSufficientStock_shouldProcessAllOrders() throws Exception {

        int customerCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(customerCount);

        // Make all customers reach the stock update together.
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);

            barrier.await(10, TimeUnit.SECONDS);

            return entityManager.merge(product);

        }).when(productRepository).save(any(Product.class));

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        // Create product and 10 customers with separate carts.
        List<Long> addressIds = transactionTemplate.execute(status -> {

            List<Long> ids = new ArrayList<>();

            Category category = new Category();
            category.setCategoryName("Electronics");
            entityManager.persist(category);

            Product product = new Product();
            product.setProductName("Shared Laptop");
            product.setQuantity(100);
            product.setPrice(1000.0);
            product.setDiscount(0.0);
            product.setSpecialPrice(1000.0);
            product.setCategory(category);
            entityManager.persist(product);

            for (int i = 0; i < customerCount; i++) {

                User buyer = new User(
                        "buyer" + i + "@example.com",
                        "buyer" + i,
                        "password"
                );
                entityManager.persist(buyer);

                Address address = new Address(
                        "Street " + i,
                        "Building " + i,
                        "Noida",
                        "Uttar Pradesh",
                        "India",
                        "201301"
                );
                address.setUser(buyer);
                entityManager.persist(address);

                ids.add(address.getAddressId());

                Cart cart = new Cart();
                cart.setUser(buyer);
                cart.setTotalPrice(10000.0);
                entityManager.persist(cart);

                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(10);
                item.setDiscount(0.0);
                item.setProductPrice(1000.0);
                entityManager.persist(item);
            }

            entityManager.flush();

            return ids;
        });

        ExecutorService executor =
                Executors.newFixedThreadPool(customerCount);

        try {
            List<Future<?>> orders = new ArrayList<>();

            for (int i = 0; i < customerCount; i++) {

                int index = i;

                orders.add(executor.submit(() ->
                        orderService.placeOrder(
                                "buyer" + index + "@example.com",
                                addressIds.get(index),
                                "CARD",
                                "TEST",
                                "payment-" + index,
                                "SUCCESS",
                                "Payment successful"
                        )
                ));
            }

            // Every order must complete successfully.
            for (Future<?> order : orders) {
                order.get(30, TimeUnit.SECONDS);
            }

        } finally {
            executor.shutdownNow();
        }

        Integer finalQuantity = transactionTemplate.execute(status ->
                entityManager.createQuery(
                                "SELECT p.quantity FROM Product p WHERE p.productName = :name",
                                Integer.class
                        )
                        .setParameter("name", "Shared Laptop")
                        .getSingleResult()
        );

        System.out.println("Final stock after 10 concurrent orders: " + finalQuantity);

        assertEquals(0, finalQuantity);
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentOrders_withInsufficientStock_shouldRejectExcessOrders() throws Exception {

        int customerCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(customerCount);

        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);

            barrier.await(10, TimeUnit.SECONDS);

            return entityManager.merge(product);

        }).when(productRepository).save(any(Product.class));

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        List<Long> addressIds = transactionTemplate.execute(status -> {

            List<Long> ids = new ArrayList<>();

            Category category = new Category();
            category.setCategoryName("Electronics");
            entityManager.persist(category);

            Product product = new Product();
            product.setProductName("Limited Stock Laptop");
            product.setQuantity(50);
            product.setPrice(1000.0);
            product.setDiscount(0.0);
            product.setSpecialPrice(1000.0);
            product.setCategory(category);
            entityManager.persist(product);

            for (int i = 0; i < customerCount; i++) {

                User buyer = new User(
                        "limitedBuyer" + i + "@example.com",
                        "limitedBuyer" + i,
                        "password"
                );
                entityManager.persist(buyer);

                Address address = new Address(
                        "Street " + i,
                        "Building " + i,
                        "Noida",
                        "Uttar Pradesh",
                        "India",
                        "201301"
                );
                address.setUser(buyer);
                entityManager.persist(address);

                ids.add(address.getAddressId());

                Cart cart = new Cart();
                cart.setUser(buyer);
                cart.setTotalPrice(10000.0);
                entityManager.persist(cart);

                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(10);
                item.setDiscount(0.0);
                item.setProductPrice(1000.0);
                entityManager.persist(item);
            }

            entityManager.flush();

            return ids;
        });

        ExecutorService executor =
                Executors.newFixedThreadPool(customerCount);

        int successfulOrders = 0;
        int rejectedOrders = 0;

        try {
            List<Future<Boolean>> orders = new ArrayList<>();

            for (int i = 0; i < customerCount; i++) {

                int index = i;

                orders.add(executor.submit(() -> {
                    try {
                        orderService.placeOrder(
                                "limitedBuyer" + index + "@example.com",
                                addressIds.get(index),
                                "CARD",
                                "TEST",
                                "limited-payment-" + index,
                                "SUCCESS",
                                "Payment successful"
                        );

                        return true;

                    } catch (Exception exception) {
                        return false;
                    }
                }));
            }

            for (Future<Boolean> order : orders) {
                if (order.get(30, TimeUnit.SECONDS)) {
                    successfulOrders++;
                } else {
                    rejectedOrders++;
                }
            }

        } finally {
            executor.shutdownNow();
        }

        Integer finalQuantity = transactionTemplate.execute(status ->
                entityManager.createQuery(
                                "SELECT p.quantity FROM Product p WHERE p.productName = :name",
                                Integer.class
                        )
                        .setParameter("name", "Limited Stock Laptop")
                        .getSingleResult()
        );

        System.out.println("Successful orders: " + successfulOrders);
        System.out.println("Rejected orders: " + rejectedOrders);
        System.out.println("Final stock: " + finalQuantity);

        assertEquals(5, successfulOrders);
        assertEquals(5, rejectedOrders);
        assertEquals(0, finalQuantity);
        assertTrue(finalQuantity >= 0);
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentOrders_withSingleStock_shouldAllowOnlyOneOrder() throws Exception {

        int customerCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(customerCount);

        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);

            barrier.await(10, TimeUnit.SECONDS);

            return entityManager.merge(product);

        }).when(productRepository).save(any(Product.class));

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        List<Long> addressIds = transactionTemplate.execute(status -> {

            List<Long> ids = new ArrayList<>();

            Category category = new Category();
            category.setCategoryName("Electronics");
            entityManager.persist(category);

            Product product = new Product();
            product.setProductName("Last Available Laptop");
            product.setQuantity(1);
            product.setPrice(1000.0);
            product.setDiscount(0.0);
            product.setSpecialPrice(1000.0);
            product.setCategory(category);
            entityManager.persist(product);

            for (int i = 0; i < customerCount; i++) {

                User buyer = new User(
                        "singleBuyer" + i + "@example.com",
                        "singleBuyer" + i,
                        "password"
                );
                entityManager.persist(buyer);

                Address address = new Address(
                        "Street " + i,
                        "Building " + i,
                        "Noida",
                        "Uttar Pradesh",
                        "India",
                        "201301"
                );
                address.setUser(buyer);
                entityManager.persist(address);

                ids.add(address.getAddressId());

                Cart cart = new Cart();
                cart.setUser(buyer);
                cart.setTotalPrice(1000.0);
                entityManager.persist(cart);

                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(1);
                item.setDiscount(0.0);
                item.setProductPrice(1000.0);
                entityManager.persist(item);
            }

            entityManager.flush();

            return ids;
        });

        ExecutorService executor =
                Executors.newFixedThreadPool(customerCount);

        int successfulOrders = 0;
        int rejectedOrders = 0;

        try {
            List<Future<Boolean>> orders = new ArrayList<>();

            for (int i = 0; i < customerCount; i++) {

                int index = i;

                orders.add(executor.submit(() -> {
                    try {
                        orderService.placeOrder(
                                "singleBuyer" + index + "@example.com",
                                addressIds.get(index),
                                "CARD",
                                "TEST",
                                "single-payment-" + index,
                                "SUCCESS",
                                "Payment successful"
                        );

                        return true;

                    } catch (Exception exception) {
                        return false;
                    }
                }));
            }

            for (Future<Boolean> order : orders) {
                if (order.get(30, TimeUnit.SECONDS)) {
                    successfulOrders++;
                } else {
                    rejectedOrders++;
                }
            }

        } finally {
            executor.shutdownNow();
        }

        Integer finalQuantity = transactionTemplate.execute(status ->
                entityManager.createQuery(
                                "SELECT p.quantity FROM Product p WHERE p.productName = :name",
                                Integer.class
                        )
                        .setParameter("name", "Last Available Laptop")
                        .getSingleResult()
        );

        System.out.println("Successful orders: " + successfulOrders);
        System.out.println("Rejected orders: " + rejectedOrders);
        System.out.println("Final stock: " + finalQuantity);

        assertEquals(1, successfulOrders);
        assertEquals(9, rejectedOrders);
        assertEquals(0, finalQuantity);
        assertTrue(finalQuantity >= 0);
    }


    @Test
    void placeOrder_withZeroStock_shouldRejectOrder() {

        User buyer = new User(
                "zeroBuyer@example.com",
                "zeroBuyer",
                "password"
        );
        entityManager.persist(buyer);

        Category category = new Category();
        category.setCategoryName("Electronics");
        entityManager.persist(category);

        Product product = new Product();
        product.setProductName("Out Of Stock Laptop");
        product.setQuantity(0);
        product.setPrice(1000.0);
        product.setDiscount(0.0);
        product.setSpecialPrice(1000.0);
        product.setCategory(category);
        entityManager.persist(product);

        Address address = new Address(
                "Test Street",
                "Test Building",
                "Noida",
                "Uttar Pradesh",
                "India",
                "201301"
        );
        address.setUser(buyer);
        entityManager.persist(address);

        Cart cart = new Cart();
        cart.setUser(buyer);
        cart.setTotalPrice(1000.0);
        entityManager.persist(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        item.setDiscount(0.0);
        item.setProductPrice(1000.0);
        entityManager.persist(item);

        entityManager.flush();
        entityManager.clear();

        assertThrows(RuntimeException.class, () ->
                orderService.placeOrder(
                        "zeroBuyer@example.com",
                        address.getAddressId(),
                        "CARD",
                        "TEST",
                        "zero-payment",
                        "SUCCESS",
                        "Payment successful"
                )
        );

        entityManager.clear();

        Product updatedProduct =
                entityManager.find(Product.class, product.getProductId());

        assertEquals(0, updatedProduct.getQuantity());
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void placeOrder_withInsufficientStock_shouldRollbackAllDatabaseChanges() {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        RollbackTestData data = transactionTemplate.execute(status -> {

            User buyer = new User(
                    "rollbackBuyer@example.com",
                    "rollbackBuyer",
                    "password"
            );
            entityManager.persist(buyer);

            Category category = new Category();
            category.setCategoryName("Electronics");
            entityManager.persist(category);

            Product product = new Product();
            product.setProductName("Rollback Laptop");
            product.setQuantity(1);
            product.setPrice(1000.0);
            product.setDiscount(0.0);
            product.setSpecialPrice(1000.0);
            product.setCategory(category);
            entityManager.persist(product);

            Address address = new Address(
                    "Test Street",
                    "Test Building",
                    "Noida",
                    "Uttar Pradesh",
                    "India",
                    "201301"
            );
            address.setUser(buyer);
            entityManager.persist(address);

            Cart cart = new Cart();
            cart.setUser(buyer);
            cart.setTotalPrice(2000.0);
            entityManager.persist(cart);

            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(2);
            item.setDiscount(0.0);
            item.setProductPrice(1000.0);
            entityManager.persist(item);

            entityManager.flush();

            return new RollbackTestData(
                    product.getProductId(),
                    address.getAddressId()
            );
        });

        // Capture existing record counts before placing the order.
        Long paymentsBefore = transactionTemplate.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(p) FROM Payment p", Long.class
                ).getSingleResult()
        );

        Long ordersBefore = transactionTemplate.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(o) FROM Order o", Long.class
                ).getSingleResult()
        );

        Long orderItemsBefore = transactionTemplate.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(i) FROM OrderItem i", Long.class
                ).getSingleResult()
        );

        // Ordering 2 units when only 1 is available must fail.
        assertThrows(RuntimeException.class, () ->
                orderService.placeOrder(
                        "rollbackBuyer@example.com",
                        data.addressId(),
                        "CARD",
                        "TEST",
                        "rollback-payment",
                        "SUCCESS",
                        "Payment successful"
                )
        );

        entityManager.clear();

        // Verify that no partial database records remain.
        Long paymentsAfter = transactionTemplate.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(p) FROM Payment p", Long.class
                ).getSingleResult()
        );

        Long ordersAfter = transactionTemplate.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(o) FROM Order o", Long.class
                ).getSingleResult()
        );

        Long orderItemsAfter = transactionTemplate.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(i) FROM OrderItem i", Long.class
                ).getSingleResult()
        );

        Integer finalStock = transactionTemplate.execute(status ->
                entityManager.find(Product.class, data.productId())
                        .getQuantity()
        );

        assertEquals(paymentsBefore, paymentsAfter, "Payment record should be rolled back");
        assertEquals(ordersBefore, ordersAfter, "Order record should be rolled back");
        assertEquals(orderItemsBefore, orderItemsAfter, "Order items should be rolled back");
        assertEquals(1, finalStock, "Product stock should remain unchanged");
    }

    private record RollbackTestData(
            Long productId,
            Long addressId
    ) {}



    private record TestData(
            Long productId,
            Long addressAId,
            Long addressBId
    ) {}

}
