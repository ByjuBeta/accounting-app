package com.accountingapp.common.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_line1")
    private String line1;

    @Column(name = "address_line2")
    private String line2;

    @Column(name = "address_city")
    private String city;

    @Column(name = "address_state")
    private String state;

    @Column(name = "address_postal_code")
    private String postalCode;

    @Column(name = "address_country")
    private String country;
}
