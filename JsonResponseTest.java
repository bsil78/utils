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
        final byte[] transferedDatas = serialize(jsonResponse);
        /* some transfert occurs here */
        return unserialize(transferedDatas);
    }

    @Test
    public void should_retrieve_simple_strings_properly() {
        //GIVEN
        final MyComplexClass sourceObject = new MyComplexClass();

        //WHEN
        final JsonResponse jsonResponse = JsonResponse.ofObject(sourceObject);

        //THEN
        assertThat(jsonResponse.jsonStringValue("subObject[0].stringField").orElse(null)).isEqualTo("MyStringField");
        assertThat(jsonResponse.jsonStringValue("subObject[0].myInterfaceField").orElse(null)).isEqualTo("MySubClass");
        assertThat(jsonResponse.jsonStringValue("subObject[0].stringArray[0]").orElse(null)).isEqualTo("string1");
        assertThat(jsonResponse.jsonStringValue("subObject[0].stringArray[1]").orElse(null)).isEqualTo("string2");
        assertThat(jsonResponse.jsonStringValue("subObject[1]").orElse(null)).isNull();
    }

    @Test
    public void should_read_properly_strings_list() {
        //GIVEN
        final ListObject listObject = new ListObject();
        final ContainingObject containingListObject = new ContainingObject(listObject);

        //WHEN
        final JsonResponse[] jsonResponses = {
            JsonResponse.ofObject(listObject),
            JsonResponse.ofObject(containingListObject)
        };

        //THEN
        assertThat(jsonResponses[0].jsonStringValue("list[0]").orElse(null)).isEqualTo("null");
        assertThat(jsonResponses[0].jsonStringValue("list[2]").orElse(null)).isEqualTo("field2");
        assertThat(jsonResponses[0].jsonStringValues("list")).contains(null,"1.0", "field2", "3");
        assertThat(jsonResponses[1].jsonStringValue("fields[1].list[2]").orElse(null)).isEqualTo("field2");
        assertThat(jsonResponses[1].jsonStringValue("fields[4]").orElse(null)).isEqualTo("oneMoreField");
        assertThat(jsonResponses[1].jsonStringValues("fields[1].list")).contains(null,"1.0", "field2", "3");

    }


    @Test
    public void should_read_properly_strings_map() {
        //GIVEN
        final MapObject mapObject = new MapObject();
        final ContainingObject containingMapObject = new ContainingObject(mapObject);

        //WHEN
        final JsonResponse[] jsonResponses = {
            JsonResponse.ofObject(containingMapObject)
        };

        //THEN
        final String undefined = "UNDEFINED";
        assertThat(jsonResponses[0].jsonStringValue("fields[1].field0").orElse(null)).isNull();
        assertThat(jsonResponses[0].jsonStringValue("fields[1].field1").orElse(null)).isEqualTo("1");
        assertThat(jsonResponses[0].jsonStringValue("fields[1].field2").orElse(undefined)).isEqualTo("[null,1.0,\"field2\",3]");
        assertThat(jsonResponses[0].jsonStringValue("fields[1].field3").orElse(null)).isEqualTo("3.0");
        assertThat(jsonResponses[0].jsonStringValue("fields[1].field4").orElse(null)).isEqualTo("field4");


        assertThat(jsonResponses[0].jsonStringsMap("fields[1]"))
            .containsEntry("field0",null)
            .containsEntry("field1", "1")
            .containsEntry("field2", "[null,1.0,\"field2\",3]")
            .containsEntry("field3", "3.0")
            .containsEntry("field4", "field4")
        ;

    }

    @Test
    public void should_serialize_and_unserialize_properly_error()
    throws IOException, ClassNotFoundException {
        //GIVEN
        final String myErrorMessage = "MyErrorMessage";
        final String someDetailsToKnow = "some details to know";
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
        final String someDetailsToKnow = "some details to know";
        final ArithmeticException myException = new ArithmeticException(someDetailsToKnow);
        final JsonResponse jsonResponse = JsonResponse.ofThrowable(myException);

        //WHEN
        final JsonResponse result = transfering(jsonResponse);

        //THEN
        assertThat(result.error().isPresent()).isTrue();
        assertThat(result.error().get().message()).isEqualTo(someDetailsToKnow);
        assertThat(result.error().get().details()).isEqualTo(ExceptionUtils.getStackTrace(myException));

    }

    private static JsonResponse unserialize(final byte[] transferedDatas)
    throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(transferedDatas);
        try(final ObjectInput ois = new ObjectInputStream(inputStream)) {
            final JsonResponse result = (JsonResponse) ois.readObject();
            return result;
        }
    }

    private static byte[] serialize(final JsonResponse jsonResponse)
    throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try(final ObjectOutput oos=new ObjectOutputStream(outputStream)) {
            oos.writeObject(jsonResponse);
            final byte[] transferedDatas = outputStream.toByteArray();
            return transferedDatas;
        }
    }

    private class ListObject {
        private final Object[] list = {null, 1.0, "field2", 3};
    }

    private class ContainingObject {

        private ContainingObject(final Object contained){
            this.fields = new Object[]{null,contained, 2, 3.0, "oneMoreField"};
        }

        private final Object[] fields;
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

    private class MapObject {
        private final Object field0 = null;
        private final int field1 = 1;
        private final Object[] field2 = {null, 1.0, "field2", 3};
        private final double field3 = 3.0d;
        private final String field4 = "field4";

    }
}
