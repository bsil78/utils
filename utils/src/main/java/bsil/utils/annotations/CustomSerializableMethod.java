package bsil.utils.annotations;

import java.lang.annotation.*;

/**
 * point out a method of Serializable specifications
 * @see java.io.Serializable
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface CustomSerializableMethod {
}
