package im.best0ption.velvet.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value= RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    public String name();
    public Type type();
    public String desc() default "";
}
