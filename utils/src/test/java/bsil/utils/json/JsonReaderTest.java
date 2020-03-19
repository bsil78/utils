package bsil.utils.json;

import bsil.utils.ciphering.CipherTest;
import bsil.utils.ciphering.CipheringConfigHelper;
import bsil.utils.json.JsonTestsObjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

import static bsil.utils.json.JsonTestsObjects.*;
import static org.assertj.core.api.Assertions.assertThat;


public class JsonReaderTest {

    @BeforeEach
    public void setupCiphering()
    throws NoSuchAlgorithmException, NoSuchPaddingException {
        CipherTest.setupPaddedAESCipheringForTest();
        JsonResponse.setupCiphering(CipheringConfigHelper.config());
    }

    @Test
    public void should_read_properly_simple_fields_as_string() {
        //GIVEN
        final MyComplexClass sourceObject = new MyComplexClass();
        final String json= JsonResponse.ofObject(sourceObject).jsonContent();

        //WHEN
        final JsonReader jsonReader = new JsonReader(json);

        //THEN
        assertThat(jsonReader.jsonOrNull("subObject[0].stringField")).isEqualTo(MY_STRING_FIELD);
        assertThat(jsonReader.jsonOrNull("subObject[0].myInterfaceField")).isEqualTo("{\"whatAbout\":\"whatAbout\"}");
        assertThat(jsonReader.jsonOrNull("subObject[0].stringArray[0]")).isEqualTo(STRING_1);
        assertThat(jsonReader.jsonOrNull("subObject[0].stringArray[1]")).isEqualTo(STRING_2);
        assertThat(jsonReader.jsonOrNull("subObject[1]")).isNull();
    }

    @Test
    public void should_read_properly_strings_list() {
        //GIVEN
        final ListObject listObject = new ListObject();
        final ContainingObject containingListObject = new ContainingObject(listObject);

        final String json1 = JsonResponse.ofObject(listObject).jsonContent();
        final String json2= JsonResponse.ofObject(containingListObject).jsonContent();

        //WHEN
        final JsonReader json1Reader = new JsonReader(json1);
        final JsonReader json2Reader = new JsonReader(json2);

        //THEN
        assertThat(json1Reader.jsonOrNull("list[0]")).isNull();
        assertThat(json1Reader.jsonOrNull("list[2]")).isEqualTo(FIELD_2);
        assertThat(json1Reader.jsonList("list")).contains(null, "1.0", FIELD_2, "3");
        assertThat(json2Reader.jsonOrNull("fields[1].list[2]")).isEqualTo(FIELD_2);
        assertThat(json2Reader.jsonOrNull("fields[4]")).isEqualTo(ONE_MORE_FIELD);
        assertThat(json2Reader.jsonList("fields[1].list")).contains(null, "1.0", FIELD_2, "3");

    }


    @Test
    public void should_read_properly_simple_strings_map() {
        //GIVEN
        final MapObject mapObject = new MapObject();
        final String json = JsonResponse.ofObject(mapObject).jsonContent();
        //WHEN
        final JsonReader jsonReader = new JsonReader(json);

        //THEN
        assertThat(jsonReader.jsonStringsMap("$")) // our MapObject
                                                           .containsEntry(FIELD + 0, null)
                                                           .containsEntry(FIELD + 1, "1")
                                                           .containsEntry(FIELD + 2, "[null,1.0,\"field2\",3]")
                                                           .containsEntry(FIELD + 3, "3.0")
                                                           .containsEntry(FIELD + 4, FIELD_4)
        ;

    }

    @Test
    public void should_read_properly_deep_strings_map() {
        //GIVEN
        final MapObject mapObject = new MapObject();
        final ContainingObject containingMapObject = new ContainingObject(mapObject);
        final String json= JsonResponse.ofObject(containingMapObject).jsonContent();
        //WHEN
        final JsonReader jsonReader = new JsonReader(json);

        //THEN

        assertThat(jsonReader.jsonOrNull("fields[1].field0")).isNull();
        assertThat(jsonReader.jsonOrNull("fields[1].field1")).isEqualTo("1");
        assertThat(jsonReader.jsonOrUndefined("fields[1].field2")).isEqualTo("[null,1.0,\"field2\",3]");
        assertThat(jsonReader.jsonOrNull("fields[1].field3")).isEqualTo("3.0");
        assertThat(jsonReader.jsonOrNull("fields[1].field4")).isEqualTo(FIELD_4);


        assertThat(jsonReader.jsonStringsMap("fields[1]")) // our MapObject
            .containsEntry(FIELD +0, null)
            .containsEntry(FIELD +1, "1")
            .containsEntry(FIELD +2, "[null,1.0,\"field2\",3]")
            .containsEntry(FIELD +3, "3.0")
            .containsEntry(FIELD +4, FIELD_4)
        ;

    }



}
