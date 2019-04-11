package bsil78.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonResponseTest {

    @Before
    public void setupCiphering(){
        final Properties cipheringForTest = new Properties();
        cipheringForTest.put(Ciphering.CIPHERING_KEY_PROPERTY, "MyCipheringKey!!"); // 16 chars => 128 bits
        cipheringForTest.put(Ciphering.CIPHERING_TRANSFORMATION_PROPERTY, "AES/ECB/PKCS5Padding");
        System.setProperties(cipheringForTest);
    }

    @Before
    public void setupJson() {
        JsonResponse.setupJsonConverterForClass(myInterface -> myInterface.getClass().getSimpleName(),
                                                MyInterface.class);

    }

    @Test
    public void should_serialize_and_unserialize_properly_object()
    throws IOException, ClassNotFoundException {
        //GIVEN
        final MyComplexClass sourceObject=new MyComplexClass();
        final JsonResponse jsonResponse = JsonResponse.ofObject(sourceObject);

        //WHEN
        final byte[] transferedDatas = serialize(jsonResponse);
        /* some transfert occurs here */
        final JsonResponse result = unserialize(transferedDatas);

        //then
        assertThat(result.jsonContent()).isEqualTo(jsonResponse.jsonContent());
        assertThat(result.jsonStringValue("subObject[0].stringField").orElse(null)).isEqualTo("MyStringField");
        assertThat(result.jsonStringValue("subObject[0].myInterfaceField").orElse(null)).isEqualTo("MySubClass");
        assertThat(result.jsonStringValue("subObject[0].stringArray[0]").orElse(null)).isEqualTo("string1");
        assertThat(result.jsonStringValue("subObject[0].stringArray[1]").orElse(null)).isEqualTo("string2");
        assertThat(result.jsonStringValue("subObject[1]").orElse(null)).isNull();
    }



    @Test
    public void should_serialize_and_unserialize_properly_error()
    throws IOException, ClassNotFoundException {
        //GIVEN
        final String myErrorMessage = "MyErrorMessage";
        final String someDetailsToKnow = "some details to know";
        final JsonResponse jsonResponse = JsonResponse.ofError(myErrorMessage, someDetailsToKnow);

        //WHEN
        final byte[] transferedDatas = serialize(jsonResponse);
        /* some transfert occurs here */
        final JsonResponse result = unserialize(transferedDatas);

        //then
        assertThat(result.error().isPresent()).isTrue();
        assertThat(result.error().get().message()).isEqualTo(myErrorMessage);
        assertThat(result.error().get().details()).isEqualTo(someDetailsToKnow);

    }


    @Test
    public void should_serialize_and_unserialize_properly_error_fromException()
    throws IOException, ClassNotFoundException {
        //GIVEN
        final String someDetailsToKnow = "some details to know";
        final ArithmeticException myException = new ArithmeticException(someDetailsToKnow);
        final JsonResponse jsonResponse = JsonResponse.ofThrowable(myException);

        //WHEN
        final byte[] transferedDatas = serialize(jsonResponse);
        /* some transfert occurs here */
        final JsonResponse result = unserialize(transferedDatas);

        //then
        assertThat(result.error().isPresent()).isTrue();
        assertThat(result.error().get().message()).isEqualTo(someDetailsToKnow);
        assertThat(result.error().get().details()).isEqualTo(ExceptionUtils.getStackTrace(myException));

    }

    private static JsonResponse unserialize(final byte[] transferedDatas)
    throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(transferedDatas);
        final ObjectInput ois = new ObjectInputStream(inputStream);
        final JsonResponse result = (JsonResponse) ois.readObject();
        ois.close();
        return result;
    }

    private static byte[] serialize(final JsonResponse jsonResponse)
    throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutput oos=new ObjectOutputStream(outputStream);
        oos.writeObject(jsonResponse);
        final byte[] transferedDatas=outputStream.toByteArray();
        oos.close();
        return transferedDatas;
    }


    static class MyClass {

        private final String stringField="MyStringField";
        private final MyInterface myInterfaceField=new MySubClass();
        private final String[] stringArray={"string1","string2"};

    }

    static class MyComplexClass {

        private final MyClass[] subObject={new MyClass(),null};
    }

    interface MyInterface{};

    static class MySubClass implements MyInterface {

    }

}
