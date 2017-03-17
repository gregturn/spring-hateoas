package org.springframework.hateoas.web.mvc;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.hateoas.web.mvc.ControllerLinkBuilder.*;

import org.junit.Before;
import org.junit.Test;

import org.springframework.web.context.request.RequestContextHolder;

/**
 * Test cases for {@link org.springframework.hateoas.web.mvc.ControllerLinkBuilder} that are NOT inside an existing Spring MVC request
 *
 * @author Greg Turnquist
 */
public class ControllerLinkBuilderOutsideSpringMvcUnitTest {

	/**
	 * Clear out any existing request attributes left behind by other tests
	 */
	@Before
	public void setUp() {
		RequestContextHolder.setRequestAttributes(null);
	}

	/**
	 * @see #342
	 */
	@Test(expected = IllegalStateException.class)
	public void createsLinkToMethodOnParameterizedControllerRoot() {

		try {
			linkTo(methodOn(ControllerLinkBuilderUnitTest.PersonsAddressesController.class, 15)
					.getAddressesForCountry("DE")).withSelfRel();
		} catch (IllegalStateException e) {
			assertThat(e.getMessage(), equalTo("Could not find current request via RequestContextHolder. Is this being called from a Spring MVC handler?"));
			throw e;
		}
	}

}
