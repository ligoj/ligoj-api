/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.PathParam;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.model.Project;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.EventResource;
import org.ligoj.app.resource.node.EventVo;
import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link Project} resource.
 */
@Service
public class ProjectHelper {

	@Autowired
	private ProjectRepository repository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private ServicePluginLocator locator;

	@Autowired
	protected IamProvider[] iamProvider;

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
		final var subscriptionStatus = getStatusByProject(project.getId());

		// Convert users, project and subscriptions
		final var projectVo = new ToVoConverter(locator, toUser(), subscriptionsResultSet, subscriptionStatus)
				.apply(project);
		projectVo.setManageSubscriptions(repository.isManageSubscription(project.getId(), securityHelper.getLogin()));
		return projectVo;
	}

	/**
	 * Retrieve the last known status of subscriptions of given project .
	 *
	 * @param project project identifier
	 * @return Status of each subscription of given project.
	 */
	public Map<Integer, EventVo> getStatusByProject(@PathParam("project") final int project) {
		return eventRepository.findLastEvents(project).stream().map(EventResource::toVo).collect(Collectors.toMap(EventVo::getSubscription, Function.identity()));
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
	 * Converter from {@link ProjectEditionVo} to {@link Project}
	 */
	static Project toEntity(final ProjectEditionVo vo) {
		final var entity = new Project();
		// map project
		DescribedBean.copy(vo, entity);
		entity.setPkey(vo.getPkey());
		entity.setTeamLeader(vo.getTeamLeader());
		entity.setCreationContext(vo.getCreationContext());
		entity.setMetadata(vo.getMetadata());
		return entity;
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
	 * Check the associated project is visible for current user.
	 *
	 * @param id Project's identifier.
	 * @return the loaded project.
	 */
	public Project checkVisibleProject(final int id) {
		final var project = repository.findOneVisible(id, securityHelper.getLogin());
		if (project == null) {
			// Associated project is not visible
			throw new EntityNotFoundException(String.valueOf(id));
		}
		return project;
	}

}
