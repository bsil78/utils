package bsil.utils.designpatterns.state;

import lombok.Getter;

/**
 * generic event for state event handeling
 */
public class Event {

    @Getter
    final String name;
    final Object payload;

    public Event(final String name, final Object payload) {
        this.name = name;
        this.payload = payload;
    }

    public <T> boolean isPayloadMatching(final Class<T> type){
        return type.isInstance(this.payload);
    }

    final <T> T retrievePayload(Class<T> ofType){
        return ofType.cast(this.payload);
    }


}
