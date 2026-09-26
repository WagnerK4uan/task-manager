package com.taskmanager.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LayerDependencyTest {

    /**
     * As anotações de mapeamento são a única concessão: {@code jakarta.persistence} e
     * {@code org.hibernate.annotations}, que carrega {@code @CreationTimestamp} e
     * {@code @UpdateTimestamp}. Qualquer outro pacote de framework está proibido no domínio.
     */
    private static final DescribedPredicate<JavaClass> FRAMEWORK_FORA_DO_MAPEAMENTO =
            resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
                    .and(not(resideInAnyPackage("jakarta.persistence..", "org.hibernate.annotations..")))
                    .as("framework além das anotações de mapeamento");

    private static JavaClasses classes;

    @BeforeAll
    static void importarClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.taskmanager");
    }

    @Test
    @DisplayName("@spec:AC-005 domínio não depende de framework nem das outras camadas")
    void dominioNaoDependeDeFrameworkNemDasOutrasCamadas() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat(
                        FRAMEWORK_FORA_DO_MAPEAMENTO.or(
                                resideInAnyPackage(
                                        "..application..", "..infrastructure..", "..presentation..")))
                .because("o domínio é o centro: não conhece Spring, DTO nem HTTP")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("@spec:AC-006 apresentação não conhece entidade nem repositório")
    void apresentacaoNaoConheceEntidadeNemRepositorio() {
        noClasses()
                .that()
                .resideInAPackage("..presentation..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..domain.entity..", "..domain.repository..", "..infrastructure.persistence..")
                .because("a apresentação conversa só com application, por DTO")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("@spec:AC-007 aplicação não depende de quem a chama nem de quem a serve")
    void aplicacaoNaoDependeDeQuemAChamaNemDeQuemAServe() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..presentation..", "..infrastructure..")
                .because("a aplicação depende do domínio para dentro, nunca das pontas")
                .allowEmptyShould(true)
                .check(classes);
    }
}
