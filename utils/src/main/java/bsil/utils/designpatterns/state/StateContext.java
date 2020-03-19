package bsil.utils.designpatterns.state;

/**
 * a context for states
 */
public interface StateContext {

    void handle(Event anEvent);

    State<? extends StateContext> currentState();

}
