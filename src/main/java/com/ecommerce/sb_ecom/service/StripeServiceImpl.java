package com.ecommerce.sb_ecom.service;

import com.ecommerce.sb_ecom.payload.StripePaymentDto;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StripeServiceImpl implements StripeService
{
    @Value("${stripe.secret.key}")
    private String stripeApiKey;


    @Override
    public PaymentIntent paymentIntent(StripePaymentDto stripePaymentDto) throws StripeException {

        Customer customer;
        StripeClient client = new StripeClient(stripeApiKey);


        CustomerSearchParams param =
                CustomerSearchParams.builder()
                        .setQuery("email:'" + stripePaymentDto.getEmail() +"'")
                        .build();

        StripeSearchResult<Customer> stripeSearchResult =
                client.v1().customers().search(param);

        if(stripeSearchResult.getData().isEmpty())
        {
            // create new customer
            CustomerCreateParams params =
                    CustomerCreateParams.builder()
                            .setName(stripePaymentDto.getName())
                            .setEmail(stripePaymentDto.getEmail())
                            .setAddress(
                                    CustomerCreateParams.Address.builder().setLine1(stripePaymentDto.getAddress().getStreet())
                                            .setCity(stripePaymentDto.getAddress().getCity())
                                            .setState(stripePaymentDto.getAddress().getState())
                                            .setPostalCode(stripePaymentDto.getAddress().getPincode())
                                            .setCountry(stripePaymentDto.getAddress().getCountry())
                                            .build()

                            )
                            .build();
             customer = client.v1().customers().create(params);
        }
        else {
            //fetch the customer that exists
            customer = stripeSearchResult.getData().get(0);
        }


        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(stripePaymentDto.getAmount())
                        .setCurrency(stripePaymentDto.getCurrency())
                        .setCustomer(customer.getId())
                        .setDescription(stripePaymentDto.getDescription())
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();
        PaymentIntent paymentIntent = client.v1().paymentIntents().create(params);

        return paymentIntent;
    }
}
