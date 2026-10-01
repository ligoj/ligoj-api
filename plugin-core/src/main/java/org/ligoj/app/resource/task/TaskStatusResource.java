/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.dao.task.LongTaskNodeRepository;
import org.ligoj.app.dao.task.LongTaskRepository;
import org.ligoj.app.dao.task.LongTaskSubscriptionRepository;
import org.ligoj.app.model.AbstractLongTask;
import org.ligoj.app.model.AbstractLongTaskNode;
import org.ligoj.app.model.AbstractLongTaskSubscription;
import org.ligoj.app.resource.node.LongTaskRunnerNode;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.app.resource.subscription.LongTaskRunnerSubscription;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Admin-only, read-only diagnostic resource exposing all {@link LongTaskRunner} beans (grouped as node /
 * subscription / other), with per-runner status statistics and a paginated, filterable task list.
 * <p>
 * Admin-only is enforced by the default RBAC rule ({@code ;.*;API;ADMIN}); no authorization entry is required.
 */
@Path("system/task")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class TaskStatusResource {

	private static final String STATUS = "status";
	private static final String AUTHOR = "author";
	/**
	 * DataTables column to ORM/VO property mapping for the task list sort.
	 */
	private static final Map<String, String> ORM_MAPPING = Map.of("id", "id", AUTHOR, AUTHOR, "start", "start",
			"end", "end", STATUS, LongTaskRepository.STATUS_ORDER);

	/**
	 * Non-string ordered columns: no case-insensitive ordering.
	 */
	private static final Set<String> CASE_SENSITIVE_COLUMNS = Set.of("id", "start", "end", STATUS);

	@Autowired
	protected ApplicationContext applicationContext;

	@Autowired
	protected SecurityHelper securityHelper;

	@Autowired
	protected PaginationJson paginationJson;

	/**
	 * Return all {@link LongTaskRunner} beans, keyed by Spring bean name.
	 *
	 * @return the runner beans.
	 */
	@SuppressWarnings("rawtypes")
	protected Map<String, LongTaskRunner> getRunners() {
		return applicationContext.getBeansOfType(LongTaskRunner.class);
	}

	/**
	 * List all runners with their visible-task statistics.
	 *
	 * @return the runners, ordered by key.
	 */
	@GET
	public List<LongTaskRunnerVo> findAll() {
		final var user = securityHelper.getLogin();
		return getRunners().entrySet().stream().map(e -> toRunnerVo(e.getKey(), e.getValue(), user))
				.sorted(Comparator.comparing(LongTaskRunnerVo::getKey)).toList();
	}

	/**
	 * List the visible tasks of a single runner, paginated, optionally filtered by status, sorted by start date
	 * descending by default. Filtering, sorting and pagination are applied by the database, the locked entity
	 * reference is resolved only for the returned page.
	 *
	 * @param key          The runner bean name.
	 * @param uriInfo      DataTables pagination parameters.
	 * @param statusFilter Optional status filter ({@code running}, {@code succeeded} or {@code failed}).
	 * @return the matching tasks.
	 */
	@GET
	@Path("{key}")
	public TableItem<TaskVo> findTasks(@PathParam("key") final String key, @Context final UriInfo uriInfo,
			@QueryParam(STATUS) final String statusFilter) {
		final var runner = getRunners().get(key);
		if (runner == null) {
			throw new NotFoundException("Unknown task runner: " + key);
		}
		final var status = TaskStatus.parse(statusFilter);
		final var user = securityHelper.getLogin();

		// Filtering, sorting and pagination by the database, start date descending by default
		final var request = paginationJson.getPageRequest(uriInfo, ORM_MAPPING, CASE_SENSITIVE_COLUMNS);
		final var pageRequest = request.getSort().isSorted() ? request
				: PageRequest.of(request.getPageNumber(), request.getPageSize(), Sort.by(Sort.Direction.DESC, "start"));
		final var page = visibleTasks(runner, user, status == null || status == TaskStatus.RUNNING,
				status == null || status == TaskStatus.SUCCEEDED, status == null || status == TaskStatus.FAILED,
				pageRequest);

		// The locked entity reference may load lazy associations: only for the returned page
		return paginationJson.applyPagination(uriInfo, page, t -> {
			final var vo = toTaskVoLight(t);
			vo.setLocked(lockedRef(t));
			return vo;
		});
	}

	/**
	 * Return a page of the visible tasks of a runner having one of the enabled statuses: node and subscription runners
	 * are user-scoped; any other runner falls back to all its tasks (justified by the admin-only access).
	 *
	 * @param runner    The task runner.
	 * @param user      The current principal user.
	 * @param running   When <code>true</code>, the running tasks are included.
	 * @param succeeded When <code>true</code>, the succeeded tasks are included.
	 * @param failed    When <code>true</code>, the failed tasks are included.
	 * @param page      The pagination and the sort.
	 * @return The page of visible tasks.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Page<AbstractLongTask<?, ?>> visibleTasks(final LongTaskRunner runner, final String user,
			final boolean running, final boolean succeeded, final boolean failed, final Pageable page) {
		if (runner instanceof LongTaskRunnerNode) {
			return ((LongTaskNodeRepository) runner.getTaskRepository()).findAllVisible(user, running, succeeded, failed,
					page);
		}
		if (runner instanceof LongTaskRunnerSubscription) {
			return ((LongTaskSubscriptionRepository) runner.getTaskRepository()).findAllVisible(user, running, succeeded,
					failed, page);
		}
		return runner.getTaskRepository().findAllByStatus(running, succeeded, failed, page);
	}

	/**
	 * Return the status counters of the visible tasks of a runner, see
	 * {@link #visibleTasks(LongTaskRunner, String, boolean, boolean, boolean, Pageable)}.
	 *
	 * @param runner The task runner.
	 * @param user   The current principal user.
	 * @return A single row: total, running and failed task counts.
	 */
	@SuppressWarnings("rawtypes")
	protected Object[] countVisibleTasks(final LongTaskRunner runner, final String user) {
		final List<Object[]> result;
		if (runner instanceof LongTaskRunnerNode) {
			result = ((LongTaskNodeRepository) runner.getTaskRepository()).countVisibleByStatus(user);
		} else if (runner instanceof LongTaskRunnerSubscription) {
			result = ((LongTaskSubscriptionRepository) runner.getTaskRepository()).countVisibleByStatus(user);
		} else {
			result = runner.getTaskRepository().countByStatus();
		}
		return result.getFirst();
	}

	/**
	 * Build the runner descriptor and compute its statistics over the visible tasks.
	 *
	 * @param key    The runner bean name.
	 * @param runner The task runner.
	 * @param user   The current principal user.
	 * @return The runner descriptor with its statistics.
	 */
	@SuppressWarnings("rawtypes")
	protected LongTaskRunnerVo toRunnerVo(final String key, final LongTaskRunner runner, final String user) {
		final var counts = countVisibleTasks(runner, user);
		final var total = ((Number) counts[0]).intValue();
		final var running = ((Number) counts[1]).intValue();
		final var failed = ((Number) counts[2]).intValue();
		final var label = runner.newTask().get().getClass().getSimpleName();
		return new LongTaskRunnerVo(key, label, type(runner),
				new TaskStatsVo(total, running, total - running - failed, failed));
	}

	/**
	 * Map a task entity to its VO, without the locked entity reference: only the task's own attributes are read.
	 *
	 * @param task The task entity.
	 * @return The task VO, without locked entity reference.
	 */
	protected TaskVo toTaskVoLight(final AbstractLongTask<?, ?> task) {
		final var vo = new TaskVo();
		vo.setId(task.getId());
		vo.setAuthor(task.getAuthor());
		vo.setStart(task.getStart());
		vo.setEnd(task.getEnd());
		vo.setStatus(status(task));
		return vo;
	}

	/**
	 * Build the locked entity reference for a task.
	 *
	 * @param task The task entity.
	 * @return The reference of the node or the subscription locked by this task.
	 */
	protected LockedRefVo lockedRef(final AbstractLongTask<?, ?> task) {
		if (task instanceof AbstractLongTaskNode node) {
			return LockedRefVo.ofNode(node.getLocked().getId());
		}
		if (task instanceof AbstractLongTaskSubscription subscriptionTask) {
			final var subscription = subscriptionTask.getLocked();
			return LockedRefVo.ofSubscription(subscription.getId(), subscription.getNode().getId(),
					subscription.getProject().getId(), subscription.getProject().getName());
		}
		return LockedRefVo.ofOther();
	}

	/**
	 * Derive the status of a task: running ({@code end} null), failed (ended with the failed flag) or succeeded.
	 *
	 * @param task The task entity.
	 * @return The task status.
	 */
	protected TaskStatus status(final AbstractLongTask<?, ?> task) {
		if (task.getEnd() == null) {
			return TaskStatus.RUNNING;
		}
		return task.isFailed() ? TaskStatus.FAILED : TaskStatus.SUCCEEDED;
	}

	/**
	 * Classify a runner by {@code instanceof}.
	 *
	 * @param runner The task runner.
	 * @return The runner type: node, subscription or other.
	 */
	@SuppressWarnings("rawtypes")
	protected TaskStatusType type(final LongTaskRunner runner) {
		if (runner instanceof LongTaskRunnerNode) {
			return TaskStatusType.NODE;
		}
		if (runner instanceof LongTaskRunnerSubscription) {
			return TaskStatusType.SUBSCRIPTION;
		}
		return TaskStatusType.OTHER;
	}
}
