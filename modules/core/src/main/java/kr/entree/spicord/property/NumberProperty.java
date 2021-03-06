package kr.entree.spicord.property;

import lombok.experimental.Delegate;

import java.util.function.LongSupplier;

/**
 * Created by JunHyung Lim on 2019-12-01
 */
public class NumberProperty implements Property<Number>, LongSupplier {
    @Delegate
    private final Property<Number> option;

    public NumberProperty(Property<Number> option) {
        this.option = option;
    }

    public long getLong() {
        return get().longValue();
    }

    @Override
    public long getAsLong() {
        return getLong();
    }

    public int getInt() {
        return get().intValue();
    }
}
