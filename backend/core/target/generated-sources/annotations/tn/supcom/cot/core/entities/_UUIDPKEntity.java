package tn.supcom.cot.core.entities;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link tn.supcom.cot.core.entities.UUIDPKEntity}
 **/
@StaticMetamodel(UUIDPKEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _UUIDPKEntity {

	
	/**
	 * @see #id
	 **/
	String ID = "id";
	
	/**
	 * @see #version
	 **/
	String VERSION = "version";

	
	/**
	 * Static metamodel for attribute {@link UUIDPKEntity#id}
	 **/
	SortableAttribute<UUIDPKEntity> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * Static metamodel for attribute {@link UUIDPKEntity#version}
	 **/
	SortableAttribute<UUIDPKEntity> version = new SortableAttributeRecord<>(VERSION);

}

