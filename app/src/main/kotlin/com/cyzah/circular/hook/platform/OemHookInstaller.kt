package com.cyzah.circular.hook.platform


class OemHookInstaller(private val loader: ClassLoader?) {
    fun matches(vararg groups: Array<String?>?): Boolean {
        return OemExpansion.matches(
            object : Compatibility.Inspector {
                @Throws(Exception::class)
                override fun check(requirement: Compatibility.Requirement) {
                    Compatibility.check(loader, requirement)
                }
            },
            *groups
        )
    }
}
