package com.ecommerce.sb_ecom.service;

import com.ecommerce.sb_ecom.exceptions.APIException;
import com.ecommerce.sb_ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.sb_ecom.model.Cart;
import com.ecommerce.sb_ecom.model.Category;
import com.ecommerce.sb_ecom.model.Product;
import com.ecommerce.sb_ecom.model.User;
import com.ecommerce.sb_ecom.payload.CartDTO;
import com.ecommerce.sb_ecom.payload.ProductDTO;
import com.ecommerce.sb_ecom.payload.ProductResponse;
import com.ecommerce.sb_ecom.repositories.CartRepository;
import com.ecommerce.sb_ecom.repositories.CategoryRepository;
import com.ecommerce.sb_ecom.repositories.ProductRepository;
import com.ecommerce.sb_ecom.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService
{

    @Autowired
    private CartServiceImpl cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Autowired
    private AuthUtil authUtil;


    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {


        Category category = categoryRepository.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("category" , "categoryId" , categoryId));

        // validation check product already exists or not

       List<Product> products =  category.getProducts();  // this happens only because we have done bidirectinoal mapping in category to product

        boolean isProductNotPresent =true;
        for(Product value : products)
        {
            if(value.getProductName().equals(productDTO.getProductName()))
            {
                isProductNotPresent = false;
                break;
            }
        }

        if(isProductNotPresent)
        {

            Product product = modelMapper.map(productDTO,Product.class);
            product.setImage("default.png");
            product.setCategory(category);
            product.setUser(authUtil.loggedInUser());


            Double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01)* product.getPrice());
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);



            return modelMapper.map(savedProduct , ProductDTO.class);
        }
        else {
            throw new APIException("product already exists");
        }


    }

    @Override
    public ProductResponse getAllProducts(String keyword, String category, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable  = PageRequest.of(pageNumber,pageSize,sortByAndOrder);

        Specification<Product> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if(keyword!=null && !keyword.isEmpty())
        {
            spec = spec.and((root,query,criteriaBuilder)->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")),"%" + keyword.toLowerCase() + "%"));
        }
        if(category!=null && !category.isEmpty())
        {
            spec = spec.and((root,query,criteriaBuilder)->
                    criteriaBuilder.like(root.get("category").get("categoryName"),category));
        }

        Page<Product> pageObj= productRepository.findAll(spec,pageable);
//      List<Product> product =   productRepository.findAll();
        List<Product> product = pageObj.getContent();

        if(product.isEmpty())
        {
            throw new APIException("no products exist");
        }
        List<ProductDTO> productDTOS =  product.stream().map(pro -> {
            ProductDTO productDTO  =modelMapper.map(pro , ProductDTO.class);
            productDTO.setImage(constructImageUrl(pro.getImage()));
            return productDTO;


        }).collect(Collectors.toList());

     ProductResponse productResponse  = new ProductResponse();
     productResponse.setContent(productDTOS);

     productResponse.setPageNumber(pageObj.getNumber());
     productResponse.setPageSize(pageObj.getSize());
     productResponse.setTotalElements((int)pageObj.getTotalElements());
     productResponse.setTotalPages(pageObj.getTotalPages());
     productResponse.setLastPage(pageObj.isLast());
     return productResponse;

    }

    private String constructImageUrl(String imageName)
    {
        return imageBaseUrl.endsWith("/")?imageBaseUrl+imageName : imageBaseUrl + "/"+ imageName;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Category category = categoryRepository.findById(categoryId).orElseThrow(()->new ResourceNotFoundException("category",  "categoryid" ,categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable  = PageRequest.of(pageNumber,pageSize,sortByAndOrder);

        Page<Product> pageObj  =productRepository.findByCategory(category,pageable);

        List<Product> product = pageObj.getContent();

       List<ProductDTO> productDTOS = product.stream().map(pro->modelMapper.map(pro,ProductDTO.class)).toList();

       ProductResponse productResponse  =new ProductResponse();

       productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageObj.getNumber());
        productResponse.setPageSize(pageObj.getSize());
        productResponse.setTotalElements((int)pageObj.getTotalElements());
        productResponse.setTotalPages(pageObj.getTotalPages());
        productResponse.setLastPage(pageObj.isLast());

       return productResponse;

    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword,Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();

        Pageable pageable =  PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product> pageObj = productRepository.findByProductNameLikeIgnoreCase('%' +  keyword + '%' , pageable);
        List<Product> products =  pageObj.getContent();

        List<ProductDTO> productDTOS = products.stream().map(pro->modelMapper.map(pro,ProductDTO.class)).toList();

        ProductResponse productResponse  =new ProductResponse();

        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageObj.getNumber());
        productResponse.setPageSize(pageObj.getSize());
        productResponse.setTotalElements((int)pageObj.getTotalElements());
        productResponse.setTotalPages(pageObj.getTotalPages());
        productResponse.setLastPage(pageObj.isLast());

        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId,  ProductDTO productDTO) {

       Product productFromDb =  productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("product","productid" ,productId));


       Product product = modelMapper.map(productDTO,Product.class);
       productFromDb.setProductName(product.getProductName());
       productFromDb.setDescription(product.getDescription());
       productFromDb.setQuantity(product.getQuantity());
       productFromDb.setDiscount(product.getDiscount());
       productFromDb.setPrice(product.getPrice());
       productFromDb.setSpecialPrice(product.getSpecialPrice());

      Product savedProduct =  productRepository.save(productFromDb);

       List<Cart> carts = cartRepository.findCartsByProductId(productId);

       List<CartDTO> cartDTOs = carts.stream().map(cart->{
           CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
           List<ProductDTO> products = cart.getCartItems().stream().map(p->modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());
           cartDTO.setProducts(products);
           return cartDTO;
       }).collect(Collectors.toList());



       cartDTOs.forEach(cart-> cartService.updateProductInCarts(cart.getCartId(),productId));
      return modelMapper.map(savedProduct,ProductDTO.class);



    }

    @Override
    public ProductDTO deleteProduct(Long productId) {

       Product productFromDb=  productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("product","productid",productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart->cartService.deleteProductFromCart(cart.getCartId(),productId));

       productRepository.delete(productFromDb);
        return modelMapper.map(productFromDb,ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {

        Product productFromDb = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("product","productid",productId));

        String fileName = fileService.uploadImage(path,image);

        productFromDb.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDb);

        return modelMapper.map(updatedProduct, ProductDTO.class);

    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable  = PageRequest.of(pageNumber,pageSize,sortByAndOrder);



        Page<Product> pageObj= productRepository.findAll(pageable);

        List<Product> product = pageObj.getContent();

        if(product.isEmpty())
        {
            throw new APIException("no products exist");
        }
        List<ProductDTO> productDTOS =  product.stream().map(pro -> {
            ProductDTO productDTO  =modelMapper.map(pro , ProductDTO.class);
            productDTO.setImage(constructImageUrl(pro.getImage()));
            return productDTO;


        }).collect(Collectors.toList());

        ProductResponse productResponse  = new ProductResponse();
        productResponse.setContent(productDTOS);

        productResponse.setPageNumber(pageObj.getNumber());
        productResponse.setPageSize(pageObj.getSize());
        productResponse.setTotalElements((int)pageObj.getTotalElements());
        productResponse.setTotalPages(pageObj.getTotalPages());
        productResponse.setLastPage(pageObj.isLast());
        return productResponse;

    }


    public ProductResponse getAllProductsForSeller(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable  = PageRequest.of(pageNumber,pageSize,sortByAndOrder);

        User user  =authUtil.loggedInUser();
        Page<Product> pageObj= productRepository.findByUser(user,pageable);

        List<Product> product = pageObj.getContent();

        if(product.isEmpty())
        {
            throw new APIException("no products exist");
        }
        List<ProductDTO> productDTOS =  product.stream().map(pro -> {
            ProductDTO productDTO  =modelMapper.map(pro , ProductDTO.class);
            productDTO.setImage(constructImageUrl(pro.getImage()));
            return productDTO;


        }).collect(Collectors.toList());

        ProductResponse productResponse  = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageObj.getNumber());
        productResponse.setPageSize(pageObj.getSize());
        productResponse.setTotalElements((int)pageObj.getTotalElements());
        productResponse.setTotalPages(pageObj.getTotalPages());
        productResponse.setLastPage(pageObj.isLast());
        return productResponse;

    }
}
