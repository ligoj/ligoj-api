/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base test class for JPA tests.
 */
public abstract class AbstractAppTest extends org.ligoj.bootstrap.AbstractAppTest {

	@Autowired
	protected IamProvider[] iamProviders;

	protected IamProvider iamProvider;
	
	@BeforeEach
	public void copyIamProvider() {
		iamProvider = iamProviders[0];
	}

	/**
	 * User repository provider.
	 *
	 * @return User repository provider.
	 */
	protected IUserRepository getUser() {
		return iamProvider.getConfiguration().getUserRepository();
	}

	/**
	 * Company repository provider.
	 *
	 * @return Company repository provider.
	 */
	protected ICompanyRepository getCompany() {
		return iamProvider.getConfiguration().getCompanyRepository();
	}

	/**
	 * Group repository provider.
	 *
	 * @return Group repository provider.
	 */
	protected IGroupRepository getGroup() {
		return iamProvider.getConfiguration().getGroupRepository();
	}

	/**
	 * Return a new mocked {@link UriInfo} instance with search and an order string.
	 *
	 * @param orderedProperty The property to order.
	 * @param search          The value to search.
	 * @return A new mocked {@link UriInfo} instance with a search criteria.
	 */
	protected UriInfo newUriInfoAscSearch(final String orderedProperty, final String search) {
		final UriInfo uriInfo = newUriInfo(orderedProperty, "asc");
		uriInfo.getQueryParameters().add(DataTableAttributes.SEARCH, search);
		return uriInfo;
	}

	/**
	 * Return a new mocked {@link UriInfo} instance with ascending order on given property.
	 *
	 * @param orderedProperty The property to order.
	 * @return A new mocked {@link UriInfo} instance.
	 */
	protected UriInfo newUriInfoAsc(final String orderedProperty) {
		return newUriInfo(orderedProperty, "asc");
	}

	/**
	 * Return a new mocked {@link UriInfo} instance with descending order on given property.
	 *
	 * @param orderedProperty The property to order.
	 * @return a new mocked {@link UriInfo} instance.
	 */
	protected UriInfo newUriInfoDesc(final String orderedProperty) {
		return newUriInfo(orderedProperty, "desc");
	}

	/**
	 * Return a new mocked {@link UriInfo} instance with descending order on given property.
	 *
	 * @param orderedProperty The property to order.
	 * @param order           The order string: <code>desc</code>, <code>asc</code>.
	 * @return a new mocked {@link UriInfo} instance.
	 */
	protected UriInfo newUriInfo(final String orderedProperty, final String order) {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add("columns[2][data]", orderedProperty);
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, order);
		return uriInfo;
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one subscription for a service.
	 *
	 * @param project The project name of the subscription to return.
	 * @param service The subscribed service of the project. May be a service or a tool or an instance.
	 *                <code>LIKE</code> is used.
	 * @return The subscription identifier.
	 */
	protected int getSubscription(final String project, final String service) {
		return em
				.createQuery("SELECT id FROM Subscription WHERE project.name = ?1 AND node.id LIKE CONCAT(?2,'%')",
						Integer.class)
				.setParameter(1, project).setParameter(2, service).setMaxResults(1).getResultList().get(0);
	}
}
