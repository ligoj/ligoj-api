/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.task;

/**
 * Per-runner task counters, by derived status. Designed to feed a progress bar.
 * {@code running + succeeded + failed == total}.
 *
 * @param total     Total amount of visible tasks.
 * @param running   Amount of running tasks ({@code end} is {@code null}).
 * @param succeeded Amount of finished tasks without the {@code failed} flag.
 * @param failed    Amount of finished tasks with the {@code failed} flag.
 */
public record TaskStatsVo(int total, int running, int succeeded, int failed) {
}
