/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.delegate;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.*;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.app.validation.DistinguishNameValidator;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Organizational delegation resource.
 */
@Path("/security/delegate")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class DelegateOrgResource {

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private DelegateOrgRepository repository;

	@Autowired
	private PaginationJson paginationJson;

	@Autowired
	protected IamProvider[] iamProvider;

	/**
	 * Receiver function from the receiver type.
	 */
	private final Map<ReceiverType, Function<String, ResourceOrg>> toReceiver = new EnumMap<>(ReceiverType.class);

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORDERED_COLUMNS = new HashMap<>();

	static {
		ORDERED_COLUMNS.put("id", "id");
		ORDERED_COLUMNS.put("name", "name");
		ORDERED_COLUMNS.put("type", "type");
		ORDERED_COLUMNS.put("receiver", "receiver");
		ORDERED_COLUMNS.put("receiverType", "receiverType");
		ORDERED_COLUMNS.put("canAdmin", "canAdmin");
		ORDERED_COLUMNS.put("canWrite", "canWrite");
	}

	/**
	 * Default constructor initializing the validators.
	 */
	public DelegateOrgResource() {
		// Check the user/company/group is visible
		toReceiver.put(ReceiverType.USER, r -> getUser().findByIdExpected(securityHelper.getLogin(), r));
		toReceiver.put(ReceiverType.COMPANY, r -> getCompany().findByIdExpected(securityHelper.getLogin(), r));
		toReceiver.put(ReceiverType.GROUP, r -> getGroup().findByIdExpected(securityHelper.getLogin(), r));
	}

	/**
	 * Converter from {@link DelegateOrg} to {@link DelegateOrgLightVo}
	 *
	 * @param entity The entity to convert.
	 * @return The initialized bean corresponding to the entity with fetched description for related user and group.
	 */
	public DelegateOrgLightVo toVo(final DelegateOrg entity) {
		final var vo = new DelegateOrgLightVo();
		NamedBean.copy(entity, vo);
		vo.copyAuditData(entity, (Function<String, UserOrg>) getUser()::toUser);

		// Map the receiver
		vo.setReceiverType(entity.getReceiverType());
		if (entity.getReceiverType() == ReceiverType.USER) {
			vo.setReceiver(getUser().toUser(entity.getReceiver()));
		} else {
			vo.setReceiver(new NamedBean<>(entity.getReceiver(), entity.getReceiver()));
		}
		vo.setType(entity.getType());
		vo.setCanWrite(entity.isCanWrite());
		vo.setCanAdmin(entity.isCanAdmin());

		// Flag to indicate the principal user can manage this entry
		vo.setManaged(isManagedDelegate(entity));
		if (entity.getType() == DelegateType.GROUP) {
			final var groups = getGroup().findAll();
			if (groups.containsKey(entity.getReferenceID())) {
				// Make nicer the display for group using the CN
				vo.setName(groups.get(entity.getReferenceID()).getName());
			}
		} else if (entity.getType() == DelegateType.TREE) {
			// For TREE mode, the DN is used as name
			vo.setName(entity.getDn());
		}
		return vo;
	}

	/**
	 * Indicate this delegate is managed : so can be updated by the current user. <br>
	 * Is managed when 'canAdmin' flag is set, or this delegate is directly involving the principal user.
	 */
	private boolean isManagedDelegate(final DelegateOrg entity) {
		return entity.isCanAdmin() || entity.getReceiverType() != ReceiverType.USER
				|| !securityHelper.getLogin().equals(entity.getReceiver());
	}

	/**
	 * Retrieve all elements with pagination
	 *
	 * @param uriInfo    pagination data.
	 * @param typeSearch Optional {@link DelegateType} search.
	 * @return all elements with pagination.
	 */
	@GET
	public TableItem<DelegateOrgLightVo> findAll(@Context final UriInfo uriInfo,
			@QueryParam("type") final DelegateType typeSearch) {
		// Trigger cache loading
		getUser().findAll();

		final var pageRequest = paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS);
		final var findAll = repository.findAll(securityHelper.getLogin(),
				StringUtils.trimToEmpty(DataTableAttributes.getSearch(uriInfo)),
				typeSearch, pageRequest);

		// Apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, this::toVo);
	}

	/**
	 * Return a visible entity by its identifier.
	 *
	 * @param id the entity identifier.
	 * @return A visible entity by its identifier.
	 */
	@GET
	@Path("{id:\\d+}")
	public DelegateOrg findById(@PathParam("id") final int id) {
		return repository.findById(securityHelper.getLogin(), id);
	}

	/**
	 * Create a delegate. Rules are :
	 * <ul>
	 * <li>Related company, group or tree must be managed by the current user, directly or via a another parent
	 * delegate.</li>
	 * <li>'write' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * <li>'admin' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * </ul>
	 *
	 * @param vo the object to create.
	 * @return the entity's identifier.
	 */
	@POST
	public int create(final DelegateOrgEditionVo vo) {
		return validateSaveOrUpdate(vo).getId();
	}

	/**
	 * Validate the user changes regarding the current user's right. The associated DN and the real CN will be stored in
	 * database.<br>
	 * Rules, order is important :
	 * <ul>
	 * <li>Related company must be managed by the current user, directly or via a another parent delegate tree/company,
	 * or act as if the company does not exist.</li>
	 * <li>Related group must be managed by the current user, directly or via a another parent delegate group/tree, or
	 * act as if the group does not exist.</li>
	 * <li>Related tree must be managed by the current user, directly or via a another parent delegate tree.</li>
	 * <li>'write' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * <li>'admin' flag cannot be <code>true</code> without already owning an applicable delegate with this flag.</li>
	 * </ul>
	 * Attention, DN is case-sensitive.
	 *
	 * @return the created/update {@link DelegateOrg}
	 */
	private DelegateOrg validateSaveOrUpdate(final DelegateOrgEditionVo importEntry) {
		final var allCompanies = getCompany().findAll();
		final var allGroups = getGroup().findAll();

		// Save the delegate with normalized name
		final var entity = toEntity(importEntry);

		// Get all delegates of current user
		final String dn;
		if (importEntry.getType() == DelegateType.COMPANY) {
			dn = validateCompany(importEntry, allCompanies);
		} else if (importEntry.getType() == DelegateType.GROUP) {
			dn = validateGroup(importEntry, allGroups);
		} else {
			// Tree, CN <- DN
			dn = validateTree(importEntry);

			// Name is ignored for this type in the internal format
			entity.setName("-");
		}

		if (dn == null) {
			// Related resource does not exists
			throw new ForbiddenException();
		}

		// Check there is at least one delegate for this user allowing to write
		// INTO the corresponding DN
		if (repository.findByMatchingDnForAdmin(securityHelper.getLogin(), dn, importEntry.getType()).isEmpty()) {
			throw new ForbiddenException();
		}

		if (importEntry.getId() != null) {
			// Check there is at least one delegate for this user allowing to write FROM the corresponding DN
			validateWriteAccess(importEntry.getId());
		}

		// DN is already normalized
		entity.setDn(dn);
		repository.saveAndFlush(entity);
		return entity;
	}

	/**
	 * Build the entity from the import entry.
	 *
	 * @param importEntry The new delegate.
	 * @return The JPA entity form with validated inputs.
	 */
	private DelegateOrg toEntity(final DelegateOrgEditionVo importEntry) {
		// Validate the related receiver of this delegate
		final var receiver = toReceiver.get(importEntry.getReceiverType()).apply(importEntry.getReceiver());

		final var entity = new DelegateOrg();
		entity.setId(importEntry.getId());
		entity.setName(Normalizer.normalize(importEntry.getName()));
		entity.setCanAdmin(importEntry.isCanAdmin());
		entity.setCanWrite(importEntry.isCanWrite());
		entity.setType(importEntry.getType());
		entity.setReceiver(receiver.getId());
		entity.setReceiverType(importEntry.getReceiverType());
		if (receiver instanceof ContainerOrg) {
			// Store receiver DN only for immutable containers
			entity.setReceiverDn(receiver.getDn());
		}
		return entity;
	}

	/**
	 * Validate and clean the tree DN, and return the corresponding DN.
	 */
	private String validateTree(final DelegateOrgEditionVo importEntry) {
		if (!new DistinguishNameValidator().isValid(importEntry.getName(), null)) {
			// Invalid LDAP syntax, prevent LDAP injection
			throw new ValidationJsonException("tree", "DistinguishName");
		}
		importEntry.setName(StringUtils.trimToEmpty(importEntry.getName()));
		return Normalizer.normalize(importEntry.getName());
	}

	/**
	 * Validate and clean the group name, and return the corresponding DN.
	 */
	private String validateGroup(final DelegateOrgEditionVo importEntry, final Map<String, GroupOrg> allGroups) {
		final var normalizedCN = Normalizer.normalize(importEntry.getName());
		final var group = allGroups.get(normalizedCN);
		if (group != null) {
			importEntry.setName(normalizedCN);
			return group.getDn();
		}
		return null;
	}

	/**
	 * Validate, clean the company name, and return the corresponding DN.
	 */
	private String validateCompany(final DelegateOrgEditionVo importEntry, final Map<String, CompanyOrg> allCompanies) {
		final var normalizedCN = Normalizer.normalize(importEntry.getName());
		if (allCompanies.containsKey(normalizedCN)) {
			importEntry.setName(normalizedCN);
			return allCompanies.get(normalizedCN).getDn();
		}
		return null;
	}

	/**
	 * Update entity.
	 *
	 * @param vo the object to update.
	 */
	@PUT
	public void update(final DelegateOrgEditionVo vo) {
		validateSaveOrUpdate(vo);
	}

	/**
	 * Delete entity. Rules, order is important :
	 * <ul>
	 * <li>Related delegate must exist</li>
	 * <li>Related delegate must be managed by the principal user with 'canAdmin' right, directly or via a another
	 * parent delegate tree/company/.., or act as if the delegate does not exist.</li>
	 * </ul>
	 * Attention, DN is case-sensitive.
	 *
	 * @param id the entity identifier.
	 */
	@DELETE
	@Path("{id:\\d+}")
	public void delete(@PathParam("id") final int id) {

		// Check the related DN
		validateWriteAccess(id);

		// Perform the deletion
		repository.deleteById(id);
	}

	/**
	 * Check the principal user can delete this delegate. 'canAdmin' flag must be enabled.
	 *
	 * @param id the entity identifier.
	 */
	private void validateWriteAccess(final int id) {

		// Get the related delegate
		final var delegate = repository.findOneExpected(id);

		// Check the related DN
		final var dn = delegate.getDn();
		final var ids = repository.findByMatchingDnForAdmin(securityHelper.getLogin(), dn, delegate.getType());
		if (ids.isEmpty()) {
			throw new ForbiddenException();
		}

	}

	/**
	 * Company repository provider.
	 *
	 * @return Company repository provider.
	 */
	private ICompanyRepository getCompany() {
		return iamProvider[0].getConfiguration().getCompanyRepository();
	}

	/**
	 * User repository provider.
	 *
	 * @return User repository provider.
	 */
	private IUserRepository getUser() {
		return iamProvider[0].getConfiguration().getUserRepository();
	}

	/**
	 * Group repository provider.
	 *
	 * @return Group repository provider.
	 */
	private IGroupRepository getGroup() {
		return iamProvider[0].getConfiguration().getGroupRepository();
	}
}
