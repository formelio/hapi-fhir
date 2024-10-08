package ca.uhn.fhir.mdm.api.paging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MdmPageLinkBuilderTest {

	@Test
	void buildLinkWithExistingParameters() {
		//Given
		String expected = "http://localhost:8000/$mdm-query-links?sourceResourceId=Patient/123&_offset=1&_count=1";
		String baseUrl = "http://localhost:8000/$mdm-query-links?sourceResourceId=Patient/123";

		//When
		String builtUrl = MdmPageLinkBuilder.buildLinkWithOffsetAndCount(baseUrl, 1, 1);

		//Then
		assertEquals(expected, builtUrl);
	}

	@Test
	void buildLinkWithoutExistingParameters() {
		//Given
		String expected = "http://localhost:8000/$mdm-query-links?_offset=1&_count=1";
		String baseUrl = "http://localhost:8000/$mdm-query-links";

		//When
		String builtUrl = MdmPageLinkBuilder.buildLinkWithOffsetAndCount(baseUrl, 1, 1);

		//Then
		assertEquals(expected, builtUrl);
	}
}
