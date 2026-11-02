package com.aetherai.ai;

import java.util.List;

public interface AIProvider {

	AIResponse complete(List<ChatTurn> conversation);
}
