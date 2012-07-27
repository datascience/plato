package eu.scape_project.planning.plato.fte;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.scape_project.planning.model.Plan;
import eu.scape_project.planning.plato.wfview.fte.FTCreatePlanView;

@RunWith(Arquillian.class)
public class FTEIntegrationTest {


	
//	@Deployment
//	public static WebArchive createDeployment() {
//		MavenDependencyResolver resolver = DependencyResolvers.use(
//			    MavenDependencyResolver.class);
//		
//		JavaArchive platoModel = ShrinkWrap.create(JavaArchive.class, "plato-model.jar");
//		platoModel.addPackages(true, "eu.scape_project.planning.model")
//					.addPackage("eu.scape_project.planning.exception")
//					.addAsManifestResource(EmptyAsset.INSTANCE, "MANIFEST.MF");
//		
//		//System.out.println(platoModel.toString(true));
//		WebArchive wa = ShrinkWrap
//				.create(WebArchive.class)
//				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addClass(FTCreatePlanView.class);
//		//System.out.println(wa.toString(true));
//		
//		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear");
//		ear.addAsModule(wa);
//		ear.addAsLibraries(platoModel);
//		ear.addAsLibraries(
//                    DependencyResolvers.
//                    use(MavenDependencyResolver.class).
//                    	loadEffectivePom("pom.xml").resolveAsFiles());
//                        
//		System.out.println(ear.toString(true));
//		return wa;
//	}
	
	@Deployment
	public static EnterpriseArchive createDeployment1() {
	     
		File planningCore = new File("../planningsuite-ear/target/planningsuite-ear/planning-core-0.0.1-SNAPSHOT.jar");
		File plato = new File("../planningsuite-ear/target/planningsuite-ear/plato-0.0.1-SNAPSHOT.war");
		
		File lib = new File("../planningsuite-ear/target/planningsuite-ear/lib");
		File metaInf = new File("../planningsuite-ear/target/planningsuite-ear/META-INF"); 
		File archiveFile = new File("../planningsuite-ear/target/planningsuite-ear.ear");	
		EnterpriseArchive archive = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, archiveFile);
		WebArchive platoArch = ShrinkWrap.createFromZipFile(WebArchive.class, plato);
//		EnterpriseArchive planningSuite = ShrinkWrap.create(EnterpriseArchive.class,"plannningsuite.ear").
//				addAsLibraries(lib).addAsModule(planningCore).addAsModule(plato).
//				addAsManifestResource("test-persistance.xml", "persistance.xml");
//		
		platoArch.delete(platoArch.get("WEB-INF/web.xml").getPath());
		platoArch.addPackage("eu.scape_project.planning.plato.fte");
		System.out.println(platoArch.toString(true));
		Node node = archive.get("plato-0.0.1-SNAPSHOT.war");
		archive.delete(node.getPath());
		archive.addAsModule(Testable.archiveToTest(platoArch));
		
//		System.out.println(planningSuite.toString(true));
//		return planningSuite;
		
		System.out.println(archive.toString(true));
		return archive;
	}



	@Inject
	FTCreatePlanView fcv;

	@Test
	public void test() {
		System.out.println("Entering test");
		Assert.assertTrue(fcv!=null);
//		fcv.createPlan();
//		Plan plan = fcv.getPlan();
//		Assert.assertTrue(plan!=null);
//		Assert.assertTrue(true);
	}
}
