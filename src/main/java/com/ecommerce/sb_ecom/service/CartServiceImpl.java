package com.ecommerce.sb_ecom.service;

import com.ecommerce.sb_ecom.exceptions.APIException;
import com.ecommerce.sb_ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.sb_ecom.model.Cart;
import com.ecommerce.sb_ecom.model.CartItem;
import com.ecommerce.sb_ecom.model.Product;
import com.ecommerce.sb_ecom.payload.CartDTO;
import com.ecommerce.sb_ecom.payload.CartItemDTO;
import com.ecommerce.sb_ecom.payload.ProductDTO;
import com.ecommerce.sb_ecom.repositories.CartItemRepository;
import com.ecommerce.sb_ecom.repositories.CartRepository;
import com.ecommerce.sb_ecom.repositories.ProductRepository;
import com.ecommerce.sb_ecom.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService
{

    @Autowired
    CartRepository cartRepository;


    // why there is a need of authutil class, as there will be instances where we need
    // the detail of authenticated user, as in below methods we need the details of authenticated user,
    // so onw way we create the authenticated user implementation to this class, but for simplicity we are
    // making a help class with name auth util which wil have all the necessary methods for getting the details of authenticated user;
    @Autowired
    AuthUtil authUtil;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;




    public CartDTO addProductToCart(Long productId, Integer quantity)
    {


        // find existing cart for a particular user if there or not if not create one

        Cart cart = createCart();



        // retrieve product details
        Product product =productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException("product","productid",productId));

        //perform validations

       CartItem cartItem =  cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(),productId);
       if(cartItem!=null)
       {
           throw new APIException("product" + product.getProductName() + "already exists");
       }

       if(product.getQuantity()==0)
       {
           throw new APIException(product.getProductName() + "is noit available");
       }

       if(product.getQuantity()<quantity)
       {
           throw new APIException("please make an order of the " + product.getProductName() + "less than or equal to the quantiyt" + product.getQuantity());
       }


        //create cart item
        CartItem newCartItem = new CartItem();
       newCartItem.setProduct(product);
       newCartItem.setCart(cart);
       newCartItem.setQuantity(quantity);
       newCartItem.setDiscount(product.getDiscount());
       newCartItem.setProductPrice(product.getSpecialPrice());


       cartItemRepository.save(newCartItem);

       // we are not changing the quanity as the order is only added to cart, it has not been placed
       product.setQuantity(product.getQuantity());

       cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice()*quantity));


       //saving cart
        cartRepository.save(cart);
        cart.getCartItems().add(newCartItem);

        CartDTO cardDTO = modelMapper.map(cart,CartDTO.class);

        List<CartItem> cartItems=cart.getCartItems();


       Stream<ProductDTO> productDTOStream=cartItems.stream().map(item->{
           ProductDTO map = modelMapper.map(item.getProduct(),ProductDTO.class);
           map.setQuantity(item.getQuantity());
           return map;
       });
       cardDTO.setProducts(productDTOStream.toList());

        //return updated cart
        return cardDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity)
    {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart  =cartRepository.findById(cartId).orElseThrow(()->new ResourceNotFoundException("cart","cartid",cartId));

        Product product =productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException("product","productid",productId));

        if(product.getQuantity()==0)
        {
            throw new APIException(product.getProductName() + "is noit available");
        }

        if(product.getQuantity()<quantity)
        {
            throw new APIException("please make an order of the " + product.getProductName() + "less than or equal to the quantiyt" + product.getQuantity());
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if(cartItem==null)
        {
            throw new APIException("product" + product.getProductName() + " not available in the cart");
        }

        int newQuantity = cartItem.getQuantity()+quantity;
        if(newQuantity<0)
        {
            throw new APIException("THE RESULTING QUANTITY CANNOT BE NEGATIVE");
        }
        if(newQuantity==0)
        {
            deleteProductFromCart(cartId,productId);
        }
        else {

            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity()+quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice()+(cartItem.getProductPrice() *quantity));

            cartRepository.save(cart);
        }

        CartItem updatedItem = cartItemRepository.save(cartItem);


        if(updatedItem.getQuantity()==0)
        {
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }

        CartDTO cartDTO  =modelMapper.map(cart,CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item->{
            ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity(item.getQuantity());

            return prd;
        });
        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {

        Cart cart = cartRepository.findById(cartId).orElseThrow(()->new ResourceNotFoundException("cart","cartId",cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);
        if(cartItem==null)
        {
            throw  new ResourceNotFoundException("product","productId",productId);
        }
        cart.setTotalPrice(cart.getTotalPrice()-(cartItem.getProductPrice()*cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);


        return "product" + cartItem.getProduct().getProductName()+ "removed";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {

        Cart cart  =cartRepository.findById(cartId).orElseThrow(()->new ResourceNotFoundException("cart","cartid",cartId));

        Product product =productRepository.findById(productId).orElseThrow(()->new ResourceNotFoundException("product","productid",productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if(cartItem==null)
        {
            throw new APIException("product" + product.getProductName() + "not available");
        }
        double cartPrice = cart.getTotalPrice()-(cartItem.getProductPrice()*cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(cartPrice+(cartItem.getProductPrice()*cartItem.getQuantity()));

        cartItem=cartItemRepository.save(cartItem);

    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if(carts.size()==0)
        {
            throw new APIException("no cart exist");
        }

        List<CartDTO> cartDTOs = carts.stream().map(cart->{
            CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
            List<ProductDTO> products = cart.getCartItems().stream().map(p->{
                ProductDTO productDTO = modelMapper.map(p.getProduct(),ProductDTO.class);
                productDTO.setQuantity(p.getQuantity());
                return productDTO;
            }).collect(Collectors.toList());
            cartDTO.setProducts(products);
            return cartDTO;
        }).collect(Collectors.toList());
        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {

        Cart cart = cartRepository.findCartByEmailAndCartId(emailId,cartId);
        if(cart==null)
        {
            throw new ResourceNotFoundException("cart","cartid",cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

        // this line of code is written because we are convert product into product dto so the quyeantity set in product willl
        // be allocated to product dto but we want the quantity added to the cart
        cart.getCartItems().forEach(c->c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> products = cart.getCartItems().stream().map(p->modelMapper.map(p.getProduct(),ProductDTO.class)).collect(Collectors.toList());
        cartDTO.setProducts(products);
        return cartDTO;
    }

    private Cart createCart()
    {

        Cart userCart = cartRepository.findCartByEmail((authUtil.loggedInEmail()));




        if(userCart!=null)
        {
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart = cartRepository.save(cart);


        return newCart;
    };

    @Transactional
    public String createOrUpdateCart(List<CartItemDTO> cartItems) {

        //get user;s email
        String emailid = authUtil.loggedInEmail();

        //check if existing cart is available or create a new one
        Cart existingCart  = cartRepository.findCartByEmail(emailid);
        if(existingCart==null)
        {
            existingCart=new Cart();
            existingCart.setTotalPrice(0.0);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart=cartRepository.save(existingCart);

        }
        else {
            // clear all current items in the existing cart
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }


        double totalPrice=0.0;

        for(CartItemDTO cartItemDTO:cartItems)
        {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            Product product =  productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("product","productid",productId));

            // directly update product stock and total prioce
//            product.setQuantity(product.getQuantity()-quantity);
            totalPrice += product.getSpecialPrice()*quantity;

            // create and save cart item

            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());

            cartItemRepository.save(cartItem);
        }


        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);

        return "Cart created/uipdted with new item succefully";
    }
}
