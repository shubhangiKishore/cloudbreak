package com.sequenceiq.environment.environment.domain;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.NoFieldShadowingRule;
import com.openpojo.validation.rule.impl.NoNestedClassRule;
import com.openpojo.validation.rule.impl.NoPublicFieldsExceptStaticFinalRule;
import com.openpojo.validation.rule.impl.NoStaticExceptFinalRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

class DomainTest {

    @Test
    void testPojoStructureAndBehavior() {

        List<PojoClass> pojoClasses = new ArrayList<>();
        pojoClasses.add(PojoClassFactory.getPojoClass(Region.class));
        pojoClasses.add(PojoClassFactory.getPojoClass(CompactView.class));
        pojoClasses.add(PojoClassFactory.getPojoClass(EnvironmentAuthentication.class));

        Validator validator = ValidatorBuilder.create()
                .with(new SetterMustExistRule(),
                        new GetterMustExistRule())
                .with(new GetterTester())
                .with(new NoPublicFieldsExceptStaticFinalRule())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .with(new NoFieldShadowingRule())
                .build();
        validator.validate(pojoClasses);

        validator = ValidatorBuilder.create()
                .with(new SetterTester())
                .build();
        validator.validate(pojoClasses);
        // openpojo will do for now (seems buggy) but later would worth experimenting with pojo-tester (https://www.pojo.pl/)
    }
}
