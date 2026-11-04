package com.aetherai.rag;

public interface EmbeddingProvider {

	float[] embed(String text);
}
