package tn.supcom.cot.core.entities;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link tn.supcom.cot.core.entities.Address}
 **/
@StaticMetamodel(Address.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Address extends _UUIDPKEntity {

	
	/**
	 * @see #street
	 **/
	String STREET = "street";
	
	/**
	 * @see #city
	 **/
	String CITY = "city";

	
	/**
	 * Static metamodel for attribute {@link Address#street}
	 **/
	TextAttribute<Address> street = new TextAttributeRecord<>(STREET);
	
	/**
	 * Static metamodel for attribute {@link Address#city}
	 **/
	TextAttribute<Address> city = new TextAttributeRecord<>(CITY);

}

