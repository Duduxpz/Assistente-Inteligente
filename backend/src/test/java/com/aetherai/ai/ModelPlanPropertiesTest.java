package com.aetherai.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelPlanPropertiesTest {

	@Test
	void resolvesConfiguredModelForEachPlan() {
		ModelPlanProperties properties = new ModelPlanProperties("free-model", "plus-model", "ultra-model");

		assertEquals("free-model", properties.forPlan("free"));
		assertEquals("plus-model", properties.forPlan("plus"));
		assertEquals("ultra-model", properties.forPlan("ultra"));
	}
}
