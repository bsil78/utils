package bsil.utils.annotations;

import java.lang.annotation.*;


/**
 * make the customization of serializable explicit
 * @see java.io.Serializable
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface CustomSerializable {

}
