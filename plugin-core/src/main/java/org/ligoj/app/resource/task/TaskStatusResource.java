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
import java.util.function.Function;

import org.ligoj.app.dao.task.LongTaskNodeRepository;
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
import org.springframework.data.domain.PageImpl;
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

	/**
	 * DataTables column to ORM/VO property mapping for the task list sort.
	 */
	private static final Map<String, String> ORM_MAPPING = Map.of("id", "id", "author", "author", "start", "start",
			"end", "end", "status", "status");

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
	 * descending by default. Filtering / sorting / pagination are applied in memory (at most one task per locked
	 * entity).
	 *
	 * @param key          The runner bean name.
	 * @param uriInfo      DataTables pagination parameters.
	 * @param statusFilter Optional status filter ({@code running}, {@code succeeded} or {@code failed}).
	 * @return the matching tasks.
	 */
	@GET
	@Path("{key}")
	public TableItem<TaskVo> findTasks(@PathParam("key") final String key, @Context final UriInfo uriInfo,
			@QueryParam("status") final String statusFilter) {
		final var runner = getRunners().get(key);
		if (runner == null) {
			throw new NotFoundException("Unknown task runner: " + key);
		}
		final var status = TaskStatus.parse(statusFilter);
		final var user = securityHelper.getLogin();
		final var filtered = visibleTasks(runner, user).stream().map(this::toTaskVo)
				.filter(v -> status == null || v.getStatus() == status).toList();

		// In-memory sort + pagination using the DataTables page request.
		final var pageRequest = paginationJson.getPageRequest(uriInfo, ORM_MAPPING);
		final var sorted = filtered.stream().sorted(comparator(pageRequest.getSort())).toList();
		final var from = (int) Math.min(pageRequest.getOffset(), sorted.size());
		final var to = Math.min(from + pageRequest.getPageSize(), sorted.size());
		final var page = new PageImpl<>(sorted.subList(from, to), pageRequest, sorted.size());
		return paginationJson.applyPagination(uriInfo, page, Function.identity());
	}

	/**
	 * Resolve the visible tasks of a runner: node and subscription runners are user-scoped via their
	 * {@code findAllVisible}; any other runner falls back to all its tasks (justified by the admin-only access).
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List<? extends AbstractLongTask<?, ?>> visibleTasks(final LongTaskRunner runner, final String user) {
		if (runner instanceof LongTaskRunnerNode) {
			return ((LongTaskNodeRepository) runner.getTaskRepository()).findAllVisible(user);
		}
		if (runner instanceof LongTaskRunnerSubscription) {
			return ((LongTaskSubscriptionRepository) runner.getTaskRepository()).findAllVisible(user);
		}
		return runner.getTaskRepository().findAll();
	}

	/**
	 * Build the runner descriptor and compute its statistics over the visible tasks.
	 */
	@SuppressWarnings("rawtypes")
	protected LongTaskRunnerVo toRunnerVo(final String key, final LongTaskRunner runner, final String user) {
		var running = 0;
		var succeeded = 0;
		var failed = 0;
		final var tasks = visibleTasks(runner, user);
		for (final var task : tasks) {
			switch (status(task)) {
			case RUNNING -> running++;
			case SUCCEEDED -> succeeded++;
			case FAILED -> failed++;
			}
		}
		final var label = runner.newTask().get().getClass().getSimpleName();
		return new LongTaskRunnerVo(key, label, type(runner), new TaskStatsVo(tasks.size(), running, succeeded, failed));
	}

	/**
	 * Map a task entity to its VO.
	 */
	protected TaskVo toTaskVo(final AbstractLongTask<?, ?> task) {
		final var vo = new TaskVo();
		vo.setId(task.getId());
		vo.setAuthor(task.getAuthor());
		vo.setStart(task.getStart());
		vo.setEnd(task.getEnd());
		vo.setStatus(status(task));
		vo.setLocked(lockedRef(task));
		return vo;
	}

	/**
	 * Build the locked entity reference for a task.
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
	 */
	protected TaskStatus status(final AbstractLongTask<?, ?> task) {
		if (task.getEnd() == null) {
			return TaskStatus.RUNNING;
		}
		return task.isFailed() ? TaskStatus.FAILED : TaskStatus.SUCCEEDED;
	}

	/**
	 * Classify a runner by {@code instanceof}.
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

	/**
	 * Build a comparator from the requested sort, defaulting to start date descending.
	 */
	protected Comparator<TaskVo> comparator(final Sort sort) {
		Comparator<TaskVo> result = null;
		for (final var order : sort) {
			final var base = comparatorFor(order.getProperty());
			final var directed = order.isDescending() ? base.reversed() : base;
			result = result == null ? directed : result.thenComparing(directed);
		}
		return result == null
				? Comparator.comparing(TaskVo::getStart, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
				: result;
	}

	/**
	 * Ascending comparator for a single sortable property. Unknown properties fall back to the start date.
	 */
	protected Comparator<TaskVo> comparatorFor(final String property) {
		return switch (property) {
		case "id" -> Comparator.comparing(TaskVo::getId, Comparator.nullsLast(Comparator.naturalOrder()));
		case "author" -> Comparator.comparing(TaskVo::getAuthor, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
		case "end" -> Comparator.comparing(TaskVo::getEnd, Comparator.nullsLast(Comparator.naturalOrder()));
		case "status" -> Comparator.comparing(t -> t.getStatus().name());
		default -> Comparator.comparing(TaskVo::getStart, Comparator.nullsLast(Comparator.naturalOrder()));
		};
	}
}
