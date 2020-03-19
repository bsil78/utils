package bsil.utils.json;

import bsil.utils.ciphering.CipherTest;
import bsil.utils.ciphering.CipheringConfigHelper;
import bsil.utils.json.JsonResponse.CannotConvertToJson;
import bsil.utils.json.JsonTestsObjects.MyComplexClass;
import bsil.utils.json.JsonTestsObjects.MyInterface;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonResponseTest {

    private static final String SOME_DETAILS_TO_KNOW = "some details to know";

    @BeforeAll
    static void setupCiphering()
        throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        CipherTest.setupPaddedAESCipheringForTest();
        JsonResponse.setupCiphering(CipheringConfigHelper.config());
    }

    @BeforeAll
    static void setupJson() {
        CustomGsonBuilder.setupJsonConverterForClass(myInterface -> myInterface.getClass().getSimpleName(), MyInterface.class);
    }

    @Test
    public void should_serialize_and_unserialize_properly_objects()
    throws IOException, ClassNotFoundException {
        //GIVEN
        final MyComplexClass sourceObject=new MyComplexClass();
        final JsonResponse jsonResponse = JsonResponse.ofObject(sourceObject);

        //WHEN
        final JsonResponse result = transfering(jsonResponse);

        //THEN
        assertThat(result.jsonContent()).isEqualTo(jsonResponse.jsonContent());

    }

    private JsonResponse transfering(final JsonResponse jsonResponse)
    throws IOException, ClassNotFoundException {
        /* some transfert occurs here */
        return JsonResponse.deserialize(jsonResponse.serialize());
    }



    @Test
    public void should_serialize_and_unserialize_properly_error()
        throws IOException, ClassNotFoundException
    {
        //GIVEN
        final String myErrorMessage = "MyErrorMessage";
        final String someDetailsToKnow = SOME_DETAILS_TO_KNOW;
        final JsonResponse jsonResponse = JsonResponse.ofError(myErrorMessage, someDetailsToKnow);

        //WHEN
        final JsonResponse result = transfering(jsonResponse);

        //THEN
        assertThat(result.error().isPresent()).isTrue();
        assertThat(result.error().get().message()).isEqualTo(myErrorMessage);
        assertThat(result.error().get().details()).isEqualTo(someDetailsToKnow);

    }


    @Test
    public void should_serialize_and_unserialize_properly_error_fromException()
    throws IOException, ClassNotFoundException {
        //GIVEN
        final String someDetailsToKnow = SOME_DETAILS_TO_KNOW;
        final ArithmeticException myException = new ArithmeticException(someDetailsToKnow);
        final JsonResponse jsonResponse = JsonResponse.ofThrowable(myException);

        //WHEN
        final JsonResponse result = transfering(jsonResponse);

        //THEN
        assertThat(result.error().isPresent()).isTrue();
        assertThat(result.error().get().message()).isEqualTo(someDetailsToKnow);
        assertThat(result.error().get().details()).isEqualTo(ExceptionUtils.getStackTrace(myException));

    }




    @Test
    public void cannot_convert_anonymous_classes()
    {
        //given
        MyInterface anonymousClassObject = new MyInterface() {
            private boolean isThere = false;
        };
        //when
        final ThrowingCallable when = () -> JsonResponse.ofObject(anonymousClassObject);
        //then
        assertThatThrownBy(when).isInstanceOf(CannotConvertToJson.class)
                                .hasMessage("Cannot convert bsil.utils.json.JsonResponseTest$1 anonymous class instance to json");;

    }

    @Test
    public void cannot_convert_localy_scoped_classes()
    {
        //given
        class MyLocalClass implements MyInterface {
            private boolean isThere = false;
        };
        MyInterface anonymousClassObject = new MyLocalClass();

        //when
        final ThrowingCallable when = () -> JsonResponse.ofObject(anonymousClassObject);
        //then
        assertThatThrownBy(when).isInstanceOf(CannotConvertToJson.class)
                                .hasMessage("Cannot convert scope limited object bsil.utils.json.JsonResponseTest$1MyLocalClass to json");
    }

    @Test
    public void cannot_convert_null_object() {
        assertThatThrownBy(()->JsonResponse.ofObject(null)).isInstanceOf(CannotConvertToJson.class);

    }


}
