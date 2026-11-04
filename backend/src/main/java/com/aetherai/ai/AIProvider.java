package com.aetherai.ai;

import java.util.List;
import java.util.function.Consumer;

public interface AIProvider {

	AIResponse complete(List<ChatTurn> conversation);

	default AIResponse complete(List<ChatTurn> conversation, String model) {
		return complete(conversation);
	}

	default AIResponse stream(List<ChatTurn> conversation, Consumer<String> onToken) {
		AIResponse response = complete(conversation);
		onToken.accept(response.content());
		return response;
	}

	default AIResponse stream(List<ChatTurn> conversation, Consumer<String> onToken, String model) {
		return stream(conversation, onToken);
	}
}
