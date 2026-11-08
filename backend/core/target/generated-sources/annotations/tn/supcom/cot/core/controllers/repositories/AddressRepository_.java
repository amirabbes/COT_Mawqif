package tn.supcom.cot.core.controllers.repositories;

import jakarta.annotation.Generated;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.data.Order;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.impl.PageRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.UUID;
import java.util.stream.Stream;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;
import static org.hibernate.query.Order.asc;
import static org.hibernate.query.SortDirection.*;
import org.hibernate.query.specification.SelectionSpecification;
import tn.supcom.cot.core.entities.Address;

/**
 * Implements Jakarta Data repository {@link tn.supcom.cot.core.controllers.repositories.AddressRepository}
 **/
@Dependent
@Generated("org.hibernate.processor.HibernateProcessor")
public class AddressRepository_ implements AddressRepository {

	
	/**
	 * @see #findByStreet(String)
	 **/
	static final String FIND_BY_STREET_String = "select a from Address a where a.street = :street";
	
	/**
	 * @see #findByCity(String)
	 **/
	static final String FIND_BY_CITY_String = "select a from Address a where a.city = :city";

	
	protected @Nonnull StatelessSession session;
	
	public AddressRepository_(@Nonnull StatelessSession session) {
		this.session = session;
	}
	
	public @Nonnull StatelessSession session() {
		return session;
	}
	
	@PersistenceUnit
	private EntityManagerFactory sessionFactory;
	
	@PostConstruct
	private void openSession() {
		session = sessionFactory.unwrap(SessionFactory.class).openStatelessSession();
	}
	
	@PreDestroy
	private void closeSession() {
		session.close();
	}
	
	@Inject
	AddressRepository_() {
	}
	
	@Override
	public  <S extends Address> S insert(@Nonnull S entity) {
		requireNonNull(entity, "Null entity");
		try {
			session.insert(entity);
		}
		catch (ConstraintViolationException _ex) {
			throw new EntityExistsException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return entity;
	}
	
	@Override
	public  <S extends Address> List<S> insertAll(@Nonnull List<S> entities) {
		requireNonNull(entities, "Null entities");
		try {
			session.insertMultiple(entities);
		}
		catch (ConstraintViolationException _ex) {
			throw new EntityExistsException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return entities;
	}
	
	@Override
	public  <S extends Address> S update(@Nonnull S entity) {
		requireNonNull(entity, "Null entity");
		try {
			session.update(entity);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return entity;
	}
	
	@Override
	public  <S extends Address> List<S> updateAll(@Nonnull List<S> entities) {
		requireNonNull(entities, "Null entities");
		try {
			session.updateMultiple(entities);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return entities;
	}
	
	@Override
	public  <S extends Address> S save(@Nonnull S entity) {
		requireNonNull(entity, "Null entity");
		try {
			session.upsert(entity);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return entity;
	}
	
	@Override
	public  <S extends Address> List<S> saveAll(@Nonnull List<S> entities) {
		requireNonNull(entities, "Null entities");
		try {
			session.upsertMultiple(entities);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return entities;
	}
	
	@Override
	public void delete(@Nonnull Address entity) {
		requireNonNull(entity, "Null entity");
		try {
			session.delete(entity);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	@Override
	public void deleteAll(@Nonnull List<? extends Address> entities) {
		requireNonNull(entities, "Null entities");
		try {
			session.deleteMultiple(entities);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Execute the query {@value #FIND_BY_STREET_String}.
	 *
	 * @see tn.supcom.cot.core.controllers.repositories.AddressRepository#findByStreet(String)
	 **/
	@Override
	public List<Address> findByStreet(String street) {
		try {
			var _select = session.createSelectionQuery(FIND_BY_STREET_String, Address.class)
				.setParameter("street", street);
			return _select
				.getResultList();
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Execute the query {@value #FIND_BY_CITY_String}.
	 *
	 * @see tn.supcom.cot.core.controllers.repositories.AddressRepository#findByCity(String)
	 **/
	@Override
	public List<Address> findByCity(String city) {
		try {
			var _select = session.createSelectionQuery(FIND_BY_CITY_String, Address.class)
				.setParameter("city", city);
			return _select
				.getResultList();
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Find {@link Address} by identifier.
	 *
	 * @see tn.supcom.cot.core.controllers.repositories.AddressRepository#findById(UUID)
	 **/
	@Override
	public Optional<Address> findById(@Nonnull UUID id) {
		requireNonNull(id, "Null id");
		try {
			return ofNullable(session.get(Address.class, id));
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Find {@link Address}.
	 *
	 * @see tn.supcom.cot.core.controllers.repositories.AddressRepository#findAll()
	 **/
	@Override
	public Stream<Address> findAll() {
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createQuery(Address.class);
		var _entity = _query.from(Address.class);
		_query.where(
		);
		try {
			var _select = session.createSelectionQuery(_query);
			return _select
				.getResultStream();
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Find {@link Address}.
	 *
	 * @see tn.supcom.cot.core.controllers.repositories.AddressRepository#findAll(PageRequest,Order)
	 **/
	@Override
	public Page<Address> findAll(@Nonnull PageRequest pageRequest, @Nonnull Order<Address> sortBy) {
		requireNonNull(pageRequest, "Null pageRequest");
		requireNonNull(sortBy, "Null sortBy");
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createQuery(Address.class);
		var _entity = _query.from(Address.class);
		_query.where(
		);
		var _spec = SelectionSpecification.create(_query);
		for (var _sort : sortBy.sorts()) {
			_spec.sort(asc(Address.class, _sort.property())
						.reversedIf(_sort.isDescending())
						.ignoringCaseIf(_sort.ignoreCase()));
		}
		try {
			var _select = _spec.createQuery(session);
			long _totalResults = 
					pageRequest.requestTotal()
							? _select
									.getResultCount()
							: -1;
			var _results = _select
				.setFirstResult((int) (pageRequest.page()-1) * pageRequest.size())
				.setMaxResults(pageRequest.size())
				.getResultList();
			return new PageRecord<>(pageRequest, _results, _totalResults);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Delete {@link Address} by identifier.
	 *
	 * @see tn.supcom.cot.core.controllers.repositories.AddressRepository#deleteById(UUID)
	 **/
	@Override
	public void deleteById(@Nonnull UUID id) {
		requireNonNull(id, "Null id");
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createCriteriaDelete(Address.class);
		var _entity = _query.from(Address.class);
		_query.where(
				_builder.equal(_entity.get("{id}"), id)
		);
		try {
			session.createMutationQuery(_query).executeUpdate();
		}
		catch (NoResultException _ex) {
			throw new EmptyResultException(_ex.getMessage(), _ex);
		}
		catch (NonUniqueResultException _ex) {
			throw new jakarta.data.exceptions.NonUniqueResultException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}

}

