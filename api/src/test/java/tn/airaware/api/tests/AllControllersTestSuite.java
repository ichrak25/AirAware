package tn.airaware.api.tests;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite to run all controller integration tests
 *
 * Run with: mvn test
 * Or run individual test classes directly
 */
@Suite
@SuiteDisplayName("AIR-AWARE Integration Tests Suite")
@SelectPackages("tn.airaware.api.tests.controllers")
public class AllControllersTestSuite {
    // This class remains empty, it's used only as a holder for the above annotations
}