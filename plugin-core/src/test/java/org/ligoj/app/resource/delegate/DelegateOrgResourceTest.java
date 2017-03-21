package org.ligoj.app.resource.delegate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.transaction.Transactional;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.AbstractJpaTest;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.SimpleUser;
import org.ligoj.app.dao.DelegateOrgRepository;
import org.ligoj.app.model.DelegateOrg;
import org.ligoj.app.model.DelegateType;
import org.ligoj.app.model.ReceiverType;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link DelegateOrgResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class DelegateOrgResourceTest extends AbstractJpaTest {

	@Autowired
	private DelegateOrgResource resource;

	@Autowired
	private DelegateOrgRepository repository;

	private DelegateOrg expected;

	@Before
	public void setUpEntities() throws IOException {
		persistEntities("csv/app-test", new Class[] { DelegateOrg.class }, StandardCharsets.UTF_8.name());
		em.flush();
		em.clear();
		expected = repository.findByName("dig rha");
		em.clear();
	}

	@Test
	public void findAllFewVisible() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		initSpringSecurityContext("someone");

		Mockito.when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("someone");
		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, null);
		Assert.assertEquals(4, result.getData().size());
		Assert.assertEquals(4, result.getRecordsTotal());

		// someone;group;dig rha;false;false;cn=dig rha,cn=dig as,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com
		DelegateOrgLightVo entity = result.getData().get(2);
		Assert.assertEquals("DIG RHA", entity.getName());
		Assert.assertEquals(DelegateType.GROUP, entity.getType());
		Assert.assertNotNull(entity.getCreatedDate());
		Assert.assertNotNull(entity.getLastModifiedDate());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getLastModifiedBy().getId());
		Assert.assertEquals("someone", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertFalse(entity.isCanAdmin());
		Assert.assertFalse(entity.isCanWrite());
		Assert.assertFalse(entity.isManaged());

		// someone;company;any;false;true;cn=any,ou=groups,dc=sample,dc=com
		entity = result.getData().get(0);
		Assert.assertEquals("any", entity.getName());
		Assert.assertEquals(DelegateType.COMPANY, entity.getType());
		Assert.assertNotNull(entity.getCreatedDate());
		Assert.assertNotNull(entity.getLastModifiedDate());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getLastModifiedBy().getId());
		Assert.assertEquals("fdaugan", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertEquals("Fabrice", ((SimpleUser) entity.getReceiver()).getFirstName());
		Assert.assertTrue(entity.isCanAdmin());
		Assert.assertTrue(entity.isCanWrite());
		Assert.assertTrue(entity.isManaged());

		// someone;company;ing;true;false;ou=ing,ou=external,ou=people,dc=sample,dc=com
	}

	@Test
	public void findAllSelf() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		initSpringSecurityContext("mlavoine");
		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, null);
		Assert.assertEquals(3, result.getData().size());
		Assert.assertEquals(3, result.getRecordsTotal());

		// mlavoine;tree;cn=Biz Agency,ou=tools;false;false;cn=Biz Agency,ou=tools,dc=sample,dc=com
		final DelegateOrgLightVo entity = result.getData().get(0);
		Assert.assertEquals("cn=biz agency,ou=tools,dc=sample,dc=com", entity.getName());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertNotNull(entity.getCreatedDate());
		Assert.assertNotNull(entity.getLastModifiedDate());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getLastModifiedBy().getId());
		Assert.assertEquals("mlavoine", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertFalse(entity.isCanAdmin());
		Assert.assertFalse(entity.isCanWrite());
		Assert.assertFalse(entity.isManaged());

		// mlavoine;company;ing;false;false;ou=ing,ou=external,ou=people,dc=sample,dc=com
		Assert.assertEquals(DelegateType.COMPANY, result.getData().get(2).getType());
		Assert.assertEquals("ing", result.getData().get(2).getName());

		// mlavoine;tree;cn=biz agency,ou=tools,dc=sample,dc=com
		Assert.assertEquals(DelegateType.TREE, result.getData().get(0).getType());
		Assert.assertEquals("cn=biz agency,ou=tools,dc=sample,dc=com", result.getData().get(0).getName());
		

	}

	@Test
	public void findAll() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		uriInfo.getQueryParameters().putSingle("length", "11");

		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, null);
		Assert.assertEquals(11, result.getData().size());
		Assert.assertEquals(23, result.getRecordsTotal());

		checkDelegateGroup2(result.getData().get(10));
		checkDelegateTree(result.getData().get(3));
	}

	@Test
	public void findAllReceiverGroup() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		initSpringSecurityContext("alongchu");
		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, null);
		Assert.assertEquals(1, result.getData().size());
		Assert.assertEquals(1, result.getRecordsTotal());

		final DelegateOrgLightVo entity = result.getData().get(0);
		Assert.assertEquals("ing", entity.getName());
		Assert.assertEquals(DelegateType.COMPANY, entity.getType());
		Assert.assertEquals("gfi-gstack", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.GROUP, entity.getReceiverType());
	}

	@Test
	public void findAllReceiverCompany() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		initSpringSecurityContext("jdoe5");
		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, null);
		Assert.assertEquals(1, result.getData().size());
		Assert.assertEquals(1, result.getRecordsTotal());

		final DelegateOrgLightVo entity = result.getData().get(0);
		Assert.assertEquals("Business Solution", entity.getName());
		Assert.assertEquals(DelegateType.GROUP, entity.getType());
		Assert.assertEquals("ing", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.COMPANY, entity.getReceiverType());
	}

	@Test
	public void findAllGlobalSearch() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		uriInfo.getQueryParameters().add(DataTableAttributes.SEARCH, "dig");

		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, null);
		Assert.assertEquals(4, result.getData().size());

		checkDelegateGroup(result.getData().get(1));
	}

	@Test
	public void findAllGlobalSearchGroup() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		uriInfo.getQueryParameters().add(DataTableAttributes.SEARCH, "dig");

		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, DelegateType.GROUP);
		Assert.assertEquals(4, result.getData().size());

		checkDelegateGroup(result.getData().get(1));
	}

	@Test
	public void findAllGlobalSearchCompany() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();
		uriInfo.getQueryParameters().add(DataTableAttributes.SEARCH, "dig");

		final TableItem<DelegateOrgLightVo> result = resource.findAll(uriInfo, DelegateType.COMPANY);
		Assert.assertEquals(0, result.getData().size());
	}

	private void checkDelegateGroup(final DelegateOrgLightVo entity) {
		Assert.assertEquals("DIG RHA", entity.getName());
		Assert.assertEquals(DelegateType.GROUP, entity.getType());
		Assert.assertNotNull(entity.getCreatedDate());
		Assert.assertNotNull(entity.getLastModifiedDate());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getLastModifiedBy().getId());
		Assert.assertEquals("fdaugan", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertEquals("Fabrice", ((SimpleUser) entity.getReceiver()).getFirstName());
		Assert.assertTrue(entity.isCanAdmin());
		Assert.assertTrue(entity.isCanWrite());
		Assert.assertTrue(entity.isManaged());
	}

	private void checkDelegateGroup2(final DelegateOrgLightVo entity) {
		Assert.assertEquals("DIG AS", entity.getName());
		Assert.assertEquals(DelegateType.GROUP, entity.getType());
		Assert.assertNotNull(entity.getCreatedDate());
		Assert.assertNotNull(entity.getLastModifiedDate());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getLastModifiedBy().getId());
		Assert.assertEquals("mmartin", entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertEquals("Marc", ((SimpleUser) entity.getReceiver()).getFirstName());
		Assert.assertFalse(entity.isCanAdmin());
		Assert.assertFalse(entity.isCanWrite());
		Assert.assertTrue(entity.isManaged());
	}

	private void checkDelegateTree(final DelegateOrgLightVo entity) {
		Assert.assertEquals("dc=sample,dc=com", entity.getName());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertNotNull(entity.getCreatedDate());
		Assert.assertNotNull(entity.getLastModifiedDate());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getLastModifiedBy().getId());
		Assert.assertEquals(DEFAULT_USER, entity.getReceiver().getId());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertTrue(entity.isCanAdmin());
		Assert.assertTrue(entity.isCanWrite());
		Assert.assertTrue(entity.isManaged());
	}

	private UriInfo newFindAllParameters() {
		final UriInfo uriInfo = Mockito.mock(UriInfo.class);
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(new MetadataMap<>());
		uriInfo.getQueryParameters().add("draw", "1");
		uriInfo.getQueryParameters().add("start", "0");
		uriInfo.getQueryParameters().add("length", "10");
		uriInfo.getQueryParameters().add("columns[0][data]", "name");
		uriInfo.getQueryParameters().add("order[0][column]", "0");
		uriInfo.getQueryParameters().add("order[0][dir]", "asc");
		return uriInfo;
	}

	@Test
	public void createOnGroup() {
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("hUb Nord");
		vo.setType(DelegateType.GROUP);
		vo.setReceiver("fdaugan");
		final int id = resource.create(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOneExpected(id);

		// Check the stored name is normalized
		Assert.assertEquals("hub nord", entity.getName());
		Assert.assertEquals("cn=hub nord,cn=hub france,cn=production,ou=branche,ou=groups,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.GROUP, entity.getType());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy());
		Assert.assertEquals("fdaugan", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertFalse(entity.isCanAdmin());
		Assert.assertFalse(entity.isCanWrite());
	}

	@Test
	public void createDelegateCompany() {
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("socygan");
		vo.setType(DelegateType.COMPANY);
		vo.setReceiver("fdaugan");
		vo.setCanAdmin(true);
		vo.setCanWrite(true);
		final int id = resource.create(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOneExpected(id);
		Assert.assertEquals("socygan", entity.getName());
		Assert.assertEquals("ou=socygan,ou=external,ou=people,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.COMPANY, entity.getType());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy());
		Assert.assertEquals("fdaugan", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertTrue(entity.isCanWrite());
		Assert.assertTrue(entity.isCanAdmin());
	}

	@Test
	public void createDelegateCompanyReceiverCompany() {
		initSpringSecurityContext("mtuyer");
	
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("InG");
		vo.setType(DelegateType.COMPANY);
		vo.setReceiver("ing");
		vo.setReceiverType(ReceiverType.COMPANY);
		vo.setCanAdmin(true);
		final int id = resource.create(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOneExpected(id);
		Assert.assertEquals("ing", entity.getName());
		Assert.assertEquals("ou=ing,ou=external,ou=people,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.COMPANY, entity.getType());
		Assert.assertEquals("mtuyer", entity.getCreatedBy());
		Assert.assertEquals("ing", entity.getReceiver());
		Assert.assertEquals(ReceiverType.COMPANY, entity.getReceiverType());
		Assert.assertTrue(entity.isCanAdmin());
	}

	@Test
	public void createDelegateCompanyReceiverGroup() {
		initSpringSecurityContext("mtuyer");
	
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("ing");
		vo.setType(DelegateType.COMPANY);
		vo.setReceiver("dig");
		vo.setReceiverType(ReceiverType.GROUP);
		vo.setCanAdmin(true);
		final int id = resource.create(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOneExpected(id);
		Assert.assertEquals("ing", entity.getName());
		Assert.assertEquals("ou=ing,ou=external,ou=people,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.COMPANY, entity.getType());
		Assert.assertEquals("mtuyer", entity.getCreatedBy());
		Assert.assertEquals("dig", entity.getReceiver());
		Assert.assertEquals(ReceiverType.GROUP, entity.getReceiverType());
		Assert.assertTrue(entity.isCanAdmin());
	}

	@Test(expected = ForbiddenException.class)
	public void createOnTreePartialDn() {
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("cn=myDn");
		vo.setType(DelegateType.TREE);
		vo.setReceiver("fdaugan");
		resource.create(vo);
	}

	@Test
	public void createInvalidDn() {
		MatcherUtil.expectValidationException(thrown, "tree", "DistinguishName");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("cn=my,invalidDn,dc=sample,dc=com");
		vo.setName("myDn*Partial");
		vo.setType(DelegateType.TREE);
		vo.setReceiver("fdaugan");
		resource.create(vo);
	}

	@Test(expected = ForbiddenException.class)
	public void createOnUnkownCompany() {
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("any");
		vo.setType(DelegateType.COMPANY);
		vo.setReceiver("fdaugan");
		resource.create(vo);
	}

	@Test
	public void createOnSubTree() {
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("cn=Any,dc=sample,dc=com");
		vo.setType(DelegateType.TREE);
		vo.setReceiver("fdaugan");
		final int id = resource.create(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOneExpected(id);
		Assert.assertEquals("-", entity.getName());
		Assert.assertEquals("cn=any,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertEquals(DEFAULT_USER, entity.getCreatedBy());
		Assert.assertEquals("fdaugan", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertFalse(entity.isCanWrite());
		Assert.assertFalse(entity.isCanWrite());
	}

	@Test
	public void createOnSubTreeInvalidDn() {
		MatcherUtil.expectValidationException(thrown, "tree", "DistinguishName");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setName("myDn,dc=sample,dc=com");
		vo.setType(DelegateType.TREE);
		vo.setReceiver("fdaugan");
		resource.create(vo);
	}

	@Test(expected = ForbiddenException.class)
	public void updateForbiddenNotAdminDn() {
		initSpringSecurityContext("mlavoine");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(expected.getId());
		vo.setName("Biz Agency");
		vo.setReceiver("mlavoine");
		vo.setType(DelegateType.GROUP);
		resource.update(vo);
	}

	@Test
	public void updateInvisibleDelegateUser() {
		MatcherUtil.expectValidationException(thrown, "id", "unknown-id");
		initSpringSecurityContext("mlavoine");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(expected.getId());
		vo.setName("Biz Agency");
		vo.setReceiver("any");
		vo.setType(DelegateType.GROUP);
		resource.update(vo);
	}

	@Test(expected = ForbiddenException.class)
	public void updateInvisibleDelegateCompany() {
		initSpringSecurityContext("mtuyer");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND dn=:dn", Integer.class).setParameter("user", "mtuyer")
				.setParameter("dn", "ou=fonction,ou=groups,dc=sample,dc=com").getSingleResult();
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(id);
		vo.setName("socygan");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.COMPANY);
		resource.update(vo);
	}

	@Test
	public void updateInvisibleReceiverUser() {
		MatcherUtil.expectValidationException(thrown, "id", "unknown-id");
		initSpringSecurityContext("mtuyer");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(expected.getId());
		vo.setName("ing");
		vo.setReceiver("fdaugan");
		vo.setType(DelegateType.COMPANY);
		resource.update(vo);
	}

	@Test
	public void updateInvisibleReceiverCompany() {
		MatcherUtil.expectValidationException(thrown, "company", "unknown-id");
		initSpringSecurityContext("mtuyer");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(expected.getId());
		vo.setName("ing");
		vo.setReceiver("socygan");
		vo.setReceiverType(ReceiverType.COMPANY);
		vo.setType(DelegateType.COMPANY);
		resource.update(vo);
	}

	@Test
	public void updateInvisibleReceiverGroup() {
		MatcherUtil.expectValidationException(thrown, "group", "unknown-id");
		initSpringSecurityContext("mtuyer");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(expected.getId());
		vo.setName("ing");
		vo.setReceiver("biz agency");
		vo.setReceiverType(ReceiverType.GROUP);
		vo.setType(DelegateType.COMPANY);
		resource.update(vo);
	}

	@Test(expected = ForbiddenException.class)
	public void updateForbiddenInvalidDelegateType() {
		initSpringSecurityContext("mtuyer");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(expected.getId());
		vo.setName("ing");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.GROUP);
		resource.update(vo);
	}

	@Test(expected = ForbiddenException.class)
	public void updateForbiddenInvalidDelegateTree() {
		initSpringSecurityContext("mtuyer");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND dn=:dn", Integer.class).setParameter("user", "mtuyer")
				.setParameter("dn", "ou=fonction,ou=groups,dc=sample,dc=com").getSingleResult();
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(id);
		vo.setName("ou=z,ou=groups,dc=sample,dc=com");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.TREE);
		resource.update(vo);
	}

	@Test
	public void updateType() {
		initSpringSecurityContext("mtuyer");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND type=:type", Integer.class)
				.setParameter("type", DelegateType.COMPANY).setParameter("user", "mtuyer").getSingleResult();
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(id);
		vo.setName("cn=any,ou=fonction,ou=groups,dc=sample,dc=com");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.TREE);
		vo.setCanAdmin(true);
		vo.setCanWrite(false);
		resource.update(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOne(id);
		Assert.assertEquals("-", entity.getName());
		Assert.assertEquals("cn=any,ou=fonction,ou=groups,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertEquals("mtuyer", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertTrue(entity.isCanAdmin());
		Assert.assertFalse(entity.isCanWrite());
	}

	/**
	 * Try to update a delegate does not exist
	 */
	@Test(expected = ObjectRetrievalFailureException.class)
	public void updateNotExist() {
		initSpringSecurityContext("mtuyer");
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(-5);
		vo.setName("ing");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.COMPANY);
		resource.update(vo);
	}

	@Test
	public void updateToSubTree() {
		initSpringSecurityContext("mtuyer");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND dn=:dn", Integer.class).setParameter("user", "mtuyer")
				.setParameter("dn", "ou=fonction,ou=groups,dc=sample,dc=com").getSingleResult();
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(id);
		vo.setName("cn=any,ou=fonction,ou=groups,dc=sample,dc=com");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.TREE);
		vo.setCanAdmin(true);
		vo.setCanWrite(false);
		resource.update(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOne(id);
		Assert.assertEquals("-", entity.getName());
		Assert.assertEquals("cn=any,ou=fonction,ou=groups,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertEquals("mtuyer", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertTrue(entity.isCanAdmin());
		Assert.assertFalse(entity.isCanWrite());
	}

	@Test
	public void updateNoChange() {
		initSpringSecurityContext("mtuyer");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND dn=:dn", Integer.class).setParameter("user", "mtuyer")
				.setParameter("dn", "ou=fonction,ou=groups,dc=sample,dc=com").getSingleResult();
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(id);

		// Add space that would be trimmed
		vo.setName("ou=fonction,ou=groups,dc=sample,dc=com  ");
		vo.setReceiver("mtuyer");
		vo.setType(DelegateType.TREE);
		vo.setCanWrite(true);
		vo.setCanAdmin(true);
		resource.update(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOne(id);
		Assert.assertEquals("-", entity.getName());
		Assert.assertEquals("ou=fonction,ou=groups,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertEquals("mtuyer", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertTrue(entity.isCanWrite());
		Assert.assertTrue(entity.isCanAdmin());
	}

	@Test
	public void updateNoChangeFromAnother() {
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND dn=:dn", Integer.class).setParameter("user", "mtuyer")
				.setParameter("dn", "ou=fonction,ou=groups,dc=sample,dc=com").getSingleResult();
		final DelegateOrgEditionVo vo = new DelegateOrgEditionVo();
		vo.setId(id);
		vo.setName("ou=fonction,ou=groups,dc=sample,dc=com");
		vo.setReceiver("fdaugan");
		vo.setType(DelegateType.TREE);
		vo.setCanWrite(true);
		vo.setCanAdmin(true);
		resource.update(vo);
		em.flush();
		em.clear();

		final DelegateOrg entity = repository.findOne(id);
		Assert.assertEquals("-", entity.getName());
		Assert.assertEquals("ou=fonction,ou=groups,dc=sample,dc=com", entity.getDn());
		Assert.assertEquals(DelegateType.TREE, entity.getType());
		Assert.assertEquals("fdaugan", entity.getReceiver());
		Assert.assertEquals(ReceiverType.USER, entity.getReceiverType());
		Assert.assertTrue(entity.isCanWrite());
		Assert.assertTrue(entity.isCanAdmin());
	}

	@Test
	public void deleteFromBaseDn() {
		final long initCount = repository.count();
		em.clear();
		resource.delete(expected.getId());
		em.flush();
		em.clear();
		Assert.assertEquals(initCount - 1, repository.count());
	}

	@Test
	public void deleteSubTreeGroup() {
		initSpringSecurityContext("fdaugan");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND name=:name", Integer.class)
				.setParameter("user", "someone").setParameter("name", "dig rha").getSingleResult();
		final long initCount = repository.count();
		em.clear();
		resource.delete(id);
		em.flush();
		em.clear();
		Assert.assertEquals(initCount - 1, repository.count());
	}

	@Test
	public void delete() {
		final long initCount = repository.count();
		em.clear();
		resource.delete(expected.getId());
		em.flush();
		em.clear();
		Assert.assertEquals(initCount - 1, repository.count());
	}

	@Test(expected = ForbiddenException.class)
	public void deleteNotAdmin() {
		initSpringSecurityContext("someone");
		final int id = em.createQuery("SELECT id FROM DelegateOrg WHERE receiver=:user AND name=:name", Integer.class)
				.setParameter("user", "someone").setParameter("name", "dig rha").getSingleResult();
		resource.delete(id);
	}

	@Test(expected = ObjectRetrievalFailureException.class)
	public void deleteUnknown() {
		resource.delete(-5);
	}
}
