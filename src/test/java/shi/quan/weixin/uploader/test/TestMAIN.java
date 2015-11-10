package shi.quan.weixin.uploader.test;

import static org.junit.Assert.*;

import java.util.*;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class TestMAIN {
	@Parameters(name="{index}: Datium := {0}")
	public static Iterable<String> data() {
		return Arrays.asList(new String[]{
			"./source", 
			"NONE"
		});
	}

	private String datium = null;

	public TestMAIN(String datium) {
		this.datium = datium;
	}

	@Before
	public void noSetup() {
		System.out.println("[noSetup]");
	}

	@After
	public void noTearDown() {
		System.out.println("[noTearDown]");
	}

	@Test
	public void test_SourceFolder() {
		System.out.println("[test_SourceFolder]");
		try {
			shi.quan.weixin.uploader.MAIN.main(new String[]{this.datium});
		} catch(Exception ex) {
			fail(ex.getMessage());
		}
	}
}
