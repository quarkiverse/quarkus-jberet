package org.acme.batch.components.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Auctions")
public class Auction {
    @Id
    Long id;
    Integer itemId;
    Integer quantity;
    Long bid;
    Long buyout;
}
