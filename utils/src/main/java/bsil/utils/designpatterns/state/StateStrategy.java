package bsil.utils.designpatterns.state;

import java.util.function.Supplier;

/**
 * concrete strategy of a state
 */
@FunctionalInterface
public interface StateStrategy<T extends StateContext> {

    Supplier<State<T>> handle(Event event, T myContext);

}

