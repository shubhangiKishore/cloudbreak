package com.sequenceiq.it.cloudbreak;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public @interface Prototype {
}
