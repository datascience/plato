package eu.scape_project.pw.planning.manager;

import java.io.File;

import javax.inject.Inject;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.scape_project.planning.exception.PlanningException;
import eu.scape_project.planning.manager.ByteStreamManager;
import eu.scape_project.planning.manager.FileStorage;
import eu.scape_project.planning.manager.IByteStreamManager;
import eu.scape_project.planning.manager.IByteStreamStorage;
import eu.scape_project.planning.manager.StorageException;
import eu.scape_project.planning.utils.FileUtils;
import eu.scape_project.planning.utils.LoggerFactory;
import eu.scape_project.planning.utils.OS;

@RunWith(Arquillian.class)
public class ByteStreamManagerTest {

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive wa = ShrinkWrap
				.create(WebArchive.class)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				.addClasses(PlanningException.class, StorageException.class,
						FileUtils.class, OS.class, IByteStreamManager.class,
						IByteStreamStorage.class, FileStorage.class,
						ByteStreamManager.class, LoggerFactory.class);
		System.out.println(wa.toString(true));
		return wa;
	}

	@Inject
	ByteStreamManager bm;

	@Test
	public void testStoreLoad() {
		byte[] array = { 1, 2, 3, 4 };
		try {
			String pid = bm.store(null, array);
			byte[] loaded = bm.load(pid);
			Assert.assertTrue(loaded.length == array.length);
			for (int i = 0; i < loaded.length; i++) {
				Assert.assertTrue(loaded[i] == array[i]);
			}
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testUpdate() {
		byte[] array = { 1, 2, 3, 4 };
		byte[] update = { 1, 2, 3, 4, 5, 6 };
		try {
			bm.store("plato:test", array);
			bm.store("plato:test", update);
			byte[] loaded = bm.load("plato:test");
			Assert.assertFalse(loaded.length == array.length);
			Assert.assertTrue(loaded.length == update.length);
			for (int i = 0; i < loaded.length; i++) {
				Assert.assertTrue(loaded[i] == update[i]);
			}
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testDelete() {
		byte[] array = { 1, 2, 3, 4 };
		String pid = null;
		try {
			pid = bm.store(null, array);
			byte[] loaded = bm.load(pid);
			Assert.assertTrue(loaded.length == array.length);
			bm.delete(pid);
			for (int i = 0; i < loaded.length; i++) {
				Assert.assertTrue(loaded[i] == array[i]);
			}
		} catch (StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			byte[] loaded = bm.load(pid);
		} catch (StorageException e) {
			Assert.assertTrue(true);
		}
	}
}
