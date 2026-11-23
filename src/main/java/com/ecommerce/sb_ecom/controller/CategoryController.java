package com.ecommerce.sb_ecom.controller;

import com.ecommerce.sb_ecom.config.AppConstants;
import com.ecommerce.sb_ecom.model.Category;
import com.ecommerce.sb_ecom.payload.CategoryDTO;
import com.ecommerce.sb_ecom.payload.CategoryResponse;
import com.ecommerce.sb_ecom.service.CategoryService;
import com.ecommerce.sb_ecom.service.CategoryServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController
{


    @Autowired
    private CategoryServiceImpl categoryService;

//    public CategoryController(CategoryService categoryService) {
//        this.categoryService = categoryService;
//    }

    //@RequestMapping(value="/api/public/categories" , method = RequestMethod.GET) // alternative way
    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories(@RequestParam(name="pageNumber" , defaultValue = AppConstants.PAGE_NUMBER,required = false) Integer pageNumber ,
                                                             @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE,required = false)Integer pageSize,
                                                             @RequestParam(name="sortBy" , defaultValue = AppConstants.SORT_CATEGORIES_BY , required = false) String sortBy,
                                                             @RequestParam(name="sortOrder" , defaultValue = AppConstants.SORT_DIR ,required = false) String sortOrder){
        CategoryResponse category = categoryService.getAllCategories(pageNumber , pageSize,sortBy,sortOrder);
        return new ResponseEntity<>(category,HttpStatus.OK);
    }

    // @valid is hibernate validator and check for the constraintes are fullfiles for javca class or entity

    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO)
    {

       CategoryDTO categoryDTO1 =  categoryService.createCategory(categoryDTO);
        return new ResponseEntity<>( categoryDTO1,HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO>  deleteCategory(@PathVariable Long categoryId)
    {

            CategoryDTO categoryDTO = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(categoryDTO,HttpStatus.OK);
//        try
//        {
//            String status = categoryService.deleteCategory(categoryId);
//            return new ResponseEntity<>(status, HttpStatus.OK);
//            // return responseentity.ok(status);
//            //responseentity.status(httpsttus.ok).body(status);
//
//        }catch(ResponseStatusException e)
//        {
//
//            return new ResponseEntity<>(e.getReason(),e.getStatusCode());
//        }

    }


    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@RequestBody CategoryDTO categoryDTO , @PathVariable Long categoryId)
    {

        CategoryDTO savedCategory =  categoryService.updateCategory(categoryDTO,categoryId);
        return new ResponseEntity<>(savedCategory , HttpStatus.OK);
//        try
//        {
//            Category savedCategory =  categoryService.updateCategory(category,categoryId);
//            return new ResponseEntity<>("updatd succesfully" , HttpStatus.OK);
//
//
//        }
//        catch(ResponseStatusException e)
//        {
//
//            return new ResponseEntity<>(e.getReason(),e.getStatusCode());
//        }

    }

}
