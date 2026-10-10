/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ligoj.app.api.CredentialExpiryLevel;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.model.Node;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.ligoj.bootstrap.resource.system.session.ISessionSettingsProvider;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Credential expiry policy of the nodes: the warning threshold, the resulting urgency level, and the session warnings
 * of the administrators.
 */
@Component
public class CredentialExpiryPolicy implements ISessionSettingsProvider {

	/**
	 * Configuration of the number of days before the expiry the administrators are warned.
	 */
	public static final String CONF_WARNING_DAYS = "ligoj.node.credential-expiry.warning-days";

	/**
	 * Default number of days before the expiry the administrators are warned.
	 */
	public static final int DEFAULT_WARNING_DAYS = 14;

	/**
	 * User settings entry holding the session warnings: a list of <code>{code, parameters, link}</code> displayed by
	 * the UI with the <code>warning.&lt;code&gt;</code> message, a click opening the link.
	 */
	static final String WARNINGS = "warnings";

	/**
	 * UI route opened by a click on a credential warning.
	 */
	static final String LINK = "/system/node";

	/**
	 * Application data telling the UI that the API stores the credential expiry: without it, the node dialog neither
	 * shows nor sends the expiry, which an older API would refuse.
	 */
	static final String FEATURE = "credential-expiry";

	@Autowired
	protected ConfigurationResource configuration;

	@Autowired
	protected NodeRepository repository;

	@Autowired
	protected SecurityHelper securityHelper;

	/**
	 * Return the number of days before the expiry the administrators are warned.
	 *
	 * @return The number of days.
	 */
	public int getWarningDays() {
		return configuration.get(CONF_WARNING_DAYS, DEFAULT_WARNING_DAYS);
	}

	/**
	 * Return the urgency of an expiry.
	 *
	 * @param expiry      The credential expiry. May be <code>null</code>.
	 * @param now         The current instant.
	 * @param warningDays The number of days before the expiry the administrators are warned.
	 * @return The level, <code>null</code> when there is nothing to signal.
	 */
	public static CredentialExpiryLevel getLevel(final Instant expiry, final Instant now, final int warningDays) {
		if (expiry == null) {
			return null;
		}
		if (!expiry.isAfter(now)) {
			return CredentialExpiryLevel.EXPIRED;
		}
		return expiry.isAfter(now.plus(warningDays, ChronoUnit.DAYS)) ? null : CredentialExpiryLevel.WARNING;
	}

	/**
	 * Tell the UI that the credential expiry is available, and warn the administrators about the nodes whose
	 * credentials have expired, or expire within the threshold. The regular users are not told: they cannot fix it.
	 */
	@Override
	public void decorate(final SessionSettings settings) {
		settings.getApplicationSettings().getData().putIfAbsent(FEATURE, "true");
		if (!securityHelper.isAdmin()) {
			return;
		}
		final var now = Instant.now();
		final var days = getWarningDays();
		final var nodes = repository.findAllCredentialExpiringBefore(now.plus(days, ChronoUnit.DAYS));
		final var expired = nodes.stream().filter(n -> !n.getCredentialExpiry().isAfter(now)).toList();
		final var expiring = nodes.stream().filter(n -> n.getCredentialExpiry().isAfter(now)).toList();
		if (!expired.isEmpty()) {
			addWarning(settings, "node-credential-expired",
					Map.of("count", String.valueOf(expired.size()), "node", expired.getFirst().getName()));
		}
		if (!expiring.isEmpty()) {
			final Node first = expiring.getFirst();
			addWarning(settings, "node-credential-expiring", Map.of("count", String.valueOf(expiring.size()), "days",
					String.valueOf(days), "node", first.getName(), "date",
					LocalDate.ofInstant(first.getCredentialExpiry(), ZoneOffset.UTC).toString()));
		}
	}

	@SuppressWarnings("unchecked")
	private void addWarning(final SessionSettings settings, final String code, final Map<String, String> parameters) {
		((List<Object>) settings.getUserSettings().computeIfAbsent(WARNINGS, _ -> new ArrayList<>()))
				.add(Map.of("code", code, "parameters", parameters, "link", LINK));
	}
}
