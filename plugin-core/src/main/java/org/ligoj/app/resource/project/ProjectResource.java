/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.model.Project;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@link Project} resource.
 */
@Path("/project")
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {

	@Autowired
	private ProjectRepository repository;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private PaginationJson paginationJson;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private SubscriptionResource subscriptionResource;

	@Autowired
	private ServicePluginLocator locator;

	@Autowired
	protected IamProvider[] iamProvider;

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORDERED_COLUMNS = new HashMap<>();

	static {
		ORDERED_COLUMNS.put("id", "id");
		ORDERED_COLUMNS.put("description", "description");
		ORDERED_COLUMNS.put("createdDate", "createdDate");
		ORDERED_COLUMNS.put("teamLeader", "teamLeader");

		// This mapping does not work for native spring-data "findAll"
		ORDERED_COLUMNS.put("name", "name");
		ORDERED_COLUMNS.put("nbSubscriptions", "COUNT(s)");
	}

	/**
	 * Converter from {@link Project} to {@link ProjectVo} with the associated subscriptions.
	 *
	 * @param project Entity to convert.
	 * @return The project description with subscriptions.
	 */
	public ProjectVo toVo(final Project project) {
		// Get subscriptions
		final var subscriptionsResultSet = subscriptionRepository.findAllWithValuesSecureByProject(project.getId());

		// Get subscriptions status
		final var subscriptionStatus = subscriptionResource.getStatusByProject(project.getId());

		// Convert users, project and subscriptions
		final var projectVo = new ToVoConverter(locator, toUser(), subscriptionsResultSet, subscriptionStatus)
				.apply(project);
		projectVo.setManageSubscriptions(repository.isManageSubscription(project.getId(), securityHelper.getLogin()));
		return projectVo;
	}

	private Function<String, ? extends UserOrg> toUser() {
		return iamProvider[0].getConfiguration().getUserRepository()::toUser;
	}

	/**
	 * Converter from {@link Project} to {@link ProjectLightVo} with subscription count.
	 *
	 * @param resultSet Entity to convert and the associated subscription count.
	 * @return The project description with subscription counter.
	 */
	public ProjectLightVo toVoLightCount(final Object[] resultSet) { // NOSONAR -- varargs
		final var vo = toVoLight((Project) resultSet[0]);
		vo.setNbSubscriptions(((Long) resultSet[1]).intValue());
		return vo;
	}

	/**
	 * Converter from {@link Project} to {@link ProjectLightVo} without subscription count.
	 *
	 * @param entity Entity to convert.
	 * @return The project description without subscription counter.
	 */
	public ProjectLightVo toVoLight(final Project entity) {

		// Convert users, project and subscriptions
		final var vo = new ProjectLightVo();
		vo.copyAuditData(entity, toUser());
		DescribedBean.copy(entity, vo);
		vo.setPkey(entity.getPkey());
		vo.setTeamLeader(toUser().apply(entity.getTeamLeader()));
		return vo;
	}

	/**
	 * /** Converter from {@link ProjectEditionVo} to {@link Project}
	 */
	private static Project toEntity(final ProjectEditionVo vo) {
		final var entity = new Project();
		// map project
		DescribedBean.copy(vo, entity);
		entity.setPkey(vo.getPkey());
		entity.setTeamLeader(vo.getTeamLeader());
		entity.setCreationContext(vo.getCreationContext());
		return entity;
	}

	/**
	 * Retrieve all project with pagination, and filtered. A visible project is attached to a visible group.
	 *
	 * @param uriInfo  pagination data.
	 * @param criteria the optional criteria to match.
	 * @return all elements with pagination.
	 */
	@GET
	public TableItem<ProjectLightVo> findAll(@Context final UriInfo uriInfo,
			@QueryParam(DataTableAttributes.SEARCH) final String criteria) {
		final var findAll = repository.findAllLight(securityHelper.getLogin(), StringUtils.trimToEmpty(criteria),
				paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS));

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, this::toVoLightCount);
	}

	/**
	 * Return a project with all subscription parameters and their status.
	 *
	 * @param id Project identifier.
	 * @return Found element. Never <code>null</code>.
	 */
	@GET
	@Path("{id:\\d+}")
	public ProjectVo findById(@PathParam("id") final int id) {
		return findOneVisible(repository::findOneVisible, id, this::toVo);
	}

	/**
	 * Return a project with all subscription parameters and their status.
	 *
	 * @param pkey Project pkey (string identifier).
	 * @return Found element. Never <code>null</code>.
	 */
	@GET
	@Path("{pkey:" + Project.PKEY_PATTERN + "}")
	public ProjectVo findByPKeyFull(@PathParam("pkey") final String pkey) {
		return findOneVisible(repository::findByPKey, pkey, this::toVo);
	}

	/**
	 * Return a project without subscription details.
	 *
	 * @param pkey Project pkey.
	 * @return Found element. Never <code>null</code>.
	 */
	public ProjectLightVo findByPKey(final String pkey) {
		return Optional.ofNullable(repository.findByPKeyNoFetch(pkey, securityHelper.getLogin())).map(this::toVoLight)
				.orElseThrow(() -> new EntityNotFoundException(pkey));
	}

	/**
	 * Create project. Should be protected with RBAC.
	 *
	 * @param vo the object to create.
	 * @return the entity's identifier.
	 */
	@POST
	public int create(final ProjectEditionVo vo) {
		return repository.saveAndFlush(ProjectResource.toEntity(vo)).getId();
	}

	/**
	 * Update project. Should be protected with RBAC.
	 *
	 * @param vo the object to save.
	 */
	@PUT
	public void update(final ProjectEditionVo vo) {
		// pkey can't be updated if there is at least subscription.
		final var project = repository.findOneExpected(vo.getId());
		final var nbSubscriptions = subscriptionRepository.countByProject(vo.getId());
		if (nbSubscriptions == 0) {
			project.setPkey(vo.getPkey());
		}

		DescribedBean.copy(vo, project);
		project.setTeamLeader(vo.getTeamLeader());
		repository.saveAndFlush(project);
	}

	/**
	 * Delete entity. Should be protected with RBAC.
	 *
	 * @param id The entity identifier.
	 * @throws Exception When the deletion fails. Managed at JAX-RS level.
	 */
	@DELETE
	@Path("{id:\\d+}")
	public void delete(@PathParam("id") final int id) throws Exception {
		final var project = findOneVisible(repository::findOneVisible, id, Function.identity());
		for (final var subscription : project.getSubscriptions()) {
			subscriptionResource.delete(subscription.getId());
		}
		project.getSubscriptions().clear();
		repository.delete(project);
	}

	private <T, K> T findOneVisible(final BiFunction<K, String, Project> finder, final K key,
			final Function<Project, T> mapper) {
		return Optional.ofNullable(finder.apply(key, securityHelper.getLogin())).map(mapper)
				.orElseThrow(() -> new EntityNotFoundException(key.toString()));
	}
}
