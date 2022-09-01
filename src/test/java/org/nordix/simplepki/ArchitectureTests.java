package org.nordix.simplepki;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@AnalyzeClasses(packages = {ArchitectureTests.BASE_PACKAGE})
public class ArchitectureTests {

    static final String BASE_PACKAGE = "org.nordix.simplepki";

    @ArchTest
    public static final ArchRule packageRule = onionArchitecture()
        .domainModels("..domain..")
        .applicationServices("application", "..application..")
        .adapter("api", "..adapters.api..")
        .adapter("db", "..adapters.db..")
        .adapter("keystore", "..adapters.keystore..")
        .withOptionalLayers(true);
}
