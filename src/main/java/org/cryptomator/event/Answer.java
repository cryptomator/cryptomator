package org.cryptomator.event;

public sealed interface Answer permits Answer.DoNothing, Answer.DoSomething {


	record DoNothing() implements Answer {}

	record DoSomething(Runnable action) implements Answer {

		void run() {
			action.run();
		}
	}
}
