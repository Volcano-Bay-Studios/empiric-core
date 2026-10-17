package xyz.volcanobay.empiric.core.data;

import java.lang.reflect.Type;

public interface ModeResolver {

    Mode resolve(Type type, int level, Mode proposed);

    final class None implements ModeResolver {

        @Override
        public Mode resolve(Type type, int level, Mode proposed) {
            return proposed;
        }
    }
}
