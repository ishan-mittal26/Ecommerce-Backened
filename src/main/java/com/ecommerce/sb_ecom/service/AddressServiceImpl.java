package com.ecommerce.sb_ecom.service;

import com.ecommerce.sb_ecom.exceptions.APIException;
import com.ecommerce.sb_ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.sb_ecom.model.Address;
import com.ecommerce.sb_ecom.model.User;
import com.ecommerce.sb_ecom.payload.AddressDTO;
import com.ecommerce.sb_ecom.repositories.AddressRepository;
import com.ecommerce.sb_ecom.repositories.UserRepository;
import com.ecommerce.sb_ecom.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService
{


    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(AddressServiceImpl.class);
    @Override
    public AddressDTO createAddress(AddressDTO addressDTO , User user) {


        Address address = modelMapper.map(addressDTO,Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);

        user.setAddresses(addressList);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress,AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddress() {

        List<Address> addresses =  addressRepository.findAll();


        List<AddressDTO> addressDTOs = addresses.stream().map(add->
            modelMapper.map(add,AddressDTO.class)).collect(Collectors.toList());
        return addressDTOs ;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow(()->new ResourceNotFoundException("address","addressid",addressId));
        return modelMapper.map(address,AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddress(User user) {

        List<Address> address = user.getAddresses();

        List<AddressDTO> addressDTOs  = address.stream().map(addr -> modelMapper.map(addr,AddressDTO.class)).toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO updateAddress(Long addressId , AddressDTO addressDTO) {
        Address address = addressRepository.findById(addressId).orElseThrow(()->new ResourceNotFoundException("address","addresid",addressId));


            address.setCity(addressDTO.getCity());
            address.setPincode(addressDTO.getPincode());
            address.setState(addressDTO.getState());
            address.setCountry(addressDTO.getCountry());
            address.setStreet(addressDTO.getStreet());
            address.setBuildingName(addressDTO.getBuildingName());

            Address updatesAddress = addressRepository.save(address);

            User user = address.getUser();
            user.getAddresses().removeIf(addr-> addr.getAddressId().equals(addressId));
            user.getAddresses().add(updatesAddress);

            userRepository.save(user);

        return modelMapper.map(updatesAddress,AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {

        Address address = addressRepository.findById(addressId).orElseThrow(()->new ResourceNotFoundException("address","addresid",addressId));

        User user = address.getUser();
        user.getAddresses().removeIf(addr-> addr.getAddressId().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(address);
        return "address delete succusefull "  + addressId;
    }
}
