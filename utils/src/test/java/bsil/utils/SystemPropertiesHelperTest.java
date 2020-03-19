package bsil.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;


@SuppressWarnings({"AccessOfSystemProperties", "CallToSystemSetSecurityManager"})
class SystemPropertiesHelperTest {

    public static final String SOME_PROPERTY = "test.some.property";
    public static final String JAVA_HOME_PROPERTY = "java.home";
    public static final String SYS_ENV_VAR = "PATH";
    public static final String TEST_OF_SET_PROPERTY = "testOfSetProperty";
    public static final String FAKE_ENV_VAR = "FakeEnvVar";

    @BeforeEach
    final void initialize(){
        System.setSecurityManager(null);
    }

    @Test
    final void should_get_system_property_when_allowed() {
        testWithSecurityManager(() -> {
            final String expectedProperty = System.getProperty(JAVA_HOME_PROPERTY);
            assertThat(expectedProperty).isNotNull();
            final String property = SystemPropertiesHelper.getProperty(JAVA_HOME_PROPERTY);
            assertThat(property).isEqualTo(expectedProperty);
        });
    }


    @Test
    final void should_set_system_property_when_allowed() {
        testWithSecurityManager(() -> {
            final String expectedProperty = TEST_OF_SET_PROPERTY;
            SystemPropertiesHelper.setProperty(SOME_PROPERTY, expectedProperty);
            final String property = System.getProperty(SOME_PROPERTY);
            assertThat(property).isNotNull().isEqualTo(expectedProperty);
        });
    }

    @Test
    final void should_get_env_property_when_not_allowed() {
        final String[] expectedProperty=new String[1];
        testWithSecurityManager(() -> {
            expectedProperty[0] = System.getenv(SYS_ENV_VAR);
            assertThat(expectedProperty[0]).isNotNull();
        });
        final String property = SystemPropertiesHelper.getProperty(SYS_ENV_VAR);
        assertThat(property).isEqualTo(expectedProperty[0]);
    }

    @Test
    final void should_set_and_get_substitute_property_when_not_allowed() {
        final String[] expectedProperty = new String[1];
        testWithSecurityManager(() -> {
            expectedProperty[0] = System.getenv(FAKE_ENV_VAR);
            assertThat(expectedProperty[0]).isNull();
        });

        SystemPropertiesHelper.setProperty(FAKE_ENV_VAR, TEST_OF_SET_PROPERTY);
        final String property = SystemPropertiesHelper.getProperty(FAKE_ENV_VAR);
        assertThat(property).isEqualTo(TEST_OF_SET_PROPERTY);

    }

    private void testWithSecurityManager(Runnable runnable) {
        try {
            System.setSecurityManager(new SecurityManager());
            assertThat(System.getSecurityManager()).isNotNull();
            runnable.run();
            System.setSecurityManager(null);
            assertThat(System.getSecurityManager()).isNull();
        }
        catch (Exception e) {
            assertThat(e)
                .isInstanceOf(java.security.AccessControlException.class);
            fail("Please configure security context with : -Djava.security.policy==/path/to/our/allpermissions/java" +
                     ".policy");
        }
    }


}
