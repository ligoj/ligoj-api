package org.ligoj.app.resource.delegate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.CompanyLdap;
import org.ligoj.app.api.GroupLdap;
import org.ligoj.app.api.Normalizer;
import org.ligoj.app.dao.DelegateOrgRepository;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.model.DelegateOrg;
import org.ligoj.app.model.DelegateType;
import org.ligoj.app.model.ReceiverType;
import org.ligoj.app.validation.DistinguishNameValidator;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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
	private IamProvider iamProvider;

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
	 * Converter from {@link DelegateOrg} to {@link DelegateOrgLightVo}
	 */
	public DelegateOrgLightVo toVo(final DelegateOrg entity) {
		final DelegateOrgLightVo vo = new DelegateOrgLightVo();
		NamedBean.copy(entity, vo);
		vo.copyAuditData(entity, getUser()::toUser);

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

		// Flag to indicate the current user can manage this entry
		vo.setManaged(isManagedDelegate(entity));
		if (entity.getType() == DelegateType.GROUP) {
			final Map<String, GroupLdap> groups = getGroup().findAll();
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
	 * Indicate this delegate is managed : so can be updated by the current user.
	 */
	private boolean isManagedDelegate(final DelegateOrg entity) {
		// Managed if : is administrator, or
		return entity.isCanAdmin() || entity.getReceiverType() != ReceiverType.USER || !securityHelper.getLogin().equals(entity.getReceiver());
	}

	/**
	 * Retrieve all elements with pagination
	 * 
	 * @param uriInfo
	 *            pagination data.
	 * @param typeSearch
	 *            Optional {@link DelegateType} search.
	 * @return all elements with pagination.
	 */
	@GET
	public TableItem<DelegateOrgLightVo> findAll(@Context final UriInfo uriInfo, @QueryParam("type") final DelegateType typeSearch) {
		// Trigger cache loading
		getUser().findAll();

		final PageRequest pageRequest = paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS);
		final Page<DelegateOrg> findAll = repository.findAll(securityHelper.getLogin(), DataTableAttributes.getSearch(uriInfo), typeSearch,
				pageRequest);

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, this::toVo);
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
	 * @param vo
	 *            the object to create.
	 * @return the entity's identifier.
	 */
	@POST
	public int create(final DelegateOrgEditionVo vo) {
		return validateSaveOrUpdate(vo).getId();
	}

	/**
	 * Validate the user changes regarding the current user's right. The associated DN and the real CN will stored in
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
	 * Attention, DN is case sensitive.
	 * 
	 * @return the created/update {@link DelegateOrg}
	 */
	private DelegateOrg validateSaveOrUpdate(final DelegateOrgEditionVo importEntry) {
		final Map<String, CompanyLdap> allCompanies = getCompany().findAll();
		final Map<String, GroupLdap> allGroups = getGroup().findAll();

		// Save the delegate with normalized name
		final DelegateOrg entity = new DelegateOrg();
		entity.setId(importEntry.getId());
		entity.setName(Normalizer.normalize(importEntry.getName()));
		entity.setCanAdmin(importEntry.isCanAdmin());
		entity.setCanWrite(importEntry.isCanWrite());
		entity.setType(importEntry.getType());

		// Check the target
		validateReceiver(importEntry, entity);

		// Get all delegates of current user
		String dn = "n/a";
		if (importEntry.getType() == DelegateType.COMPANY) {
			dn = validateCompany(importEntry, allCompanies, dn);
		} else if (importEntry.getType() == DelegateType.GROUP) {
			dn = validateGroup(importEntry, allGroups, dn);
		} else {
			// Tree, CN <- DN
			dn = validateTree(importEntry);

			// Name is ignored for this type in the internal format
			entity.setName("-");
		}

		// Check there is at least one delegate for this user allowing to write INTO the corresponding DN
		if (repository.findByMatchingDnForAdmin(securityHelper.getLogin(), dn, importEntry.getType()).isEmpty()) {
			throw new ForbiddenException();
		}

		// Check there is at least one delegate for this user allowing to write FROM the corresponding DN
		if (importEntry.getId() != null) {

			// Check the related DN
			validateWriteAccess(importEntry.getId());
		}

		// DN is already normalized
		entity.setDn(dn);
		repository.saveAndFlush(entity);
		return entity;
	}

	/**
	 * Validate the related receiver of this delegate.
	 * 
	 * @param importEntry
	 *            The new delegate.
	 * @param entity
	 *            The receiver entity.
	 */
	private void validateReceiver(final DelegateOrgEditionVo importEntry, final DelegateOrg entity) {
		if (importEntry.getReceiverType() == ReceiverType.USER) {
			// Check the user is visible
			entity.setReceiver(getUser().findByIdExpected(securityHelper.getLogin(), importEntry.getReceiver()).getId());
		} else if (importEntry.getReceiverType() == ReceiverType.COMPANY) {
			// Check the company is visible
			entity.setReceiver(getCompany().findByIdExpected(securityHelper.getLogin(), importEntry.getReceiver()).getId());
		} else {
			// Check the group is visible
			entity.setReceiver(getGroup().findByIdExpected(securityHelper.getLogin(), importEntry.getReceiver()).getId());
		}
		entity.setReceiverType(importEntry.getReceiverType());
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
	private String validateGroup(final DelegateOrgEditionVo importEntry, final Map<String, GroupLdap> allGroups, final String dn) {
		final String normalizedCN = Normalizer.normalize(importEntry.getName());
		final GroupLdap group = allGroups.get(normalizedCN);
		if (group != null) {
			importEntry.setName(normalizedCN);
			return group.getDn();
		}
		return dn;
	}

	/**
	 * Validate, clean the company name, and return the corresponding DN.
	 */
	private String validateCompany(final DelegateOrgEditionVo importEntry, final Map<String, CompanyLdap> allCompanies, final String dn) {
		final String normalizedCN = Normalizer.normalize(importEntry.getName());
		if (allCompanies.containsKey(normalizedCN)) {
			importEntry.setName(normalizedCN);
			return allCompanies.get(normalizedCN).getDn();
		}
		return dn;
	}

	/**
	 * Update entity.
	 * 
	 * @param vo
	 *            the object to update.
	 */
	@PUT
	public void update(final DelegateOrgEditionVo vo) {
		validateSaveOrUpdate(vo);
	}

	/**
	 * Delete entity. Rules, order is important :
	 * <ul>
	 * <li>Related delegate must exist</li>
	 * <li>Related delegate must be managed by the current user with 'admin' right, directly or via a another parent
	 * delegate tree/company/.., or act as if the delegate does not exist.</li>
	 * </ul>
	 * Attention, DN is case sensitive.
	 * 
	 * @param id
	 *            the entity identifier.
	 */
	@DELETE
	@Path("{id:\\d+}")
	public void delete(@PathParam("id") final int id) {

		// Check the related DN
		validateWriteAccess(id);

		// Perform the deletion
		repository.delete(id);
	}

	private void validateWriteAccess(final int id) {

		// Get the related delegate
		final DelegateOrg delegateLdap = repository.findOneExpected(id);

		// Check the related DN
		final String dn = delegateLdap.getDn();
		final List<Integer> ids = repository.findByMatchingDnForAdmin(securityHelper.getLogin(), dn, delegateLdap.getType());
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
		return iamProvider.getConfiguration().getCompanyRepository();
	}

	/**
	 * User repository provider.
	 * 
	 * @return User repository provider.
	 */
	private IUserRepository getUser() {
		return iamProvider.getConfiguration().getUserRepository();
	}

	/**
	 * Group repository provider.
	 * 
	 * @return Group repository provider.
	 */
	private IGroupRepository getGroup() {
		return iamProvider.getConfiguration().getGroupRepository();
	}
}
