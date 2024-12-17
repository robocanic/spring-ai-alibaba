package com.alibaba.cloud.ai.graph.state;

import java.util.Optional;
import java.util.function.Supplier;

public class ImmutableChannel<T> implements Channel<T>{
    @Override
    public Optional<Reducer<T>> getReducer() {
        return Optional.empty();
    }

    @Override
    public Optional<Supplier<T>> getDefault() {
        return Optional.empty();
    }

    @Override
    public Object update(String key, Object oldValue, Object newValue) {
        return oldValue;
    }

}
