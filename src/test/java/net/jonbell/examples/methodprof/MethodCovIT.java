package net.jonbell.examples.methodprof;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class MethodCovIT {
	@Test
	public void testCleanup() throws Exception {
		ProfileLogger.dump();
//		assertEquals(0, ProfileLogger.dump().size());
	}

	@Test
	public void testSingleCall() throws Exception {
		ProfileLogger.dump();
		otherMethod();
		AtomicLong l = new AtomicLong();
		String s = l.toString();
		HashSet<String> meths = ProfileLogger.dump();
//		System.out.println(meths);
//		assertEquals(45, meths.size());
//		assertEquals("net/jonbell/examples/methodprof/MethodCovIT.otherMethod()V", meths.iterator().next());
	}

	private void otherMethod() {
	}

	@Test
	public void testJavaMethodsExcluded() throws Exception {
//		ProfileLogger.dump();
//		HashSet<Object> foo = new HashSet<Object>();
//		assertEquals(0, ProfileLogger.dump().size());
	}
}
