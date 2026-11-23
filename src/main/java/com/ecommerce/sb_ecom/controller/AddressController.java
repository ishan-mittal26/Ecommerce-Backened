package com.ecommerce.sb_ecom.controller;


import com.ecommerce.sb_ecom.model.User;
import com.ecommerce.sb_ecom.payload.AddressDTO;
import com.ecommerce.sb_ecom.service.AddressService;
import com.ecommerce.sb_ecom.service.AddressServiceImpl;
import com.ecommerce.sb_ecom.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController
{

    private AuthUtil authUtil;

    private AddressService addressService;

    public AddressController(AuthUtil authUtil, AddressService addressServiceImpl)
    {
        this.authUtil=authUtil;
        this.addressService=addressServiceImpl;
    }

    @PostMapping("/address")
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody  AddressDTO addressDTO)
    {

         User user = authUtil.loggedInUser();

      AddressDTO addressDTOs =   addressService.createAddress(addressDTO,user);
      return new ResponseEntity<AddressDTO>(addressDTOs, HttpStatus.CREATED);

    }

    @GetMapping("/address")
    public ResponseEntity<List<AddressDTO>> getAddress()
    {
       List< AddressDTO> addressDTOs =   addressService.getAddress();
        return new ResponseEntity<List<AddressDTO>>(addressDTOs, HttpStatus.OK);

    }

    @GetMapping("/address/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId)
    {
        AddressDTO addressDTOs =   addressService.getAddressById(addressId);
        return new ResponseEntity<AddressDTO>(addressDTOs, HttpStatus.FOUND);

    }

    @GetMapping("/users/address")
    public ResponseEntity<List<AddressDTO>> getUserAddress()
    {
        User user  =authUtil.loggedInUser();
        List< AddressDTO> addressDTOs =   addressService.getUserAddress(user);
        return new ResponseEntity<List<AddressDTO>>(addressDTOs, HttpStatus.OK);

    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@PathVariable Long addressId ,@RequestBody AddressDTO addressDTO)
    {
        AddressDTO addressDTOs =   addressService.updateAddress(addressId,addressDTO);
        return new ResponseEntity<AddressDTO>(addressDTOs, HttpStatus.OK);

    }

    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId )
    {
        String status =   addressService.deleteAddress(addressId);
        return new ResponseEntity<String>(status, HttpStatus.OK);

    }




}
