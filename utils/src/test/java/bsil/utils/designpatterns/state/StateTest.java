package bsil.utils.designpatterns.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.assertThat;

public class StateTest  {

    private TestContext context;

    @BeforeEach
    public void setup(){
        this.context= new TestContext();
        this.context.initStates();
    }


    @Test
    public void shouldStatesProceed(){
        final Event anEvent = new Event("A", null);
        IntStream.range(0,  this.context.totalStates())
                 .forEach(i-> verifyStateChangeOnEvent(anEvent));
    }

    private void verifyStateChangeOnEvent(final Event anEvent) {
        final Object stateBefore=this.context.currentState();
        this.context.handle(anEvent);
        final Object stateAfter = this.context.currentState();
        assertThat(stateBefore).isNotEqualTo(stateAfter);
    }


    static class TestContext implements StateContext {

        private final List<State<TestContext>> states = new ArrayList<>();
        private State<TestContext> currentState;


        public void initStates(){
            int nbStates = -1;
            this.states.add(State.of(this, strategyForState(++nbStates)));
            this.states.add(State.of(this, strategyForState(++nbStates)));
            this.states.add(State.of(this, strategyForState(++nbStates)));
            currentState = states.get(0);
        }

        private StateStrategy<TestContext> strategyForState(final int stateIndex) {

            return (event, myContext) -> () -> {
                final int nextStateIndex = stateIndex >= (totalStates() - 1)
                                               ? 0
                                               : (stateIndex + 1);
                System.out.println(format("State {0}, to state {1}", stateIndex, nextStateIndex));
                return states.get(nextStateIndex);
            };
        }

        private int totalStates() {
            return states.size();
        }

        @Override
        public State<TestContext> currentState() {
            return currentState;
        }

        @Override
        public void handle(final Event anEvent) {
            this.currentState=this.currentState.handle(anEvent);
        }
    }


}
