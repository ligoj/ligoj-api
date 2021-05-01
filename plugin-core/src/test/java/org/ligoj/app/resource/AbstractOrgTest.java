/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.CacheCompanyRepository;
import org.ligoj.app.iam.dao.CacheGroupRepository;
import org.ligoj.app.iam.empty.EmptyCompanyRepository;
import org.ligoj.app.iam.empty.EmptyGroupRepository;
import org.ligoj.app.iam.empty.EmptyIamProvider;
import org.ligoj.app.iam.empty.EmptyUserRepository;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheContainer;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.model.CacheProjectGroup;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test inserting all organizational data in database.
 */
public abstract class AbstractOrgTest extends AbstractAppTest {

	@Autowired
	private CacheCompanyRepository cacheCompanyRepository;

	@Autowired
	private CacheGroupRepository cacheGroupRepository;

	@BeforeEach
	protected void setUpEntities() throws IOException {
		persistSystemEntities();

		// Prepare the standard data
		persistEntities("csv", new Class[] { DelegateOrg.class }, StandardCharsets.UTF_8.name());
		persistEntities("csv",
				new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
				StandardCharsets.UTF_8.name());

		// Add the IAM data
		csvForJpa.cleanup(CacheCompany.class, CacheUser.class, CacheGroup.class, CacheMembership.class,
				CacheProjectGroup.class);
		final var companies = csvForJpa.insert("csv", CacheCompany.class, StandardCharsets.UTF_8.name()).stream()
				.map(c -> new CompanyOrg(c.getDescription(), c.getName()))
				.collect(Collectors.toMap(CompanyOrg::getId, Function.identity()));
		final var users = csvForJpa.insert("csv", CacheUser.class, StandardCharsets.UTF_8.name()).stream().map(c -> {
			final var user = new UserOrg();
			user.setId(c.getId());
			user.setDn("uid=" + c.getId() + "," + companies.get(c.getCompany().getId()).getDn());
			user.setCompany(c.getCompany().getId());
			user.setFirstName(c.getFirstName());
			user.setLastName(c.getLastName());
			user.setMails(Arrays.asList(Optional.ofNullable(c.getMails()).orElse("").split(",")));
			return user;
		}).collect(Collectors.toMap(UserOrg::getId, Function.identity()));
		final var groups = csvForJpa.insert("csv", CacheGroup.class, StandardCharsets.UTF_8.name()).stream()
				.map(c -> new GroupOrg(c.getDescription(), c.getName(), new HashSet<>()))
				.collect(Collectors.toMap(GroupOrg::getId, Function.identity()));
		var cacheMembership = csvForJpa.insert("csv", CacheMembership.class, StandardCharsets.UTF_8.name()).get(0);
		csvForJpa.insert("csv", CacheProjectGroup.class, StandardCharsets.UTF_8.name());

		// Coverage required here only there because of JPA bean
		Assertions.assertNotNull(cacheMembership.getGroup());
		Assertions.assertNotNull(cacheMembership.getUser());
		Assertions.assertNull(cacheMembership.getSubGroup());
		cacheMembership.setSubGroup(null);

		// Plug-in the IAMProvider to the database
		final var configuration = new IamConfiguration();
		final EmptyUserRepository userRepository = new EmptyUserRepository() {
			@Override
			public Map<String, UserOrg> findAll() {
				return users;
			}

			@Override
			public UserOrg findById(final String login) {
				return findAll().get(login);
			}

			@Override
			public UserOrg findOneBy(final String attribute, final String value) {
				return findAllBy(attribute, value).stream().findFirst().orElse(null);
			}
		};
		configuration.setUserRepository(userRepository);
		configuration.setCompanyRepository(new EmptyCompanyRepository() {
			@Override
			public Map<String, CompanyOrg> findAll() {
				return companies;
			}

			@Override
			public CompanyOrg findById(final String user, final String id) {
				// Check the container exists and return the in memory object.
				return Optional.ofNullable(cacheCompanyRepository.findById(user, Normalizer.normalize(id)))
						.map(CacheContainer::getId).map(this::findById).orElse(null);
			}
		});
		configuration.setGroupRepository(new EmptyGroupRepository() {
			@Override
			public Map<String, GroupOrg> findAll() {
				return groups;
			}

			@Override
			public GroupOrg findById(final String user, final String id) {
				// Check the container exists and return the in memory object.
				return Optional.ofNullable(cacheGroupRepository.findById(user, Normalizer.normalize(id)))
						.map(CacheContainer::getId).map(this::findById).orElse(null);
			}
		});
		userRepository.setCompanyRepository(configuration.getCompanyRepository());

		iamProvider = new EmptyIamProvider() {
			@Override
			public IamConfiguration getConfiguration() {
				return configuration;
			}
		};

		em.flush();
		em.clear();
	}

}
