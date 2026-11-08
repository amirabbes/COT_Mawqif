package tn.supcom.cot.core.entities;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.UUID;

/**
 * Static metamodel for {@link tn.supcom.cot.core.entities.UUIDPKEntity}
 **/
@StaticMetamodel(UUIDPKEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class UUIDPKEntity_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #version
	 **/
	public static final String VERSION = "version";

	
	/**
	 * Static metamodel type for {@link tn.supcom.cot.core.entities.UUIDPKEntity}
	 **/
	public static volatile MappedSuperclassType<UUIDPKEntity> class_;
	
	/**
	 * Static metamodel for attribute {@link tn.supcom.cot.core.entities.UUIDPKEntity#id}
	 **/
	public static volatile SingularAttribute<UUIDPKEntity, UUID> id;
	
	/**
	 * Static metamodel for attribute {@link tn.supcom.cot.core.entities.UUIDPKEntity#version}
	 **/
	public static volatile SingularAttribute<UUIDPKEntity, Long> version;

}

