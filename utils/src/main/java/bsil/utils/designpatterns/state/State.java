package bsil.utils.designpatterns.state;

import javax.validation.constraints.NotNull;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * common machinerie of a state
 */
public class State<T extends StateContext> {

    private final T myContext;
    private final StateStrategy<T> myStrategy;
    private String attributesDescription="aState";

    public State(final T myContext, @NotNull final StateStrategy<T> myStrategy) {
        this.myContext = myContext;
        this.myStrategy = requireNonNull(myStrategy);
    }

    public static <T extends StateContext> State<T> of(final T context,
                                                    final StateStrategy<T> stateStrategy) {
        return new State<>(context, stateStrategy);
    }

    public State<T> withContext(@NotNull final T aContext){
        return new State<>(requireNonNull(aContext),this.myStrategy);
    }

    public State<T> handle(@NotNull final Event anEvent){
        requireNonNull(anEvent);
        return ofNullable(this.myContext)
                   .map(context->this.myStrategy.handle(anEvent, context).get())
                   .orElseThrow(IllegalStateException::new);
    }

    @Override
    public String toString() {
        return this.attributesDescription;
    }

}
