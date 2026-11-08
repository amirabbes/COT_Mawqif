package tn.supcom.cot.core.entities;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link tn.supcom.cot.core.entities.Address}
 **/
@StaticMetamodel(Address.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Address_ extends UUIDPKEntity_ {

	
	/**
	 * @see #street
	 **/
	public static final String STREET = "street";
	
	/**
	 * @see #city
	 **/
	public static final String CITY = "city";

	
	/**
	 * Static metamodel type for {@link tn.supcom.cot.core.entities.Address}
	 **/
	public static volatile EntityType<Address> class_;
	
	/**
	 * Static metamodel for attribute {@link tn.supcom.cot.core.entities.Address#street}
	 **/
	public static volatile SingularAttribute<Address, String> street;
	
	/**
	 * Static metamodel for attribute {@link tn.supcom.cot.core.entities.Address#city}
	 **/
	public static volatile SingularAttribute<Address, String> city;

}

