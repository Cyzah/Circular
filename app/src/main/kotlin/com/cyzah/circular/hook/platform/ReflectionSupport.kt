package com.cyzah.circular.hook.platform

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ReflectionSupport {
    @JvmStatic fun usableBooleanField(field: Field?): Boolean {
        return field != null && field.getType() == Boolean::class.javaPrimitiveType!! && !Modifier.isStatic(field.getModifiers())
    }
}
