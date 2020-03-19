package bsil.utils.json;

@SuppressWarnings("unused")
enum JsonTestsObjects {;


    public static final String FIELD = "field";
    public static final String FIELD_2 = FIELD+2;
    public static final String ONE_MORE_FIELD = "oneMoreField";
    public static final String FIELD_4 = FIELD+4;
    public static final String MY_STRING_FIELD = "MyStringField";
    public static final String STRING_1 = "string1";
    public static final String STRING_2 = "string2";

    static class MyClass {

        private final String stringField = MY_STRING_FIELD;
        private final MyInterface myInterfaceField = new MySubClass();
        private final String[] stringArray = {STRING_1, STRING_2};
    }

    static class MyComplexClass {

        private final MyClass[] subObject = {new MyClass(), null};

    }

    interface MyInterface {
    }

    static class MySubClass implements MyInterface {
        private String whatAbout="whatAbout";
    }

    static class ListObject {
        private final Object[] list = {null, 1.0, FIELD_2, 3};

    }

    static class ContainingObject {

        public ContainingObject(final Object contained) {
            this.fields = new Object[]{null, contained, 2, 3.0, ONE_MORE_FIELD};
        }

        private final Object[] fields;
    }

    static class MapObject {
        private final Object field0 = null;
        private final int field1 = 1;
        private final Object[] field2 = {null, 1.0, FIELD_2, 3};
        private final double field3 = 3.0d;
        private final String field4 = FIELD_4;
        private final Object field5 = null;
    }
}
